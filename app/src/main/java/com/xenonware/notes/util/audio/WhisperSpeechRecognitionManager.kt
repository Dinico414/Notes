package com.xenonware.notes.util.audio

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.File

/**
 * Local on-device speech recognition powered by whisper.cpp.
 *
 * Language is auto-detected. Supports mixed languages (e.g. Denglisch).
 * Pass "auto" to let Whisper detect, or a specific code like "en" / "de".
 */
class WhisperSpeechRecognitionManager(
    private val context: Context,
) {
    companion object {
        private const val TAG = "WhisperLocal"
        private const val TRANSCRIPTION_TIMEOUT_MS = 120_000L // 2 minutes max
    }

    private val modelManager = WhisperModelManager(context)
    private val ioScope = CoroutineScope(Dispatchers.IO)

    private var contextPtr: Long = 0L
    private var loadedModelType: WhisperModelType? = null

    var selectedModelType: WhisperModelType = WhisperModelType.TINY

    // ── Observable state ────────────────────────────────────────────────────

    var isTranscribing by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var isModelLoading by mutableStateOf(false)
        private set

    var modelLoadProgress by mutableFloatStateOf(0f)
        private set

    val currentPartialText: String = ""

    var onTranscriptUpdate: ((List<TranscriptSegment>) -> Unit)? = null

    private val segments = mutableListOf<TranscriptSegment>()

    // ── Language ────────────────────────────────────────────────────────────

    /**
     * Use "de" as default — Whisper handles English words within German speech
     * natively (Denglisch works perfectly). "auto" causes the tiny model to hang.
     */
    private var language: String = "de"

    fun setLanguage(lang: String) {
        language = lang
    }

    // ── Public helpers ──────────────────────────────────────────────────────

    fun isAvailable(): Boolean {
        val tinyAvailable = modelManager.isModelDownloaded(WhisperModelType.TINY)
        val baseAvailable = modelManager.isModelDownloaded(WhisperModelType.BASE)
        Log.d(TAG, "isAvailable() → tiny=$tinyAvailable, base=$baseAvailable")
        return tinyAvailable || baseAvailable
    }

    suspend fun ensureReady() {
        Log.i(TAG, "ensureReady(): extracting tiny model if needed …")
        val ok = modelManager.ensureTinyModelExtracted()
        Log.i(TAG, "ensureReady(): extraction result=$ok, isAvailable=${isAvailable()}")
    }

    // ── Model lifecycle ─────────────────────────────────────────────────────

    private suspend fun ensureModelLoaded(): Boolean {
        val wantedType = selectedModelType
        Log.i(TAG, "ensureModelLoaded(): want=${wantedType.displayName}, current=${loadedModelType?.displayName}, ptr=$contextPtr")

        if (contextPtr != 0L && loadedModelType == wantedType) {
            Log.i(TAG, "Model already loaded")
            return true
        }

        if (contextPtr != 0L) {
            Log.i(TAG, "Releasing previous model: ${loadedModelType?.displayName}")
            WhisperLib.freeContext(contextPtr)
            contextPtr = 0L
            loadedModelType = null
        }

        if (wantedType == WhisperModelType.TINY) {
            val extracted = modelManager.ensureTinyModelExtracted()
            Log.i(TAG, "Tiny model extraction: $extracted")
            if (!extracted) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Failed to extract Tiny model. Is ggml-tiny.bin in assets?"
                }
                return false
            }
        }

        val modelFile = modelManager.getModelFile(wantedType)
        Log.i(TAG, "Model file: ${modelFile.absolutePath}, exists=${modelFile.exists()}, size=${modelFile.length()}")

        if (!modelFile.exists() || modelFile.length() == 0L) {
            val msg = "Model file missing: ${wantedType.displayName}"
            Log.e(TAG, msg)
            withContext(Dispatchers.Main) { errorMessage = msg }
            return false
        }

        withContext(Dispatchers.Main) { isModelLoading = true }

        return try {
            Log.i(TAG, "Loading native context …")
            val ptr = withContext(Dispatchers.IO) {
                WhisperLib.initContext(modelFile.absolutePath)
            }
            Log.i(TAG, "initContext returned ptr=$ptr")

            if (ptr == 0L) {
                Log.e(TAG, "initContext returned 0 — model may be corrupt")
                withContext(Dispatchers.Main) {
                    errorMessage = "Failed to load model"
                    isModelLoading = false
                }
                false
            } else {
                contextPtr = ptr
                loadedModelType = wantedType
                withContext(Dispatchers.Main) { isModelLoading = false }
                Log.i(TAG, "Model ready: ${wantedType.displayName}")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Model load error", e)
            withContext(Dispatchers.Main) {
                errorMessage = "Model load error: ${e.message}"
                isModelLoading = false
            }
            false
        }
    }

    // ── Transcription ───────────────────────────────────────────────────────

    fun transcribeFile(
        audioId: String,
        onDone: (List<TranscriptSegment>) -> Unit = {},
    ) {
        val file = File(context.filesDir, "$audioId.mp3")
        Log.i(TAG, "transcribeFile: audioId=$audioId, exists=${file.exists()}, size=${file.length()}, lang=$language")

        if (!file.exists() || file.length() == 0L) {
            Log.e(TAG, "Audio file not found")
            ioScope.launch(Dispatchers.Main) {
                errorMessage = "Audio file not found"
                Toast.makeText(context, "Audio file not found", Toast.LENGTH_LONG).show()
            }
            return
        }

        if (!isAvailable()) {
            Log.e(TAG, "No model available")
            ioScope.launch(Dispatchers.Main) {
                errorMessage = "No Whisper model available"
                Toast.makeText(context, "Whisper model not found!", Toast.LENGTH_LONG).show()
            }
            return
        }

        if (isTranscribing) {
            Log.w(TAG, "Already transcribing — ignoring")
            return
        }

        ioScope.launch(Dispatchers.Main) { isTranscribing = true; errorMessage = null }

        ioScope.launch(Dispatchers.IO) {
            try {
                if (!ensureModelLoaded()) {
                    withContext(Dispatchers.Main) {
                        isTranscribing = false
                        Toast.makeText(context, "Failed to load model", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                // ── Decode audio ────────────────────────────────────────
                Log.i(TAG, "Decoding audio …")
                val audioData = AudioDecoder.decodeToFloat16kMono(file.absolutePath)

                if (audioData == null || audioData.isEmpty()) {
                    Log.e(TAG, "Audio decode failed")
                    withContext(Dispatchers.Main) {
                        errorMessage = "Failed to decode audio"
                        isTranscribing = false
                        Toast.makeText(context, "Audio decode failed", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                Log.i(TAG, "Decoded ${audioData.size} samples (${audioData.size / 16000f}s)")

                // ── Run inference with timeout ──────────────────────────
                val numThreads = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
                Log.i(TAG, "Starting inference: lang=$language, threads=$numThreads")

                val jsonResult = try {
                    withTimeout(TRANSCRIPTION_TIMEOUT_MS) {
                        withContext(Dispatchers.IO) {
                            WhisperLib.transcribeAudio(
                                contextPtr, audioData, language, numThreads
                            )
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.e(TAG, "Transcription timed out after ${TRANSCRIPTION_TIMEOUT_MS}ms")
                    // Force reload model on next attempt since state may be corrupted
                    WhisperLib.freeContext(contextPtr)
                    contextPtr = 0L
                    loadedModelType = null
                    withContext(Dispatchers.Main) {
                        errorMessage = "Transcription timed out. Try a shorter recording."
                        isTranscribing = false
                        Toast.makeText(context, "Transcription timed out", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                Log.i(TAG, "Raw result: ${jsonResult.take(500)}")

                val result = parseResult(jsonResult)
                Log.i(TAG, "Parsed ${result.size} segment(s)")

                if (result.isEmpty()) {
                    Log.w(TAG, "0 segments. Full JSON: $jsonResult")
                }

                saveTranscript(context, audioId, result)
                Log.i(TAG, "Transcript saved")

                withContext(Dispatchers.Main) {
                    segments.clear()
                    segments.addAll(result)
                    isTranscribing = false
                    onTranscriptUpdate?.invoke(segments.toList())
                    onDone(segments.toList())

                    if (result.isNotEmpty()) {
                        Log.i(TAG, "Delivered ${result.size} segments to UI")
                    } else {
                        errorMessage = "No speech detected"
                    }
                }
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Native library error", e)
                withContext(Dispatchers.Main) {
                    errorMessage = "Native library error"
                    isTranscribing = false
                    Toast.makeText(context, "whisper_jni failed to load", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Transcription error", e)
                withContext(Dispatchers.Main) {
                    errorMessage = "Transcription failed: ${e.message}"
                    isTranscribing = false
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun parseResult(json: String): List<TranscriptSegment> {
        val result = mutableListOf<TranscriptSegment>()
        try {
            val obj = JSONObject(json)
            if (obj.has("error")) {
                Log.e(TAG, "Whisper error: ${obj.getString("error")}")
                return result
            }
            val segs = obj.optJSONArray("segments") ?: run {
                Log.w(TAG, "No segments array. Keys: ${obj.keys().asSequence().toList()}")
                return result
            }
            for (i in 0 until segs.length()) {
                val seg = segs.getJSONObject(i)
                val text = seg.optString("text", "").trim()
                val startMs = seg.optLong("start_ms", 0L)
                if (text.isNotBlank()) {
                    result.add(TranscriptSegment(text = text, timestampMillis = startMs))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse error", e)
        }
        return result
    }

    fun clearTranscript() {
        segments.clear()
        onTranscriptUpdate?.invoke(emptyList())
    }

    fun loadTranscript(existing: List<TranscriptSegment>) {
        segments.clear()
        segments.addAll(existing)
        onTranscriptUpdate?.invoke(segments.toList())
    }

    @Suppress("UNUSED_PARAMETER")
    fun startListening(recordingStartTime: Long = System.currentTimeMillis()) {}
    fun stopListening() {}
    fun restartListening() {}
    fun cancel() {}

    fun dispose() {
        if (contextPtr != 0L) {
            WhisperLib.freeContext(contextPtr)
            contextPtr = 0L
            loadedModelType = null
        }
    }
}