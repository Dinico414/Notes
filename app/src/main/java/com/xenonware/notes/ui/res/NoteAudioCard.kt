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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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


// Placeholder for AudioRecorderManager
// This would ideally be a separate class responsible for handling MediaRecorder
// and managing the audio file.
class AudioRecorderManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    var audioFilePath: String? = null
        private set

    var isRecording: Boolean by mutableStateOf(false)
        private set
    var isPaused: Boolean by mutableStateOf(false)
        private set
    var hasRecording: Boolean by mutableStateOf(false)
        private set
    var recordingDurationMillis: Long by mutableLongStateOf(0L) // Added for recording duration


    fun startRecording() {
        if (isRecording) return
        if (isPaused) {
            mediaRecorder?.resume()
            isPaused = false
            isRecording = true
            // TODO: Resume the recording duration update
            println("Resumed recording")
            return
        }
        startNewRecording()
    }

    private fun startNewRecording() {
        val audioFile = File(context.cacheDir, "audio_note_${System.currentTimeMillis()}.mp3")
        audioFilePath = audioFile.absolutePath

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
                isRecording = true
                isPaused = false
                hasRecording = true
                recordingDurationMillis = 0L // Reset on new recording
                // TODO: Start a coroutine or Handler here to update recordingDurationMillis every second
                println("Started recording to: $audioFilePath")
            } catch (e: IOException) {
                println("Failed to start recording: ${e.message}")
                isRecording = false
                isPaused = false
                hasRecording = false
            }
        }
    }

    fun pauseRecording() {
        if (!isRecording) return
        mediaRecorder?.pause()
        isPaused = true
        isRecording = false
        // TODO: Pause the recording duration update
        println("Paused recording")
    }


    fun stopRecording() {
        if (isRecording || isPaused) {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            isPaused = false
            // TODO: Stop and reset the recording duration update
            recordingDurationMillis = 0L
            println("Stopped recording")
        }
    }


    fun dispose() {
        mediaRecorder?.release()
        mediaRecorder = null
        // TODO: Cancel any active coroutine scopes for recording duration
    }
}


// Placeholder for AudioPlayerManager
// This would ideally be a separate class responsible for handling MediaPlayer
// and playing back the audio file.
class AudioPlayerManager {
    var isPlaying: Boolean by mutableStateOf(false)

    // private set removed
    var currentPlaybackPositionMillis: Long by mutableLongStateOf(0L) // Added for playback position

    // private set removed
    var totalAudioDurationMillis: Long by mutableLongStateOf(0L) // Added for total duration
    // private set removed

    private var mediaPlayer: MediaPlayer? = null
    // TODO: Add a CoroutineScope to manage playback timer updates

    fun playAudio(filePath: String) {
        // TODO: Implement actual audio playback using MediaPlayer
        // Initialize MediaPlayer, setDataSource, prepare, and start.
        // Get total duration from mediaPlayer.duration
        // Start a coroutine to update currentPlaybackPositionMillis periodically.
        try {
            mediaPlayer?.release() // Release previous player if any
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                start()
                this@AudioPlayerManager.totalAudioDurationMillis = duration.toLong()
                this@AudioPlayerManager.isPlaying = true
                // TODO: Start a coroutine here to update currentPlaybackPositionMillis every 100ms
                setOnCompletionListener {
                    stopAudio() // Stop when playback completes
                    this@AudioPlayerManager.currentPlaybackPositionMillis =
                        0L // Reset position on completion
                }
            }
            println("Playing audio from: $filePath")
        } catch (e: IOException) {
            println("Failed to play audio: ${e.message}")
            this@AudioPlayerManager.isPlaying = false
        }
    }

    fun stopAudio() {
        // TODO: Implement actual audio stopping
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
        isPlaying = false
        currentPlaybackPositionMillis = 0L
        // TODO: Stop and reset the playback timer coroutine
        println("Stopping audio playback")
    }

    fun dispose() {
        mediaPlayer?.release()
        mediaPlayer = null
        // TODO: Cancel the coroutine scope for playback
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
    onSave: (String, String) -> Unit, // (title, description)
    cardBackgroundColor: Color = colorScheme.surfaceContainer,
    toolbarHeight: Dp,
    saveTrigger: Boolean,
    onSaveTriggerConsumed: () -> Unit,
    selectedAudioViewType: AudioViewType, // Current selected view type from parent
) {
    val hazeState = remember { HazeState() }
    val hazeThinColor = colorScheme.surfaceDim
    val context = LocalContext.current // Get the current context

    // Initialize AudioRecorderManager and AudioPlayerManager
    val recorderManager = remember { AudioRecorderManager(context) }
    val playerManager = remember { AudioPlayerManager() }

    // Use state from the manager directly or derive from it
    val isRecording = recorderManager.isRecording
    val isPaused = recorderManager.isPaused
    val hasRecording = recorderManager.hasRecording
    var isStopped by remember { mutableStateOf(false) } // Still need this for player control

    // Simulation for recording timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recorderManager.recordingDurationMillis = 0L // Reset on start
            while (isActive) {
                delay(1000L)
                recorderManager.recordingDurationMillis += 1000L
            }
        } else {
            if (!isPaused) recorderManager.recordingDurationMillis = 0L
        }
    }

    // Simulation for playback timer
    LaunchedEffect(playerManager.isPlaying) {
        if (playerManager.isPlaying) {
            while (isActive && playerManager.isPlaying) {
                // In a real scenario, you'd get this from MediaPlayer.currentPosition
                playerManager.currentPlaybackPositionMillis =
                    (playerManager.currentPlaybackPositionMillis + 1000L).coerceAtMost(playerManager.totalAudioDurationMillis)
                delay(1000L)
            }
        } else {
            playerManager.currentPlaybackPositionMillis = 0L
        }
    }


    // Permission launcher
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, start recording
            recorderManager.startRecording()
            isStopped = false
        } else {
            // Permission denied, handle accordingly (e.e., show a toast)
            println("Microphone permission denied")
        }
    }

    // Handle saving: stop recording/playback and save the audio file path
    LaunchedEffect(saveTrigger) {
        if (saveTrigger) {
            recorderManager.stopRecording()
            playerManager.stopAudio()
            onSave(audioTitle, recorderManager.audioFilePath ?: "") // Pass the audio file path
            onSaveTriggerConsumed()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recorderManager.dispose() // Release recorder resources
            playerManager.stopAudio() // Stop playback if any
            playerManager.dispose() // Dispose player resources
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

            // New Timer Display at the top
            AudioTimerDisplay(
                isRecording = isRecording,
                isPlaying = playerManager.isPlaying,
                recordingDurationMillis = recorderManager.recordingDurationMillis,
                currentPlaybackPositionMillis = playerManager.currentPlaybackPositionMillis,
                totalAudioDurationMillis = playerManager.totalAudioDurationMillis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp) // Adjusted padding
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
                    // Placeholder for Waveform visualization
                    WaveformDisplay(
                        audioFilePath = recorderManager.audioFilePath,
                        modifier = Modifier.fillMaxSize()
                    )
                } else { // Implicitly AudioViewType.Transcript
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
                // Start/Play Button
                IconButton(
                    onClick = {
                        if (isStopped && hasRecording) {
                            // Play recording
                            recorderManager.audioFilePath?.let { path ->
                                playerManager.playAudio(path)
                                isStopped = false // Playing, not stopped
                            }
                        } else if (!isRecording && !isPaused) {
                            // Check for permission before starting recording
                            when {
                                ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED -> {
                                    // Permission already granted, start recording
                                    recorderManager.startRecording()
                                    isStopped = false
                                }

                                else -> {
                                    // Request permission
                                    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        } else if (isPaused) {
                            // Resume recording
                            recorderManager.startRecording() // startRecording also handles resume
                            isStopped = false
                        }
                    }, enabled = !isRecording || isPaused || (isStopped && hasRecording)
                ) {
                    if (isStopped && hasRecording) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play recording")
                    } else if (isRecording) {
                        Icon(Icons.Default.Mic, contentDescription = "Recording in progress")
                    } else {
                        Icon(Icons.Default.Mic, contentDescription = "Start recording")
                    }
                }

                // Pause Button
                IconButton(
                    onClick = {
                        if (isRecording) {
                            recorderManager.pauseRecording()
                        }
                    }, enabled = isRecording
                ) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause recording")
                }

                // Stop Button
                IconButton(
                    onClick = {
                        if (isRecording || isPaused || playerManager.isPlaying) {
                            recorderManager.stopRecording()
                            playerManager.stopAudio()
                            isStopped = true
                            // If we stop during recording, we still have a recording.
                            // If we stop during playback, we still have a recording.
                        }
                    }, enabled = isRecording || isPaused || playerManager.isPlaying || hasRecording
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop recording")
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
                )
            )
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
            // TODO: Implement actual waveform drawing here.
            // This would typically involve:
            // 1. Reading audio data (e.g., from the audioFilePath).
            // 2. Processing audio data to extract amplitude values.
            // 3. Using Canvas or custom Composables to draw the waveform.
        } else {
            Text(
                text = "Start recording to see the waveform...",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}