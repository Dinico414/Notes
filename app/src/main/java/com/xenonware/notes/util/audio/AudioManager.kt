package com.xenonware.notes.util.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.io.IOException

enum class RecordingState {
    IDLE, RECORDING, PAUSED, STOPPED_UNSAVED, PLAYING, VIEWING_SAVED_AUDIO
}

class AudioRecorderManager(
    private val context: Context,
    private val onNewRecordingStarted: () -> Unit = {},
) {
    private var mediaRecorder: MediaRecorder? = null
    var audioFilePath: String? = null
        private set
    var uniqueAudioId: String? = null
        private set

    var recordingDurationMillis: Long by mutableLongStateOf(0L)
    var currentRecordingState: RecordingState by mutableStateOf(RecordingState.IDLE)
        private set
    var isPersistentAudio: Boolean by mutableStateOf(false)
        private set

    fun getMaxAmplitude(): Int = mediaRecorder?.maxAmplitude ?: 0

    fun startRecording() {

        if (currentRecordingState == RecordingState.RECORDING) {
            return
        }

        if (currentRecordingState == RecordingState.PAUSED) {
            mediaRecorder?.resume()
            currentRecordingState = RecordingState.RECORDING
            return
        }

        startNewRecording()
    }

    private fun startNewRecording() {

        uniqueAudioId = "audio_${System.currentTimeMillis()}"
        val audioFile = File(context.filesDir, "$uniqueAudioId.mp3")
        audioFilePath = audioFile.absolutePath
        isPersistentAudio = false

        onNewRecordingStarted()

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION") MediaRecorder()
        }

        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(192000)
            setOutputFile(audioFilePath)
            try {
                prepare()
                start()
                currentRecordingState = RecordingState.RECORDING
                recordingDurationMillis = 0L
            } catch (e: IOException) {
                e.printStackTrace()
                currentRecordingState = RecordingState.IDLE
            }
        }
    }

    fun pauseRecording() {
        if (currentRecordingState != RecordingState.RECORDING) {
            return
        }

        mediaRecorder?.pause()
        currentRecordingState = RecordingState.PAUSED
    }

    fun stopRecording() {
        if (currentRecordingState == RecordingState.RECORDING || currentRecordingState == RecordingState.PAUSED) {

            mediaRecorder?.release()
            mediaRecorder = null

            currentRecordingState = if (isPersistentAudio) RecordingState.VIEWING_SAVED_AUDIO else RecordingState.STOPPED_UNSAVED

            audioFilePath?.let { path ->
                File(path)
            }
        }
    }

    fun setInitialAudioFilePath(filePath: String) {
        audioFilePath = filePath
        uniqueAudioId = File(filePath).nameWithoutExtension
        isPersistentAudio = true
        currentRecordingState = RecordingState.VIEWING_SAVED_AUDIO
        recordingDurationMillis = 0L
    }

    fun restoreCachedState(
        cachedUniqueId: String?,
        cachedDuration: Long,
        cachedIsPersistent: Boolean
    ) {
        if (cachedUniqueId == null) {
            return
        }

        val cachedFile = File(context.filesDir, "$cachedUniqueId.mp3")

        if (cachedFile.exists()) {
            uniqueAudioId = cachedUniqueId
            audioFilePath = cachedFile.absolutePath
            recordingDurationMillis = cachedDuration
            isPersistentAudio = cachedIsPersistent
            currentRecordingState = if (cachedIsPersistent) {
                RecordingState.VIEWING_SAVED_AUDIO
            } else {
                RecordingState.STOPPED_UNSAVED
            }
        }
    }

    fun markAudioAsPersistent() {
        isPersistentAudio = true
    }

    fun deleteRecording() {

        audioFilePath = null
        uniqueAudioId = null
        recordingDurationMillis = 0L
        currentRecordingState = RecordingState.IDLE
        isPersistentAudio = false
    }

    fun resetState() {

        if (currentRecordingState == RecordingState.RECORDING || currentRecordingState == RecordingState.PAUSED) {
            mediaRecorder?.apply {
                try { stop() } catch (_: Exception) {}
                release()
            }
            mediaRecorder = null
        }
        onNewRecordingStarted()
        deleteRecording()
    }

    fun dispose() {

        mediaRecorder?.release()
        mediaRecorder = null

    }
}


object GlobalAudioPlayer {
    private var _instance: AudioPlayerManager? = null
    private val lock = Any()

    fun getInstance(): AudioPlayerManager = synchronized(lock) {
        _instance ?: AudioPlayerManager().also {
            _instance = it
        }
    }

    fun release() = synchronized(lock) {
        _instance?.dispose()
        _instance = null
    }
}

class AudioPlayerManager {

    var mediaPlayer: MediaPlayer? = null; private set
    var isPlaying by mutableStateOf(false); private set
    var currentPlaybackPositionMillis by mutableLongStateOf(0L)
    var totalAudioDurationMillis by mutableLongStateOf(0L)
    var currentFilePath: String? = null
        private set(value) {
            field = value
            if (value != null && value != field) currentPlaybackPositionMillis = 0L
        }
    var currentRecordingState by mutableStateOf(RecordingState.VIEWING_SAVED_AUDIO); private set

    fun playAudio(filePath: String) {

        if (currentFilePath != filePath || mediaPlayer == null) stopAudio()
        if (currentFilePath == filePath && isPlaying) {
            return
        }

        currentFilePath = filePath
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA).build())
                setDataSource(filePath)
                prepare()
                totalAudioDurationMillis = duration.toLong()
                setOnCompletionListener {
                    this@AudioPlayerManager.isPlaying = false
                    currentPlaybackPositionMillis = 0L
                    currentRecordingState = RecordingState.VIEWING_SAVED_AUDIO
                }
                start()
            }
            isPlaying = true
            currentRecordingState = RecordingState.PLAYING
        } catch (e: Exception) {
            e.printStackTrace()
            isPlaying = false
        }
    }

    fun pauseAudio() {
        mediaPlayer?.pause()
        isPlaying = false
    }

    fun resumeAudio() {

        if (mediaPlayer == null) {
            currentFilePath?.let {
                playAudio(it)
            }
            return
        }
        mediaPlayer?.start()
        isPlaying = true
        currentRecordingState = RecordingState.PLAYING
    }

    fun stopAudio() {

        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        isPlaying = false
        currentPlaybackPositionMillis = 0L
        currentFilePath = null
        currentRecordingState = RecordingState.VIEWING_SAVED_AUDIO
    }

    fun seekTo(positionMillis: Long) {
        mediaPlayer?.seekTo(positionMillis.toInt())
        currentPlaybackPositionMillis = positionMillis.coerceIn(0L, totalAudioDurationMillis)
    }

    fun getAudioDuration(filePath: String): Long = try {
        val p = MediaPlayer().apply { setDataSource(filePath); prepare() }
        p.duration.toLong().also {
            p.release()
        }
    } catch (_: Exception) {
        0L
    }

    fun dispose() {
        stopAudio()
    }
}