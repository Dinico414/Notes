package com.xenonware.notes.util.audio

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Describes the state of the (single) in-flight Pro model download.
 */
sealed interface WhisperDownloadState {

    /** Nothing in flight. */
    data object Idle : WhisperDownloadState

    /**
     * @param progress 0f..1f
     * @param bytesDownloaded raw counter, for "12 of 148 MB" style labels
     * @param totalBytes expected size; may be the [WhisperModelType.approximateBytes]
     *        fallback when the server omits Content-Length
     */
    data class Downloading(
        val progress: Float,
        val bytesDownloaded: Long,
        val totalBytes: Long,
    ) : WhisperDownloadState

    /** Terminal states. [Failed.message] carries a user-displayable reason. */
    data class Completed(val modelFile: String) : WhisperDownloadState
    data class Failed(val message: String) : WhisperDownloadState
}

/**
 * App-scoped owner of the Whisper model downloads.
 *
 * **Why this exists:** the download used to be launched from
 * `rememberCoroutineScope()` inside the note sheet, which meant closing the sheet
 * cancelled the coroutine and lost all progress. This object owns its own
 * [CoroutineScope] (backed by a [SupervisorJob] on [Dispatchers.IO]), so the transfer
 * continues regardless of which screen is on top, and any screen can observe
 * [state] to re-attach its UI.
 *
 * Follows the same singleton shape as [GlobalAudioPlayer].
 */
object WhisperModelDownloads {

    private const val TAG = "WhisperDownloads"

    /** Outlives any individual screen; cleared only by process death. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<WhisperDownloadState>(WhisperDownloadState.Idle)

    /** Observable progress, safe to collect from any composable. */
    val state: StateFlow<WhisperDownloadState> = _state.asStateFlow()

    private var activeJob: Job? = null

    /**
     * Optional hook so a foreground service can mirror progress into a notification
     * without this class needing to know about Android notifications.
     */
    var progressListener: ((WhisperDownloadState) -> Unit)? = null

    private fun publish(newState: WhisperDownloadState) {
        _state.value = newState
        progressListener?.invoke(newState)
    }

    val isDownloading: Boolean get() = _state.value is WhisperDownloadState.Downloading

    /**
     * Starts the Pro model download, or no-ops if one is already running.
     *
     * Idempotent by design: re-entering the sheet while a download is in flight calls
     * this again, and it simply returns the existing job rather than starting a second.
     */
    @Synchronized
    fun start(context: Context, modelType: WhisperModelType = WhisperModelType.BASE) {
        if (activeJob?.isActive == true) {
            Log.i(TAG, "Download already in flight; ignoring duplicate start")
            return
        }

        // Application context so we never hold an Activity leak.
        val appContext = context.applicationContext
        val manager = WhisperModelManager(appContext)

        Log.i(TAG, "Starting download for ${modelType.displayName}")
        publish(
            WhisperDownloadState.Downloading(
                progress = 0f,
                bytesDownloaded = 0L,
                totalBytes = modelType.approximateBytes,
            )
        )

        activeJob = scope.launch {
            val success = try {
                manager.downloadModel(modelType) { progress ->
                    // Guard against divide-by-zero and report raw bytes too.
                    val total = modelType.approximateBytes
                    publish(
                        WhisperDownloadState.Downloading(
                            progress = progress.coerceIn(0f, 1f),
                            bytesDownloaded = (total * progress).toLong().coerceAtLeast(0L),
                            totalBytes = total,
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download threw", e)
                publish(WhisperDownloadState.Failed(e.message ?: "Download failed"))
                return@launch
            }

            publish(
                if (success) {
                    Log.i(TAG, "Download completed")
                    WhisperDownloadState.Completed(manager.getModelFile(modelType).name)
                } else {
                    Log.e(TAG, "Download reported failure")
                    WhisperDownloadState.Failed("Download failed — check your connection and try again.")
                }
            )
        }
    }

    /** Clears a terminal state so the UI can return to idle after showing it. */
    @Synchronized
    fun acknowledgeTerminalState() {
        if (_state.value !is WhisperDownloadState.Downloading) {
            _state.value = WhisperDownloadState.Idle
        }
    }

    /** Cancels an in-flight download. Safe to call when idle. */
    @Synchronized
    fun cancel() {
        activeJob?.cancel()
        activeJob = null
        publish(WhisperDownloadState.Idle)
    }
}
