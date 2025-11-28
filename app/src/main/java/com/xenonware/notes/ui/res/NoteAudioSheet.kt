package com.xenonware.notes.ui.res

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.xenon.mylibrary.QuicksandTitleVariable
import com.xenonware.notes.ui.layouts.notes.AudioViewType
import com.xenonware.notes.ui.theme.LocalIsDarkTheme
import com.xenonware.notes.ui.theme.XenonTheme
import com.xenonware.notes.ui.theme.extendedMaterialColorScheme
import com.xenonware.notes.viewmodel.classes.Label
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

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

    fun startRecording() {
        if (currentRecordingState == RecordingState.RECORDING) return
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
            setAudioEncodingBitRate(192000) // High quality
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
        if (currentRecordingState != RecordingState.RECORDING) return
        mediaRecorder?.pause()
        currentRecordingState = RecordingState.PAUSED
    }

    fun stopRecording() {
        if (currentRecordingState == RecordingState.RECORDING || currentRecordingState == RecordingState.PAUSED) {
            try {
                mediaRecorder?.stop()
            } catch (_: Exception) { /* sometimes throws if too short */
            }
            mediaRecorder?.release()
            mediaRecorder = null
            currentRecordingState =
                if (isPersistentAudio) RecordingState.VIEWING_SAVED_AUDIO else RecordingState.STOPPED_UNSAVED
        }
    }

    fun setInitialAudioFilePath(filePath: String) {
        audioFilePath = filePath
        uniqueAudioId = File(filePath).nameWithoutExtension
        isPersistentAudio = true
        currentRecordingState = RecordingState.VIEWING_SAVED_AUDIO
        recordingDurationMillis = 0L
    }

    fun markAudioAsPersistent() {
        isPersistentAudio = true
    }

    fun deleteRecording() {
        if (!isPersistentAudio && audioFilePath != null) {
            File(audioFilePath!!).delete()
        }
        audioFilePath = null
        uniqueAudioId = null
        recordingDurationMillis = 0L
        currentRecordingState = RecordingState.IDLE
        isPersistentAudio = false
    }

    fun resetState() {
        if (currentRecordingState == RecordingState.RECORDING || currentRecordingState == RecordingState.PAUSED) {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (_: Exception) { /* ignore */
                }
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
        deleteRecording()
    }
}

class AudioPlayerManager {

    // These are mutable state holders (required for Compose recomposition)
    var currentRecordingState: RecordingState by mutableStateOf(RecordingState.IDLE)
    var isPlaying: Boolean by mutableStateOf(false)
    var currentPlaybackPositionMillis: Long by mutableLongStateOf(0L)
    var totalAudioDurationMillis: Long by mutableLongStateOf(0L)
    private var currentFilePath: String? = null

    // This MUST be a var – we re-assign it every time we play a new file
    var mediaPlayer: MediaPlayer? = null

    fun playAudio(filePath: String) {
        // If we're already playing the same file → do nothing
        if (isPlaying && currentFilePath == filePath) return

        // If we have a different file or player is dead → create new one
        if (mediaPlayer == null || currentFilePath != filePath) {
            mediaPlayer?.release()
            mediaPlayer = null
            currentPlaybackPositionMillis = 0L

            try {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA).build()
                    )
                    setDataSource(filePath)
                    prepare()
                    this@AudioPlayerManager.totalAudioDurationMillis = duration.toLong()
                    currentFilePath = filePath

                    setOnCompletionListener {
                        this@AudioPlayerManager.isPlaying = false
                        currentPlaybackPositionMillis = 0L
                        currentRecordingState = RecordingState.VIEWING_SAVED_AUDIO
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isPlaying = false
                currentRecordingState = RecordingState.VIEWING_SAVED_AUDIO
                return
            }
        }

        mediaPlayer?.start()
        isPlaying = true
        currentRecordingState = RecordingState.PLAYING
    }

    fun pauseAudio() {
        mediaPlayer?.pause()
        isPlaying = false
    }

    fun resumeAudio() {
        mediaPlayer?.start()
        isPlaying = true
        currentRecordingState = RecordingState.PLAYING
    }

    fun stopAudio() {
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
        isPlaying = false
        currentPlaybackPositionMillis = 0L
        currentRecordingState = RecordingState.VIEWING_SAVED_AUDIO
    }

    fun seekTo(positionMillis: Long) {
        mediaPlayer?.seekTo(positionMillis.toInt())
        currentPlaybackPositionMillis = positionMillis
    }

    fun getAudioDuration(filePath: String): Long {
        var duration = 0L
        val tempPlayer = MediaPlayer()
        try {
            tempPlayer.setDataSource(filePath)
            tempPlayer.prepare()
            duration = tempPlayer.duration.toLong()
        } catch (e: Exception) {
            e.printStackTrace()
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

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalHazeMaterialsApi::class
)
@Composable
fun NoteAudioSheet(
    audioTitle: String,
    onAudioTitleChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String?) -> Unit,
    toolbarHeight: Dp,
    saveTrigger: Boolean,
    onSaveTriggerConsumed: () -> Unit,
    selectedAudioViewType: AudioViewType,
    initialAudioFilePath: String? = null,
    initialTheme: String = "Default",
    onThemeChange: (String) -> Unit,
    allLabels: List<Label>,
    initialSelectedLabelId: String?,
    onLabelSelected: (String?) -> Unit,
    onAddNewLabel: (String) -> Unit,
) {
    val hazeState = remember { HazeState() }
    val isDarkTheme = LocalIsDarkTheme.current

    var selectedTheme by rememberSaveable { mutableStateOf(initialTheme) }
    var colorMenuItemText by remember { mutableStateOf("Color") }
    val scope = rememberCoroutineScope()
    var isFadingOut by remember { mutableStateOf(false) }
    var colorChangeJob by remember { mutableStateOf<Job?>(null) }

    val availableThemes = remember {
        listOf("Default", "Red", "Orange", "Yellow", "Green", "Turquoise", "Blue", "Purple")
    }

    var showMenu by remember { mutableStateOf(false) }
    var isOffline by remember { mutableStateOf(false) }

    var showLabelDialog by remember { mutableStateOf(false) }
    var selectedLabelId by rememberSaveable { mutableStateOf(initialSelectedLabelId) }
    val isLabeled = selectedLabelId != null

    val context = LocalContext.current
    val playerManager = remember { AudioPlayerManager() }
    val recorderManager = remember {
        AudioRecorderManager(context) {
            playerManager.totalAudioDurationMillis = 0L
            playerManager.currentPlaybackPositionMillis = 0L
            playerManager.stopAudio()
        }
    }
    var recordingState by remember { mutableStateOf(RecordingState.IDLE) }

    // Sync state
    LaunchedEffect(recorderManager.currentRecordingState, playerManager.currentRecordingState) {
        recordingState = when {
            playerManager.currentRecordingState == RecordingState.PLAYING -> RecordingState.PLAYING
            recorderManager.currentRecordingState == RecordingState.RECORDING -> RecordingState.RECORDING
            recorderManager.currentRecordingState == RecordingState.PAUSED -> RecordingState.PAUSED
            recorderManager.currentRecordingState == RecordingState.STOPPED_UNSAVED -> RecordingState.STOPPED_UNSAVED
            recorderManager.currentRecordingState == RecordingState.VIEWING_SAVED_AUDIO -> RecordingState.VIEWING_SAVED_AUDIO
            else -> RecordingState.IDLE
        }
    }

    // Load existing audio
    LaunchedEffect(initialAudioFilePath) {
        if (initialAudioFilePath != null && File(initialAudioFilePath).exists()) {
            recorderManager.setInitialAudioFilePath(initialAudioFilePath)
            playerManager.totalAudioDurationMillis =
                playerManager.getAudioDuration(initialAudioFilePath)
        }
    }

    // Timer updates
    LaunchedEffect(recordingState) {
        if (recordingState == RecordingState.RECORDING) {
            while (isActive) {
                delay(1000L)
                recorderManager.recordingDurationMillis += 1000L
            }
        }
    }

// ADD THIS BLOCK (playback timer)
    LaunchedEffect(playerManager.isPlaying) {
        if (playerManager.isPlaying) {
            while (isActive) {
                playerManager.currentPlaybackPositionMillis =
                    playerManager.mediaPlayer?.currentPosition?.toLong() ?: 0L
                delay(50L)
            }
        }
    }

    // Save trigger
    LaunchedEffect(saveTrigger) {
        if (saveTrigger && recorderManager.audioFilePath != null) {
            recorderManager.stopRecording()
            playerManager.stopAudio()
            recorderManager.markAudioAsPersistent()
            onSave(audioTitle, recorderManager.uniqueAudioId ?: "", selectedTheme, selectedLabelId)
            onSaveTriggerConsumed()
        }
    }

    LaunchedEffect(initialAudioFilePath) {
        if (initialAudioFilePath != null && File(initialAudioFilePath).exists()) {
            recorderManager.setInitialAudioFilePath(initialAudioFilePath)
            playerManager.totalAudioDurationMillis =
                playerManager.getAudioDuration(initialAudioFilePath)
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) recorderManager.startRecording()
    }

    DisposableEffect(Unit) {
        onDispose {
            recorderManager.dispose()
            playerManager.dispose()
        }
    }

    @Suppress("DEPRECATION") val systemUiController = rememberSystemUiController()

    XenonTheme(
        darkTheme = isDarkTheme,
        useDefaultTheme = selectedTheme == "Default",
        useRedTheme = selectedTheme == "Red",
        useOrangeTheme = selectedTheme == "Orange",
        useYellowTheme = selectedTheme == "Yellow",
        useGreenTheme = selectedTheme == "Green",
        useTurquoiseTheme = selectedTheme == "Turquoise",
        useBlueTheme = selectedTheme == "Blue",
        usePurpleTheme = selectedTheme == "Purple",
        dynamicColor = selectedTheme == "Default"
    ) {
        val animatedTextColor by animateColorAsState(
            targetValue = if (isFadingOut) colorScheme.onSurface.copy(alpha = 0f) else colorScheme.onSurface,
            animationSpec = tween(durationMillis = 500),
            label = "animatedTextColor"
        )

        DisposableEffect(systemUiController, isDarkTheme) {
            systemUiController.setStatusBarColor(Color.Transparent, darkIcons = !isDarkTheme)
            onDispose { systemUiController.setStatusBarColor(Color.Transparent) }
        }

        if (showLabelDialog) {
            LabelSelectionDialog(
                allLabels = allLabels,
                selectedLabelId = selectedLabelId,
                onLabelSelected = {
                    selectedLabelId = it
                    onLabelSelected(it)
                },
                onAddNewLabel = onAddNewLabel,
                onDismiss = { showLabelDialog = false })
        }

        val hazeThinColor = colorScheme.surfaceDim
        val labelColor = extendedMaterialColorScheme.label

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surfaceContainer)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            Column(
                modifier = Modifier
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Top
                        )
                    )
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .hazeSource(hazeState), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(68.dp))

                AudioTimerDisplay(
                    isRecording = recordingState == RecordingState.RECORDING || recordingState == RecordingState.PAUSED,
                    isPlaying = recordingState == RecordingState.PLAYING,
                    recordingDurationMillis = recorderManager.recordingDurationMillis,
                    currentPlaybackPositionMillis = playerManager.currentPlaybackPositionMillis,
                    totalAudioDurationMillis = playerManager.totalAudioDurationMillis,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedAudioViewType == AudioViewType.Waveform) {
                        WaveformDisplay(recorderManager.audioFilePath, Modifier.fillMaxSize())
                    } else {
                        Text(
                            "Transcript coming soon...",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }

                // Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            bottom = WindowInsets.navigationBars.asPaddingValues()
                                .calculateBottomPadding() + toolbarHeight + 16.dp
                        ),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // Inside the 'Controls' Row composable
                    val isActionActive =
                        (playerManager.isPlaying) || (recordingState == RecordingState.RECORDING)

                    val targetCornerRadius = if (isActionActive) 32.dp else 64.dp

                    val animatedRadius: Dp by animateDpAsState(
                        targetValue = targetCornerRadius,
                        animationSpec = tween(durationMillis = 250),
                        label = "Corner Radius Animation"
                    )
                    when (recordingState) {
                        RecordingState.IDLE -> {
                            FilledIconButton(
                                onClick = {
                                    if (ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        recorderManager.startRecording()
                                    } else {
                                        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                shape = RoundedCornerShape(64.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = colorScheme.primary,
                                    contentColor = colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .height(136.dp)
                                    .weight(1f)
                                    .widthIn(max = 184.dp)
                                    .fillMaxWidth(),
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    "Start recording",
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        RecordingState.RECORDING -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                                FilledIconButton(
                                    onClick = { recorderManager.pauseRecording() },
                                    shape = RoundedCornerShape(animatedRadius),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = colorScheme.primary,
                                        contentColor = colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .height(136.dp)
                                        .weight(1f)
                                        .widthIn(max = 184.dp)
                                        .fillMaxWidth(),
                                ) {
                                    Icon(
                                        Icons.Default.Pause,
                                        "Pause",
                                        modifier = Modifier.size(40.dp)
                                    )
                                }

                                FilledIconButton(
                                    onClick = { recorderManager.stopRecording() },
                                    shape = RoundedCornerShape(64.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = colorScheme.error,
                                        contentColor = colorScheme.onError
                                    ),
                                    modifier = Modifier
                                        .height(136.dp)
                                        .weight(1f)
                                        .widthIn(max = 184.dp)
                                        .fillMaxWidth(),
                                ) {
                                    Icon(
                                        Icons.Default.Stop,
                                        "Stop",
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                        }

                        RecordingState.PAUSED -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                                FilledIconButton(
                                    onClick = { recorderManager.startRecording() },
                                    shape = RoundedCornerShape(animatedRadius),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = colorScheme.primary,
                                        contentColor = colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .height(136.dp)
                                        .weight(1f)
                                        .widthIn(max = 184.dp)
                                        .fillMaxWidth(),
                                ) {
                                    Icon(
                                        Icons.Default.Mic,
                                        "Resume",
                                        modifier = Modifier.size(40.dp)
                                    )
                                }

                                FilledIconButton(
                                    onClick = { recorderManager.stopRecording() },
                                    shape = RoundedCornerShape(64.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = colorScheme.error,
                                        contentColor = colorScheme.onError
                                    ),
                                    modifier = Modifier
                                        .height(136.dp)
                                        .weight(1f)
                                        .widthIn(max = 184.dp)
                                        .fillMaxWidth(),
                                ) {
                                    Icon(
                                        Icons.Default.Stop,
                                        "Stop",
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                        }

                        else -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledIconButton(
                                    onClick = {
                                        recorderManager.audioFilePath?.let { path ->
                                            when {
                                                playerManager.isPlaying -> {
                                                    playerManager.pauseAudio()
                                                }

                                                playerManager.currentPlaybackPositionMillis > 0 && playerManager.mediaPlayer != null -> {
                                                    playerManager.resumeAudio()
                                                }

                                                else -> {
                                                    playerManager.playAudio(path)
                                                }
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(animatedRadius),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = colorScheme.primary,
                                        contentColor = colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .height(136.dp)
                                        .weight(1f)
                                        .widthIn(max = 184.dp)
                                        .fillMaxWidth(),
                                ) {
                                    Icon(
                                        imageVector = if (playerManager.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (playerManager.isPlaying) "Pause" else "Play",
                                        modifier = Modifier.size(40.dp)
                                    )
                                }

                                FilledIconButton(
                                    onClick = {
                                        playerManager.stopAudio()
                                    },
                                    enabled = playerManager.currentPlaybackPositionMillis > 0 || playerManager.isPlaying,
                                    shape = RoundedCornerShape(64.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = colorScheme.primaryContainer,
                                        contentColor = colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier
                                        .height(136.dp)
                                        .weight(1f)
                                        .widthIn(max = 184.dp)
                                        .fillMaxWidth(),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop",
                                        modifier = Modifier.size(40.dp)
                                    )
                                }

                                FilledIconButton(
                                    onClick = {
                                        recorderManager.resetState()
                                        recorderManager.deleteRecording()
                                        playerManager.stopAudio()
                                    },
                                    shape = RoundedCornerShape(64.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = colorScheme.errorContainer,
                                        contentColor = colorScheme.onErrorContainer
                                    ),
                                    modifier = Modifier
                                        .height(136.dp)
                                        .weight(0.5f)
                                        .widthIn(max = 184.dp)
                                        .fillMaxWidth(),
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        "Discard",
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Toolbar
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Top
                        )
                    )
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp)
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
                    onValueChange = onAudioTitleChange,
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
                                    color = colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            innerTextField()
                        }
                    })
                Box {
                    IconButton(
                        onClick = { showMenu = !showMenu }, modifier = Modifier.padding(4.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownNoteMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        items = listOfNotNull(
                            MenuItem(text = "Label", onClick = {
                                showLabelDialog = true
                                showMenu = false
                            }, dismissOnClick = true, icon = {
                                if (isLabeled) {
                                    Icon(
                                        Icons.Default.Bookmark,
                                        contentDescription = "Label",
                                        tint = labelColor
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.BookmarkBorder, contentDescription = "Label"
                                    )
                                }
                            }), MenuItem(
                                text = colorMenuItemText, onClick = {
                                    val currentIndex = availableThemes.indexOf(selectedTheme)
                                    val nextIndex = (currentIndex + 1) % availableThemes.size
                                    selectedTheme = availableThemes[nextIndex]
                                    onThemeChange(selectedTheme) // Call the callback here
                                    colorChangeJob?.cancel()
                                    colorChangeJob = scope.launch {
                                        colorMenuItemText = availableThemes[nextIndex]
                                        isFadingOut = false
                                        delay(2500)
                                        isFadingOut = true
                                        delay(500)
                                        colorMenuItemText = "Color"
                                        isFadingOut = false
                                    }
                                }, dismissOnClick = false, icon = {
                                    Icon(
                                        Icons.Default.ColorLens,
                                        contentDescription = "Color",
                                        tint = if (selectedTheme == "Default") colorScheme.onSurfaceVariant else colorScheme.primary
                                    )
                                }, textColor = animatedTextColor
                            ), MenuItem(
                                text = if (isOffline) "Offline note" else "Online note",
                                onClick = { isOffline = !isOffline },
                                dismissOnClick = false,
                                textColor = if (isOffline) colorScheme.error else null,
                                icon = {
                                    if (isOffline) {
                                        Icon(
                                            Icons.Default.CloudOff,
                                            contentDescription = "Offline note",
                                            tint = colorScheme.error
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Cloud, contentDescription = "Online note"
                                        )
                                    }
                                })
                        ),
                        hazeState = hazeState
                    )
                }
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
    val currentTime = if (isRecording) recordingDurationMillis else currentPlaybackPositionMillis
    val showTotal = !isRecording && totalAudioDurationMillis > 0

    Text(
        text = formatDuration(currentTime) + if (showTotal) " / ${
            formatDuration(
                totalAudioDurationMillis
            )
        }" else "",
        style = MaterialTheme.typography.headlineSmall,
        color = colorScheme.onSurface,
        modifier = modifier
    )
}

@SuppressLint("DefaultLocale")
fun formatDuration(millis: Long): String {
    val m = TimeUnit.MILLISECONDS.toMinutes(millis)
    val s = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format("%02d:%02d", m, s)
}

@Composable
fun WaveformDisplay(audioFilePath: String?, modifier: Modifier = Modifier) {
    Box(modifier, Alignment.Center) {
        Text(
            text = if (audioFilePath != null) "Waveform (coming soon)" else "Start recording...",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}