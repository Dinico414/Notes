package com.xenonware.notes.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Represents a segment of transcribed text with its timestamp
 */
data class TranscriptSegment(
    val text: String,
    val timestampMillis: Long,
    val confidence: Float = 1.0f
)

/**
 * Manages speech recognition for audio recording transcription.
 * Uses Android's SpeechRecognizer to convert speech to text in real-time.
 */
class SpeechRecognitionManager(private val context: Context) {

    companion object {
        private const val TAG = "SpeechRecognition"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    // State
    var transcriptSegments = mutableListOf<TranscriptSegment>()
        private set

    var currentPartialText by mutableStateOf("")
        private set

    var isTranscribing by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var onTranscriptUpdate: ((List<TranscriptSegment>) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private var currentRecordingStartTime: Long = 0L
    private var shouldRestart = false

    /**
     * Check if speech recognition is available on this device
     */
    fun isAvailable(): Boolean {
        val available = SpeechRecognizer.isRecognitionAvailable(context)
        Log.d(TAG, "Speech recognition available: $available")
        return available
    }

    /**
     * Start listening for speech input
     */
    fun startListening(recordingStartTime: Long = System.currentTimeMillis()) {
        Log.d(TAG, "startListening called - isListening: $isListening")

        if (!isAvailable()) {
            val error = "Speech recognition not available on this device"
            Log.e(TAG, error)
            errorMessage = error
            onError?.invoke(error)
            return
        }

        if (isListening) {
            Log.d(TAG, "Already listening, stopping first")
            stopListening()
        }

        currentRecordingStartTime = recordingStartTime
        shouldRestart = true

        try {
            // Destroy old instance if exists
            speechRecognizer?.destroy()

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createRecognitionListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 10000L)
            }

            Log.d(TAG, "Starting speech recognizer")
            speechRecognizer?.startListening(intent)
            isListening = true
            isTranscribing = true
            errorMessage = null

        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            errorMessage = "Failed to start speech recognition: ${e.message}"
            onError?.invoke(errorMessage ?: "Unknown error")
            isListening = false
            isTranscribing = false
        }
    }

    /**
     * Stop listening for speech input
     */
    fun stopListening() {
        Log.d(TAG, "stopListening called")
        shouldRestart = false
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping listener", e)
        }
        isListening = false
        isTranscribing = false
        currentPartialText = ""
    }

    /**
     * Cancel speech recognition
     */
    fun cancel() {
        Log.d(TAG, "cancel called")
        shouldRestart = false
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling", e)
        }
        speechRecognizer = null
        isListening = false
        isTranscribing = false
        currentPartialText = ""
    }

    /**
     * Restart listening (useful for continuous transcription)
     */
    fun restartListening() {
        Log.d(TAG, "restartListening called - shouldRestart: $shouldRestart")
        if (shouldRestart && isListening) {
            stopListening()
            // Small delay to ensure proper cleanup
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (shouldRestart) {
                    startListening(currentRecordingStartTime)
                }
            }, 500)
        }
    }

    /**
     * Clear all transcript segments
     */
    fun clearTranscript() {
        Log.d(TAG, "clearTranscript called")
        transcriptSegments.clear()
        currentPartialText = ""
        errorMessage = null
        onTranscriptUpdate?.invoke(transcriptSegments)
    }

    /**
     * Load existing transcript segments (e.g., from saved note)
     */
    fun loadTranscript(segments: List<TranscriptSegment>) {
        Log.d(TAG, "loadTranscript called with ${segments.size} segments")
        transcriptSegments.clear()
        transcriptSegments.addAll(segments)
        onTranscriptUpdate?.invoke(transcriptSegments)
    }

    /**
     * Get full transcript as plain text
     */
    fun getFullTranscriptText(): String {
        return transcriptSegments.joinToString("\n") { it.text }
    }

    /**
     * Release resources
     */
    fun dispose() {
        Log.d(TAG, "dispose called")
        shouldRestart = false
        cancel()
        speechRecognizer = null
        onTranscriptUpdate = null
        onError = null
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "onReadyForSpeech")
            isTranscribing = true
            errorMessage = null
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech")
            isTranscribing = true
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Audio level - could be used for visualization
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            Log.d(TAG, "onBufferReceived")
        }

        override fun onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech")
            isTranscribing = false
        }

        override fun onError(error: Int) {
            val errorMsg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                else -> "Unknown error: $error"
            }

            Log.e(TAG, "onError: $errorMsg (code: $error)")

            // Only show errors that aren't normal (like no match or timeout)
            if (error != SpeechRecognizer.ERROR_NO_MATCH &&
                error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                errorMessage = errorMsg
                onError?.invoke(errorMsg)
            }

            isTranscribing = false
            currentPartialText = ""

            // Auto-restart for continuous transcription (except for critical errors)
            if (shouldRestart && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS &&
                error != SpeechRecognizer.ERROR_CLIENT) {
                Log.d(TAG, "Auto-restarting after error")
                restartListening()
            }
        }

        override fun onResults(results: Bundle?) {
            Log.d(TAG, "onResults called")
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                Log.d(TAG, "Results: $matches")
                if (matches.isNotEmpty()) {
                    val recognizedText = matches[0]
                    val timestamp = System.currentTimeMillis() - currentRecordingStartTime

                    // Get confidence score if available
                    val scores = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                    val confidence = scores?.firstOrNull() ?: 1.0f

                    Log.d(TAG, "Recognized text: '$recognizedText' at ${timestamp}ms with confidence $confidence")

                    if (recognizedText.isNotBlank()) {
                        val segment = TranscriptSegment(
                            text = recognizedText,
                            timestampMillis = timestamp,
                            confidence = confidence
                        )

                        transcriptSegments.add(segment)
                        Log.d(TAG, "Added segment, total segments: ${transcriptSegments.size}")
                        onTranscriptUpdate?.invoke(transcriptSegments)
                    }
                }
            }

            currentPartialText = ""

            // Restart for continuous transcription
            if (shouldRestart) {
                Log.d(TAG, "Restarting for continuous transcription")
                restartListening()
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                if (matches.isNotEmpty()) {
                    val partial = matches[0]
                    Log.d(TAG, "Partial result: '$partial'")
                    currentPartialText = partial
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            Log.d(TAG, "onEvent: $eventType")
        }
    }
}