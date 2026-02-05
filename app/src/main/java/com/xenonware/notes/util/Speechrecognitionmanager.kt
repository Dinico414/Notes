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
        private const val RESTART_DELAY_MS = 300L
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var restartHandler: android.os.Handler? = null

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
    private var shouldContinue = false
    private var consecutiveErrors = 0

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

        currentRecordingStartTime = recordingStartTime
        shouldContinue = true
        consecutiveErrors = 0

        startRecognizer()
    }

    private fun startRecognizer() {
        try {
            // Clean up existing instance
            speechRecognizer?.destroy()
            speechRecognizer = null

            Log.d(TAG, "Creating new speech recognizer")
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createRecognitionListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)

                // Enable partial results
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

                // Request multiple results for better accuracy
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)

                // Timing parameters - MORE AGGRESSIVE for better detection
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15000L)

                // Prefer offline if available
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            }

            Log.d(TAG, "Starting speech recognizer with language: ${Locale.getDefault()}")
            speechRecognizer?.startListening(intent)
            isListening = true
            isTranscribing = true
            errorMessage = null

        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            errorMessage = "Failed to start: ${e.message}"
            onError?.invoke(errorMessage ?: "Unknown error")
            isListening = false
            isTranscribing = false
            scheduleRestart()
        }
    }

    private fun scheduleRestart() {
        if (!shouldContinue) {
            Log.d(TAG, "Not restarting - shouldContinue is false")
            return
        }

        consecutiveErrors++

        // Stop after 3 consecutive errors to avoid infinite loop
        if (consecutiveErrors >= 3) {
            Log.e(TAG, "Too many consecutive errors ($consecutiveErrors), stopping auto-restart")
            errorMessage = "Speech recognition failed multiple times. Please check microphone."
            onError?.invoke(errorMessage ?: "")
            shouldContinue = false
            return
        }

        Log.d(TAG, "Scheduling restart (attempt ${consecutiveErrors}/3)")

        if (restartHandler == null) {
            restartHandler = android.os.Handler(android.os.Looper.getMainLooper())
        }

        restartHandler?.postDelayed({
            if (shouldContinue) {
                Log.d(TAG, "Executing scheduled restart")
                startRecognizer()
            }
        }, RESTART_DELAY_MS)
    }

    /**
     * Stop listening for speech input
     */
    fun stopListening() {
        Log.d(TAG, "stopListening called")
        shouldContinue = false
        consecutiveErrors = 0

        try {
            restartHandler?.removeCallbacksAndMessages(null)
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
        shouldContinue = false
        consecutiveErrors = 0

        try {
            restartHandler?.removeCallbacksAndMessages(null)
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
        Log.d(TAG, "restartListening called")
        if (shouldContinue) {
            stopListening()
            shouldContinue = true // Re-enable after stop
            scheduleRestart()
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
        shouldContinue = false
        cancel()
        restartHandler = null
        speechRecognizer = null
        onTranscriptUpdate = null
        onError = null
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "✓ onReadyForSpeech - Listening for audio...")
            isTranscribing = true
            errorMessage = null
            consecutiveErrors = 0 // Reset on success
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "✓ onBeginningOfSpeech - Audio detected!")
            isTranscribing = true
            consecutiveErrors = 0 // Reset when speech detected
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Log audio levels to debug microphone issues
            if (rmsdB > 0) {
                Log.v(TAG, "Audio level: $rmsdB dB")
            }
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            Log.v(TAG, "onBufferReceived - ${buffer?.size ?: 0} bytes")
        }

        override fun onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech")
            isTranscribing = false
        }

        override fun onError(error: Int) {
            val errorMsg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error - check microphone"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error - recognizer issue"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Missing RECORD_AUDIO permission"
                SpeechRecognizer.ERROR_NETWORK -> "Network error - check internet"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected - speak louder or closer to mic"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
                else -> "Unknown error: $error"
            }

            Log.e(TAG, "✗ onError: $errorMsg (code: $error)")

            // Only show user-facing errors for serious issues
            if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
                error == SpeechRecognizer.ERROR_AUDIO) {
                errorMessage = errorMsg
                onError?.invoke(errorMsg)
            }

            isTranscribing = false
            currentPartialText = ""

            // Auto-restart for recoverable errors
            if (shouldContinue &&
                error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS &&
                error != SpeechRecognizer.ERROR_AUDIO) {

                Log.d(TAG, "Recoverable error, scheduling restart...")
                scheduleRestart()
            } else if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
                error == SpeechRecognizer.ERROR_AUDIO) {
                Log.e(TAG, "Fatal error, stopping recognition")
                shouldContinue = false
            }
        }

        override fun onResults(results: Bundle?) {
            Log.d(TAG, "✓ onResults called")
            consecutiveErrors = 0 // Reset on successful result

            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                Log.d(TAG, "Results received: ${matches.size} matches")
                matches.forEachIndexed { index, match ->
                    Log.d(TAG, "  [$index] $match")
                }

                if (matches.isNotEmpty()) {
                    val recognizedText = matches[0]
                    val timestamp = System.currentTimeMillis() - currentRecordingStartTime

                    // Get confidence score if available
                    val scores = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                    val confidence = scores?.firstOrNull() ?: 1.0f

                    Log.d(TAG, "✓ Recognized: '$recognizedText' at ${timestamp}ms (confidence: $confidence)")

                    if (recognizedText.isNotBlank()) {
                        val segment = TranscriptSegment(
                            text = recognizedText,
                            timestampMillis = timestamp,
                            confidence = confidence
                        )

                        transcriptSegments.add(segment)
                        Log.d(TAG, "✓ Segment added! Total: ${transcriptSegments.size}")
                        onTranscriptUpdate?.invoke(transcriptSegments)
                    }
                }
            }

            currentPartialText = ""

            // Continue listening
            if (shouldContinue) {
                Log.d(TAG, "Continuing transcription...")
                scheduleRestart()
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                if (matches.isNotEmpty()) {
                    val partial = matches[0]
                    Log.d(TAG, "Partial: '$partial'")
                    currentPartialText = partial
                    consecutiveErrors = 0 // Reset when we get partial results
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            Log.v(TAG, "onEvent: $eventType")
        }
    }
}