package com.xenonware.notes.util

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.util.zip.ZipInputStream

data class TranscriptSegment(
    val text: String,
    val timestampMillis: Long,
    val confidence: Float = 1.0f,
)

class VoskSpeechRecognitionManager(
    private val context: Context,
    private val language: String = "en",
    private val sampleRate: Int = 16000,
) {

    companion object {
        private const val TAG = "VoskSTT"
        private const val BUFFER_SIZE = 8192
    }

    init {
        LibVosk.setLogLevel(LogLevel.WARNINGS)
    }

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var audioRecord: AudioRecord? = null
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    var isListening by mutableStateOf(false)
        private set

    var currentPartialText by mutableStateOf("")
        private set

    var transcriptSegments = mutableListOf<TranscriptSegment>()
        private set

    var isTranscribing by mutableStateOf(false)
        private set

    var isModelLoading by mutableStateOf(true)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var onTranscriptUpdate: ((List<TranscriptSegment>) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onReady: (() -> Unit)? = null

    private var recordingStartTimeMs: Long = 0L
    private var shouldContinue = false

    init {
        // Auto-load default model on creation
        val defaultKey = "en-small" // or prefsManager.voskModelKey
        switchModel(defaultKey, {}, {})
    }

    fun switchModel(
        modelKey: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val info = AVAILABLE_MODELS.find { it.key == modelKey }
            ?: return onError("Unknown model key: $modelKey")

        isModelLoading = true
        errorMessage = null
        android.util.Log.i(TAG, "Switching to model: ${info.name}")

        scope.launch {
            val modelDir = File(context.filesDir, info.folderName)

            // First try to extract from assets (for small models)
            if (tryExtractFromAssets(info, modelDir)) {
                loadModelFromPath(modelDir.absolutePath, onSuccess, onError)
                return@launch
            }

            // If not in assets, download
            downloadAndExtractModel(info, modelDir, onSuccess, onError)
        }
    }

    private fun tryExtractFromAssets(info: ModelInfo, targetDir: File): Boolean {
        if (targetDir.exists() && targetDir.listFiles()?.isNotEmpty() == true) return true

        return try {
            context.assets.open(info.zipName).use { input ->
                ZipInputStream(input).use { zip ->
                    targetDir.mkdirs()
                    var entry: java.util.zip.ZipEntry?
                    while (zip.nextEntry.also { entry = it } != null) {
                        val file = File(targetDir, entry!!.name)
                        if (entry!!.isDirectory) file.mkdirs()
                        else {
                            file.parentFile?.mkdirs()
                            file.outputStream().use { out -> zip.copyTo(out) }
                        }
                        zip.closeEntry()
                    }
                }
            }
            android.util.Log.i(TAG, "Extracted from assets: ${info.name}")
            true
        } catch (e: Exception) {
            android.util.Log.w(TAG, "No asset zip for ${info.zipName}: ${e.message}")
            false
        }
    }

    private suspend fun downloadAndExtractModel(
        info: ModelInfo,
        targetDir: File,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) = withContext(Dispatchers.IO) {
        try {
            val url = "https://alphacephei.com/vosk/models/${info.zipName}"
            android.util.Log.i(TAG, "Downloading from: $url")

            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle("Downloading ${info.name}")
                setDescription("~${info.approxSizeMB} MB")
                setDestinationInExternalFilesDir(context, null, "vosk_${info.zipName}")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(false)
            }

            val dm = context.getSystemService<DownloadManager>()!!
            val downloadId = dm.enqueue(request)

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
                    if (id == downloadId) {
                        val query = DownloadManager.Query().setFilterById(downloadId)
                        dm.query(query)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                    val uriCol = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)
                                    val uriStr = cursor.getString(uriCol)
                                    val path = uriStr.removePrefix("file://")
                                    extractZipAndLoad(path, targetDir, onSuccess, onError)
                                } else {
                                    onError("Download failed (status $status)")
                                }
                            }
                        }
                        ctx?.unregisterReceiver(this)
                    }
                }
            }
            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onError("Download setup failed: ${e.message}") }
        }
    }

    private fun extractZipAndLoad(
        zipPath: String,
        targetDir: File,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                targetDir.mkdirs()
                ZipInputStream(java.io.FileInputStream(zipPath)).use { zip ->
                    var entry: java.util.zip.ZipEntry?
                    while (zip.nextEntry.also { entry = it } != null) {
                        val file = File(targetDir, entry!!.name)
                        if (entry!!.isDirectory) file.mkdirs()
                        else {
                            file.parentFile?.mkdirs()
                            file.outputStream().use { out -> zip.copyTo(out) }
                        }
                        zip.closeEntry()
                    }
                }
                File(zipPath).delete()
                withContext(Dispatchers.Main) {
                    loadModelFromPath(targetDir.absolutePath, onSuccess, onError)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError("Extraction failed: ${e.message}") }
            }
        }
    }

    private fun loadModelFromPath(path: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        try {
            model?.close()
            recognizer?.close()
            model = Model(path)
            recognizer = Recognizer(model, sampleRate.toFloat())
            isModelLoading = false
            android.util.Log.i(TAG, "Vosk model loaded successfully: $path")
            onSuccess()
            onReady?.invoke()
        } catch (e: Exception) {
            isModelLoading = false
            errorMessage = "Failed to load model"
            android.util.Log.e(TAG, "Model load failed: ${e.message}")
            onError("Load failed: ${e.message}")
        }
    }

    fun isAvailable(): Boolean = model != null && recognizer != null && !isModelLoading

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startListening(recordingStartTime: Long = System.currentTimeMillis()) {
        if (isModelLoading || model == null || recognizer == null) {
            onError?.invoke("Model not ready")
            return
        }

        if (isListening) return

        recordingStartTimeMs = recordingStartTime
        shouldContinue = true

        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuffer * 2
        )

        try {
            audioRecord?.startRecording()
        } catch (e: Exception) {
            onError?.invoke("Microphone error: ${e.message}")
            stopListening()
            return
        }

        isListening = true
        isTranscribing = true
        currentPartialText = ""

        job = scope.launch {
            val buffer = ShortArray(BUFFER_SIZE)
            while (isActive && isListening && shouldContinue) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                if (read > 0) {
                    try {
                        recognizer?.acceptWaveForm(buffer, read)
                    } catch (e: Throwable) {
                        android.util.Log.e(TAG, "acceptWaveForm crashed", e)
                    }

                    try {
                        recognizer?.partialResult?.let { jsonStr ->
                            if (jsonStr.isNotBlank()) {
                                val json = JSONObject(jsonStr)
                                val text = json.optString("partial", "").trim()
                                if (text.isNotBlank()) currentPartialText = text
                            }
                        }
                    } catch (_: Throwable) {}

                    try {
                        recognizer?.result?.let { jsonStr ->
                            if (jsonStr.isNotBlank()) {
                                val json = JSONObject(jsonStr)
                                val text = json.optString("text", "").trim()
                                if (text.isNotBlank()) {
                                    val segment = TranscriptSegment(
                                        text = text,
                                        timestampMillis = System.currentTimeMillis() - recordingStartTimeMs
                                    )
                                    transcriptSegments.add(segment)
                                    onTranscriptUpdate?.invoke(transcriptSegments.toList())
                                    currentPartialText = ""
                                }
                            }
                        }
                    } catch (_: Throwable) {}
                }
                delay(5)
            }
        }
    }

    fun stopListening() {
        if (!isListening) return

        shouldContinue = false
        isListening = false
        isTranscribing = false

        job?.cancel()
        job = null

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        scope.launch {
            delay(500)
            recognizer?.let { rec ->
                try {
                    rec.result?.let { jsonStr ->
                        if (jsonStr.isNotBlank()) {
                            val json = JSONObject(jsonStr)
                            val text = json.optString("text", "").trim()
                            if (text.isNotBlank() && text != currentPartialText) {
                                transcriptSegments.add(
                                    TranscriptSegment(text, System.currentTimeMillis() - recordingStartTimeMs)
                                )
                                onTranscriptUpdate?.invoke(transcriptSegments.toList())
                            }
                        }
                    }
                } catch (e: Throwable) {
                    android.util.Log.e(TAG, "Final result crash prevented", e)
                }
            }
            currentPartialText = ""
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun restartListening() {
        if (shouldContinue) {
            stopListening()
            shouldContinue = true
            startListening(recordingStartTimeMs)
        }
    }

    fun cancel() {
        shouldContinue = false
        stopListening()
    }

    fun clearTranscript() {
        transcriptSegments.clear()
        currentPartialText = ""
        onTranscriptUpdate?.invoke(emptyList())
    }

    fun loadTranscript(segments: List<TranscriptSegment>) {
        transcriptSegments.clear()
        transcriptSegments.addAll(segments)
        onTranscriptUpdate?.invoke(transcriptSegments)
    }

    fun dispose() {
        shouldContinue = false
        cancel()
        recognizer?.close()
        model?.close()
        recognizer = null
        model = null
    }
}