package com.xenonware.notes.util.audio

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class TranscriptSegment(
    val text: String,
    val timestampMillis: Long,
    val confidence: Float = 1.0f,
)

class VoskSpeechRecognitionManager(
    private val context: Context,
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

    private var lastPartialUpdateTime = 0L

    init {
        val defaultKey = "en-small"
        switchModel(defaultKey, {}, {})
    }

    fun switchModel(
        modelKey: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val info = AVAILABLE_MODELS.find { it.key == modelKey }
            ?: return onError("Unknown model key: $modelKey")

        model?.close()
        recognizer?.close()
        model = null
        recognizer = null

        isModelLoading = true
        errorMessage = null
        Log.i(TAG, "Switching to model: ${info.name} (old freed)")

        scope.launch {
            val modelDir = File(context.filesDir, info.folderName)

            if (tryExtractFromAssets(info, modelDir)) {
                loadModelFromPath(modelDir.absolutePath, onSuccess, onError)
                return@launch
            }

            if (modelDir.exists() && modelDir.listFiles()?.isNotEmpty() == true) {
                loadModelFromPath(modelDir.absolutePath, onSuccess, onError)
            } else {
                isModelLoading = false
                onError("Model not installed. Use download button.")
            }
        }
    }

    private fun tryExtractFromAssets(info: ModelInfo, targetDir: File): Boolean {
        if (targetDir.exists() && targetDir.listFiles()?.isNotEmpty() == true) return true

        return try {
            context.assets.open(info.zipName).use { input ->
                ZipInputStream(input).use { zip ->
                    targetDir.mkdirs()
                    var entry: ZipEntry?
                    while (zip.nextEntry.also { entry = it } != null) {
                        val file = File(targetDir, entry!!.name)
                        if (entry.isDirectory) file.mkdirs()
                        else {
                            file.parentFile?.mkdirs()
                            file.outputStream().use { out -> zip.copyTo(out) }
                        }
                        zip.closeEntry()
                    }
                }
            }
            Log.i(TAG, "Extracted from assets: ${info.name}")
            true
        } catch (e: Exception) {
            Log.w(TAG, "No asset zip for ${info.zipName}: ${e.message}")
            false
        }
    }

    private fun loadModelFromPath(path: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        try {
            model?.close()
            recognizer?.close()
            model = null
            recognizer = null

            model = Model(path)
            recognizer = Recognizer(model, sampleRate.toFloat())
            isModelLoading = false
            Log.i(TAG, "Vosk model loaded successfully: $path")
            onSuccess()
            onReady?.invoke()
        } catch (e: Exception) {
            isModelLoading = false
            errorMessage = "Failed to load model"
            Log.e(TAG, "Model load failed: ${e.message}")
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

        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuffer * 4
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
        lastPartialUpdateTime = 0L

        job = scope.launch {
            val buffer = ShortArray(BUFFER_SIZE)
            while (isActive && isListening && shouldContinue) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                if (read > 0) {
                    try {
                        recognizer?.acceptWaveForm(buffer, read)
                    } catch (e: Throwable) {
                        Log.e(TAG, "acceptWaveForm crashed", e)
                    }

                    try {
                        recognizer?.partialResult?.let { jsonStr ->
                            if (jsonStr.isNotBlank()) {
                                val json = JSONObject(jsonStr)
                                val text = json.optString("partial", "").trim()
                                val currentTime = System.currentTimeMillis()
                                if (text.isNotBlank() && (currentTime - lastPartialUpdateTime >= 300)) {
                                    currentPartialText = text
                                    lastPartialUpdateTime = currentTime
                                }
                            }
                        }
                    } catch (_: Throwable) {}

                    try {
                        recognizer?.result?.let { jsonStr ->
                            if (jsonStr.isNotBlank()) {
                                val json = JSONObject(jsonStr)
                                val rawText = json.optString("text", "").trim()
                                if (rawText.isNotBlank()) {
                                    val cleanedText = postProcessText(rawText)
                                    val segment = TranscriptSegment(
                                        text = cleanedText,
                                        timestampMillis = System.currentTimeMillis() - recordingStartTimeMs
                                    )
                                    transcriptSegments.add(segment)
                                    onTranscriptUpdate?.invoke(transcriptSegments.toList())
                                    currentPartialText = ""
                                    lastPartialUpdateTime = 0L
                                }
                            }
                        }
                    } catch (_: Throwable) {}
                }
                delay(50)
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
                            val rawText = json.optString("text", "").trim()
                            if (rawText.isNotBlank() && rawText != currentPartialText) {
                                val cleanedText = postProcessText(rawText)
                                transcriptSegments.add(
                                    TranscriptSegment(
                                        text = cleanedText,
                                        timestampMillis = System.currentTimeMillis() - recordingStartTimeMs
                                    )
                                )
                                onTranscriptUpdate?.invoke(transcriptSegments.toList())
                            }
                        }
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "Final result crash prevented", e)
                }
            }
            currentPartialText = ""
            lastPartialUpdateTime = 0L
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

        model?.close()
        recognizer?.close()
        model = null
        recognizer = null

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        job?.cancel()
        job = null
    }
}