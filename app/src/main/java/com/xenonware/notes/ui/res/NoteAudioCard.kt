package com.xenonware.notes.ui.res

// Added imports for permission handling
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.xenonware.notes.ui.layouts.QuicksandTitleVariable
import com.xenonware.notes.ui.layouts.notes.AudioViewType
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit


enum class RecordingState {
    IDLE, RECORDING, PAUSED, STOPPED_UNSAVED, PLAYING, VIEWING_SAVED_AUDIO
}

class AudioRecorderManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    var audioFilePath: String? = null
        private set
    var uniqueAudioId: String? = null // New: To store the unique ID for the audio file
        private set

    var recordingDurationMillis: Long by mutableLongStateOf(0L)

    var currentRecordingState: RecordingState by mutableStateOf(RecordingState.IDLE)
        private set
    
    var isPersistentAudio: Boolean by mutableStateOf(false) // New: Flag to indicate if the audio is persistently saved
        private set

    fun startRecording() {
        if (currentRecordingState == RecordingState.RECORDING) return
        if (currentRecordingState == RecordingState.PAUSED) {
            mediaRecorder?.resume()
            currentRecordingState = RecordingState.RECORDING
            println("Resumed recording")
            return
        }
        startNewRecording()
    }

    private fun startNewRecording() {
        uniqueAudioId = "audio_${System.currentTimeMillis()}" // Generate a unique ID
        val audioFile = File(context.filesDir, "$uniqueAudioId.mp3") // Use ID in filename
        audioFilePath = audioFile.absolutePath
        isPersistentAudio = false // New recordings are not persistent until explicitly saved

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION") MediaRecorder()
        }

        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(audioFilePath)
            try {
                prepare()
                start()
                currentRecordingState = RecordingState.RECORDING
                recordingDurationMillis = 0L // Reset on new recording
                println("Started recording to: $audioFilePath with ID: $uniqueAudioId")
            } catch (e: IOException) {
                println("Failed to start recording: ${e.message}")
                currentRecordingState = RecordingState.IDLE
            }
        }
    }

    fun pauseRecording() {
        if (currentRecordingState != RecordingState.RECORDING) return
        mediaRecorder?.pause()
        currentRecordingState = RecordingState.PAUSED
        println("Paused recording")
    }


    fun stopRecording() {
        if (currentRecordingState == RecordingState.RECORDING || currentRecordingState == RecordingState.PAUSED) {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            currentRecordingState = RecordingState.STOPPED_UNSAVED
            println("Stopped recording")
        }
    }

    fun setInitialAudioFilePath(filePath: String) {
        audioFilePath = filePath
        // Extract uniqueAudioId from the filePath (assuming filename is uniqueId.mp3)
        uniqueAudioId = File(filePath).nameWithoutExtension 
        isPersistentAudio = true // Audio loaded from initial path is considered persistent
        currentRecordingState = RecordingState.VIEWING_SAVED_AUDIO
        // We don't know the duration here, the AudioPlayerManager will set it when played.
        recordingDurationMillis = 0L 
        println("Set initial audio file path: $filePath, extracted ID: $uniqueAudioId")
    }

    fun markAudioAsPersistent() {
        isPersistentAudio = true
        println("Audio with ID: $uniqueAudioId marked as persistent.")
    }

    fun deleteRecording() {
        // Only delete the file if it's NOT marked as persistent.
        // If isPersistentAudio is true, it means the app considers this file saved externally
        // and won't delete it on cleanup of this manager instance.
        if (!isPersistentAudio && audioFilePath != null) {
            val fileToDelete = File(audioFilePath!!)
            if (fileToDelete.exists()) {
                fileToDelete.delete()
                println("Temporary recording deleted: $audioFilePath")
            }
        } else if (isPersistentAudio) {
            println("Persistent audio not deleted: $audioFilePath")
        }
        audioFilePath = null
        uniqueAudioId = null // Reset the ID too
        recordingDurationMillis = 0L
        currentRecordingState = RecordingState.IDLE
        isPersistentAudio = false // Reset the flag for future uses
    }

    fun resetState() {
        // Stop any ongoing recording/playback before resetting
        if (currentRecordingState == RecordingState.RECORDING || currentRecordingState == RecordingState.PAUSED) {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
        }
        deleteRecording() // This will now intelligently delete only non-persistent files
        currentRecordingState = RecordingState.IDLE
        isPersistentAudio = false // Reset the flag
        uniqueAudioId = null // Ensure ID is reset here too
    }


    fun dispose() {
        mediaRecorder?.release()
        mediaRecorder = null
        deleteRecording() // Intelligent deletion based on isPersistentAudio
    }
}


class AudioPlayerManager {
    var currentRecordingState: RecordingState by mutableStateOf(RecordingState.IDLE)

    var isPlaying: Boolean by mutableStateOf(false)
    var currentPlaybackPositionMillis: Long by mutableLongStateOf(0L)
    var totalAudioDurationMillis: Long by mutableLongStateOf(0L)

    private var mediaPlayer: MediaPlayer? = null

    fun playAudio(filePath: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                start()
                this@AudioPlayerManager.totalAudioDurationMillis = duration.toLong()
                this@AudioPlayerManager.isPlaying = true
                currentRecordingState = RecordingState.PLAYING
                setOnCompletionListener {
                    stopAudio() // This will now correctly trigger the logic in NoteAudioCard
                    this@AudioPlayerManager.currentPlaybackPositionMillis = 0L
                    // Don't set currentRecordingState here, let NoteAudioCard handle the transition
                }
            }
            println("Playing audio from: $filePath")
        } catch (e: IOException) {
            println("Failed to play audio: ${e.message}")
            this@AudioPlayerManager.isPlaying = false
            currentRecordingState = RecordingState.IDLE // If playback fails, player is idle
        }
    }

    fun stopAudio() {
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
        isPlaying = false
        currentPlaybackPositionMillis = 0L
        currentRecordingState = RecordingState.IDLE // Player itself is now idle
        println("Stopping audio playback. Player Manager state set to IDLE.")
    }

    fun getAudioDuration(filePath: String): Long {
        var duration: Long = 0L
        val tempPlayer = MediaPlayer()
        try {
            tempPlayer.setDataSource(filePath)
            tempPlayer.prepare()
            duration = tempPlayer.duration.toLong()
        } catch (e: IOException) {
            println("Failed to get audio duration: ${e.message}")
        } finally {
            tempPlayer.release()
        }
        return duration
    }

    fun dispose() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalHazeMaterialsApi::class
)
@Composable
fun NoteAudioCard(
    audioTitle: String,
    onAudioTitleChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit, // (title, uniqueAudioId) - now passes the unique ID
    cardBackgroundColor: Color = colorScheme.surfaceContainer,
    toolbarHeight: Dp,
    saveTrigger: Boolean,
    onSaveTriggerConsumed: () -> Unit,
    selectedAudioViewType: AudioViewType, // Current selected view type from parent
    initialAudioFilePath: String? = null, // New parameter for existing audio files (full path)
) {
    val hazeState = remember { HazeState() }
    val hazeThinColor = colorScheme.surfaceDim
    val context = LocalContext.current

    val recorderManager = remember { AudioRecorderManager(context) }
    val playerManager = remember { AudioPlayerManager() }

    var recordingState by remember { mutableStateOf(RecordingState.IDLE) }

    // Sync recordingState from managers
    LaunchedEffect(recorderManager.currentRecordingState) {
        recordingState = recorderManager.currentRecordingState
        // When recorderManager stops (e.g., to STOPPED_UNSAVED), also update playerManager's state
        if (recordingState == RecordingState.STOPPED_UNSAVED) {
            playerManager.currentRecordingState = RecordingState.STOPPED_UNSAVED
        } else if (recordingState == RecordingState.IDLE) {
            playerManager.currentRecordingState = RecordingState.IDLE
        } else if (recordingState == RecordingState.VIEWING_SAVED_AUDIO) {
            playerManager.currentRecordingState = RecordingState.VIEWING_SAVED_AUDIO
        }
    }
    LaunchedEffect(playerManager.currentRecordingState) {
        // Only update if player manager is actively changing state to PLAYING, otherwise recorderManager is primary
        if (playerManager.currentRecordingState == RecordingState.PLAYING) {
            recordingState = playerManager.currentRecordingState
        } else if (playerManager.currentRecordingState == RecordingState.IDLE) {
            // When player stops, determine the next overall card state based on recorder's state
            if (recorderManager.currentRecordingState == RecordingState.VIEWING_SAVED_AUDIO) {
                recordingState = RecordingState.VIEWING_SAVED_AUDIO
            } else if (recorderManager.currentRecordingState == RecordingState.STOPPED_UNSAVED) {
                recordingState = RecordingState.STOPPED_UNSAVED
            } else {
                recordingState = RecordingState.IDLE
            }
        }
    }

    // Handle initial audio file loading
    LaunchedEffect(initialAudioFilePath) {
        println("NoteAudioCard: LaunchedEffect(initialAudioFilePath) triggered with: $initialAudioFilePath")
        if (initialAudioFilePath != null) {
            // initialAudioFilePath here is the full path.
            // setInitialAudioFilePath will extract the ID, set audioFilePath internally, and mark as persistent.
            recorderManager.setInitialAudioFilePath(initialAudioFilePath)
            playerManager.totalAudioDurationMillis = playerManager.getAudioDuration(initialAudioFilePath)
            playerManager.currentRecordingState = RecordingState.VIEWING_SAVED_AUDIO
            recordingState = RecordingState.VIEWING_SAVED_AUDIO
        } else {
            // If initialAudioFilePath becomes null, reset to IDLE, e.g., if parent changes mind
            if (recorderManager.currentRecordingState != RecordingState.IDLE) {
                recorderManager.resetState()
            }
            if (playerManager.currentRecordingState != RecordingState.IDLE) {
                playerManager.stopAudio() // Ensure player is stopped
                playerManager.currentRecordingState = RecordingState.IDLE
            }
            recordingState = RecordingState.IDLE
        }
    }


    // Simulation for recording timer
    LaunchedEffect(recordingState) {
        if (recordingState == RecordingState.RECORDING) {
            // No reset here, as it's handled in startNewRecording
            while (isActive && recordingState == RecordingState.RECORDING) {
                delay(1000L)
                recorderManager.recordingDurationMillis += 1000L
            }
        }
    }

    // Simulation for playback timer
    LaunchedEffect(recordingState) {
        if (recordingState == RecordingState.PLAYING) {
            while (isActive && recordingState == RecordingState.PLAYING && playerManager.isPlaying) {
                playerManager.currentPlaybackPositionMillis =
                    (playerManager.currentPlaybackPositionMillis + 1000L).coerceAtMost(playerManager.totalAudioDurationMillis)
                delay(1000L)
            }
        } else {
            playerManager.currentPlaybackPositionMillis = 0L // Reset when not playing
        }
    }


    // Permission launcher
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            recorderManager.startRecording()
        } else {
            println("Microphone permission denied")
        }
    }

    LaunchedEffect(saveTrigger) {
        if (saveTrigger) {
            println("NoteAudioCard: Save trigger activated. Audio ID: ${recorderManager.uniqueAudioId}")
            recorderManager.stopRecording()
            playerManager.stopAudio()
            onSave(audioTitle, recorderManager.uniqueAudioId ?: "") // Pass the unique ID
            recorderManager.markAudioAsPersistent() // Mark the current recording as persistent
            onSaveTriggerConsumed()
            recordingState = RecordingState.VIEWING_SAVED_AUDIO // After saving, view it as a saved audio
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            println("NoteAudioCard: DisposableEffect onDispose called.")
            // This will now only delete the file if it hasn't been marked as persistent.
            recorderManager.dispose()
            playerManager.stopAudio()
            playerManager.dispose()
        }
    }

    val systemUiController = rememberSystemUiController()
    val originalStatusBarColor = Color.Transparent
    DisposableEffect(systemUiController, cardBackgroundColor) {
        systemUiController.setStatusBarColor(
            color = cardBackgroundColor
        )
        onDispose {
            systemUiController.setStatusBarColor(
                color = originalStatusBarColor
            )
        }
    }
    val bottomPadding =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + toolbarHeight

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cardBackgroundColor)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                )
            )
            .padding(top = 4.dp)
    ) {
        val topPadding = 68.dp

        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .hazeSource(state = hazeState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(topPadding))

            AudioTimerDisplay(
                isRecording = recordingState == RecordingState.RECORDING || recordingState == RecordingState.PAUSED,
                isPlaying = recordingState == RecordingState.PLAYING,
                recordingDurationMillis = recorderManager.recordingDurationMillis,
                currentPlaybackPositionMillis = playerManager.currentPlaybackPositionMillis,
                totalAudioDurationMillis = playerManager.totalAudioDurationMillis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp)
                    .align(Alignment.CenterHorizontally)
            )

            // Waveform/Transcript Display Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (selectedAudioViewType == AudioViewType.Waveform) {
                    WaveformDisplay(
                        audioFilePath = recorderManager.audioFilePath,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = "Audio Transcript coming soon...",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Recording Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .padding(bottom = bottomPadding),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (recordingState) {
                    RecordingState.IDLE -> {
                        IconButton(
                            onClick = {
                                when {
                                    ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED -> {
                                        recorderManager.startRecording()
                                    }
                                    else -> {
                                        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Start recording")
                        }
                    }
                    RecordingState.RECORDING -> {
                        IconButton(
                            onClick = { recorderManager.pauseRecording() }
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause recording")
                        }
                        IconButton(
                            onClick = {
                                recorderManager.stopRecording()
                                playerManager.stopAudio()
                            }
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop recording")
                        }
                    }
                    RecordingState.PAUSED -> {
                        IconButton(
                            onClick = { recorderManager.startRecording() } // Resumes recording
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Resume recording")
                        }
                        IconButton(
                            onClick = {
                                recorderManager.stopRecording()
                                playerManager.stopAudio()
                            }
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop recording")
                        }
                    }
                    RecordingState.STOPPED_UNSAVED -> {
                        IconButton(
                            onClick = {
                                recorderManager.audioFilePath?.let { path ->
                                    playerManager.playAudio(path)
                                }
                            }
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play recording")
                        }
                        IconButton(
                            onClick = {
                                // This delete will work because the audio is not yet marked as persistent
                                recorderManager.deleteRecording() 
                                playerManager.stopAudio()
                                recordingState = RecordingState.IDLE
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Redo recording")
                        }
                        IconButton(
                            onClick = {
                                // This delete will work because the audio is not yet marked as persistent
                                recorderManager.deleteRecording() 
                                onDismiss()
                            }
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Discard recording")
                        }
                    }
                    RecordingState.PLAYING -> {
                        IconButton(
                            onClick = { playerManager.stopAudio() }
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop playback")
                        }
                    }
                    RecordingState.VIEWING_SAVED_AUDIO -> {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = {
                                    recorderManager.audioFilePath?.let { path ->
                                        playerManager.playAudio(path)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play saved recording")
                            }
                            IconButton(
                                onClick = { playerManager.stopAudio() }
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop playback")
                            }
                            Spacer(modifier = Modifier.width(16.dp)) 
                            IconButton(
                                onClick = {
                                    // User wants to start a new recording, so we delete the current (persistent) one
                                    // if it's not being actively viewed in another part of the app.
                                    // For simplicity here, we'll reset state, which will delete it IF not persistent.
                                    // However, in a real app, you might want to confirm deletion of a persistent file.
                                    recorderManager.resetState() 
                                    playerManager.stopAudio()
                                    recordingState = RecordingState.IDLE 
                                }
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Start new recording")
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(100f))
                .background(colorScheme.surfaceDim)
                .hazeEffect(
                    state = hazeState,
                    style = HazeMaterials.ultraThin(hazeThinColor),
                ), verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss, Modifier.padding(4.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }

            val titleTextStyle = MaterialTheme.typography.titleLarge.merge(
                TextStyle(
                    fontFamily = QuicksandTitleVariable,
                    textAlign = TextAlign.Center,
                    color = colorScheme.onSurface
                ) )
            BasicTextField(
                value = audioTitle,
                onValueChange = { onAudioTitleChange(it) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = titleTextStyle,
                cursorBrush = SolidColor(colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (audioTitle.isEmpty()) {
                            Text(
                                text = "Title",
                                style = titleTextStyle,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        innerTextField()
                    }
                })
            IconButton(
                onClick = { /*TODO*/ }, Modifier.padding(4.dp)
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
            }
        }
    }
}

@Composable
fun AudioTimerDisplay(
    isRecording: Boolean,
    isPlaying: Boolean,
    recordingDurationMillis: Long,
    currentPlaybackPositionMillis: Long,
    totalAudioDurationMillis: Long,
    modifier: Modifier = Modifier,
) {
    val displayTime = when {
        isRecording -> recordingDurationMillis
        isPlaying -> currentPlaybackPositionMillis
        else -> 0L
    }

    val totalDurationDisplay =
        if (totalAudioDurationMillis > 0L && (isPlaying || (!isRecording))) {
            " / ${formatDuration(totalAudioDurationMillis)}"
        } else {
            ""
        }

    Text(
        text = "${formatDuration(displayTime)}$totalDurationDisplay",
        style = MaterialTheme.typography.headlineSmall,
        color = colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

fun formatDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format("%02d:%02d", minutes, seconds)
}

// Placeholder Composable for Waveform Display
@Composable
fun WaveformDisplay(audioFilePath: String?, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (audioFilePath != null) {
            Text(
                text = "Waveform visualization for $audioFilePath\n(requires custom drawing implementation)",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = "Start recording to see the waveform...",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}
