package com.xenonware.notes.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.xenonware.notes.util.audio.WhisperDownloadNotifier
import com.xenonware.notes.util.audio.WhisperDownloadState
import com.xenonware.notes.util.audio.WhisperModelDownloads
import com.xenonware.notes.util.audio.WhisperModelType

/**
 * Keeps the model download alive while the app is backgrounded and mirrors progress
 * into a notification.
 *
 * The actual transfer is owned by [WhisperModelDownloads]; this service exists purely
 * to (a) hold a foreground-service slot so the OS does not kill the process mid-download,
 * and (b) drive the notification. It observes [WhisperModelDownloads.state] rather than
 * owning the coroutine, so the download survives independently of this service.
 */
class WhisperDownloadService : Service() {

    companion object {
        private const val TAG = "WhisperDlService"
        private const val EXTRA_MODEL = "model"

        // Fixed type for API 34+; dataSync is the correct category for a download.
        private val FOREGROUND_TYPE: Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }

        fun start(context: Context, modelType: WhisperModelType = WhisperModelType.BASE) {
            val intent = Intent(context, WhisperDownloadService::class.java)
                .putExtra(EXTRA_MODEL, modelType.name)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WhisperDownloadService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        WhisperDownloadNotifier.ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val modelType = intent?.getStringExtra(EXTRA_MODEL)
            ?.let { runCatching { WhisperModelType.valueOf(it) }.getOrNull() }
            ?: WhisperModelType.BASE

        // Must post a notification promptly or the OS kills us on API 26+.
        postForegroundNotification(
            WhisperDownloadNotifier.progress(
                this,
                WhisperDownloadState.Downloading(0f, 0L, modelType.approximateBytes),
            )
        )

        // Kick off (or attach to) the download. Idempotent.
        WhisperModelDownloads.start(applicationContext, modelType)

        WhisperModelDownloads.progressListener = { state ->
            when (state) {
                is WhisperDownloadState.Downloading ->
                    postForegroundNotification(WhisperDownloadNotifier.progress(this, state))

                is WhisperDownloadState.Completed -> {
                    showTerminalNotification(WhisperDownloadNotifier.completed(this, state.modelFile))
                    tearDown()
                }

                is WhisperDownloadState.Failed -> {
                    showTerminalNotification(WhisperDownloadNotifier.failed(this, state.message))
                    tearDown()
                }

                WhisperDownloadState.Idle -> tearDown()
            }
        }

        // START_NOT_STICKY: a download is not worth restarting silently after a kill.
        return START_NOT_STICKY
    }

    private fun postForegroundNotification(notification: Notification) {
        try {
            // minSdk is 29, so the typed overload is always available.
            startForeground(
                WhisperDownloadNotifier.NOTIFICATION_ID,
                notification,
                FOREGROUND_TYPE,
            )
        } catch (e: Exception) {
            // e.g. notifications revoked mid-download, or a foreground-service-start
            // restriction. The transfer itself is unaffected, so log and carry on
            // rather than taking down the service.
            Log.w(TAG, "Could not post foreground notification", e)
        }
    }

    /** Posts a non-ongoing notification after detaching the foreground notification. */
    private fun showTerminalNotification(notification: Notification) {
        // canPost() wraps areNotificationsEnabled(), which reflects the runtime
        // POST_NOTIFICATIONS grant on API 33+. Guard rather than assume.
        if (!WhisperDownloadNotifier.canPost(this)) return
        try {
            NotificationManagerCompat.from(this)
                .notify(WhisperDownloadNotifier.NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing POST_NOTIFICATIONS permission", e)
        } catch (e: Exception) {
            Log.w(TAG, "Could not post terminal notification", e)
        }
    }

    private fun tearDown() {
        WhisperModelDownloads.progressListener = null
        stopForegroundCompat()
        stopSelf()
    }

    private fun stopForegroundCompat() {
        // minSdk is 29, so the STOP_FOREGROUND_DETACH overload is always available.
        stopForeground(STOP_FOREGROUND_DETACH)
    }

    override fun onDestroy() {
        WhisperModelDownloads.progressListener = null
        super.onDestroy()
    }
}
