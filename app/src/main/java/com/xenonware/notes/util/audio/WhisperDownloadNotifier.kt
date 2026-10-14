package com.xenonware.notes.util.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_DEFAULT
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.core.app.NotificationManagerCompat
import com.xenonware.notes.R
import kotlin.math.max

/**
 * Builds the progress/complete notifications for the Whisper model download.
 *
 * Kept separate from the foreground service so both the service and any in-app
 * surface can render identical text.
 */
object WhisperDownloadNotifier {

    const val CHANNEL_ID = "whisper_model_downloads"
    const val NOTIFICATION_ID = 8921
    private const val CHANNEL_NAME = "Model downloads"

    /** Creates the channel. Safe to call repeatedly. */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                // LOW: progress updates are frequent and non-urgent. A higher importance
                // would buzz the user on every percentage tick.
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Progress while downloading Whisper transcription models"
                setShowBadge(false)
                enableVibration(false)
            }
        )
    }

    /**
     * True when notifications are permitted. On API 33+ this requires the runtime
     * grant; below that it is always true.
     */
    fun canPost(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            true
        }

    /** "12 of 148 MB" style subtitle. */
    private fun sizeText(downloaded: Long, total: Long): String {
        val mb = 1_000_000.0
        return "%.0f of %.0f MB".format(downloaded / mb, total / mb)
    }

    fun progress(context: Context, state: WhisperDownloadState.Downloading): Notification {
        val percent = (state.progress * 100).toInt().coerceIn(0, 100)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Downloading ${WhisperModelType.BASE.displayName} model")
            .setContentText("$percent% · ${sizeText(state.bytesDownloaded, state.totalBytes)}")
            .setSmallIcon(R.drawable.ic_whisper_download)
            .setPriority(PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, percent, state.progress <= 0f)
            .build()
    }

    fun completed(context: Context, modelFile: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("${WhisperModelType.BASE.displayName} model ready")
            .setContentText("Transcription is now available offline.")
            .setSmallIcon(R.drawable.ic_whisper_download)
            .setPriority(PRIORITY_DEFAULT)
            // Auto-dismisses, so the user is not left with a stale success notification.
            .setAutoCancel(true)
            .setOngoing(false)
            .build()

    fun failed(context: Context, message: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("${WhisperModelType.BASE.displayName} download failed")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_whisper_download)
            .setPriority(PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()

    /** Progress fraction used by the UI bar; clamps to avoid >100% from a bad Content-Length. */
    fun clampProgress(progress: Float): Float = max(0f, progress.coerceAtMost(1f))
}
