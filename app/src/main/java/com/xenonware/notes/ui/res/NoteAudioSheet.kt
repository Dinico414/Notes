@file:Suppress("AssignedValueIsNeverRead")

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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
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

    fun getMaxAmplitude(): Int = mediaRecorder?.maxAmplitude ?: 0

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
        if (currentRecordingState != RecordingState.RECORDING) return
        mediaRecorder?.pause()
        currentRecordingState = RecordingState.PAUSED
    }

    fun stopRecording() {
        if (currentRecordingState == RecordingState.RECORDING || currentRecordingState == RecordingState.PAUSED) {
            try { mediaRecorder?.stop() } catch (_: Exception) {}
            mediaRecorder?.release()
            mediaRecorder = null
            currentRecordingState = if (isPersistentAudio) RecordingState.VIEWING_SAVED_AUDIO else RecordingState.STOPPED_UNSAVED
        }
    }

    fun setInitialAudioFilePath(filePath: String) {
        audioFilePath = filePath
        uniqueAudioId = File(filePath).nameWithoutExtension
        isPersistentAudio = true
        currentRecordingState = RecordingState.VIEWING_SAVED_AUDIO
        recordingDurationMillis = 0L
    }

    fun markAudioAsPersistent() { isPersistentAudio = true }

    fun deleteRecording() {
        if (!isPersistentAudio && audioFilePath != null) File(audioFilePath!!).delete()
        audioFilePath = null
        uniqueAudioId = null
        recordingDurationMillis = 0L
        currentRecordingState = RecordingState.IDLE
        isPersistentAudio = false
    }

    fun resetState() {
        if (currentRecordingState == RecordingState.RECORDING || currentRecordingState == RecordingState.PAUSED) {
            mediaRecorder?.apply { try { stop() } catch (_: Exception) {}; release() }
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

object GlobalAudioPlayer {
    private var _instance: AudioPlayerManager? = null
    private val lock = Any()

    fun getInstance(): AudioPlayerManager = synchronized(lock) {
        _instance ?: AudioPlayerManager().also { _instance = it }
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
    var currentFilePath: String? = null; private set(value) {
        field = value
        if (value != null && value != field) currentPlaybackPositionMillis = 0L
    }
    var currentRecordingState by mutableStateOf(RecordingState.VIEWING_SAVED_AUDIO); private set

    fun playAudio(filePath: String) {
        if (currentFilePath != filePath || mediaPlayer == null) stopAudio()
        if (currentFilePath == filePath && isPlaying) return

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

    fun pauseAudio() { mediaPlayer?.pause(); isPlaying = false }
    fun resumeAudio() {
        if (mediaPlayer == null) {
            currentFilePath?.let { playAudio(it) }
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
        p.duration.toLong().also { p.release() }
    } catch (_: Exception) { 0L }

    fun dispose() = stopAudio()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalHazeMaterialsApi::class)
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
    onHasUnsavedAudioChange: (Boolean) -> Unit = {},
) {
    val hazeState = remember { HazeState() }
    val isDarkTheme = LocalIsDarkTheme.current
    val context = LocalContext.current
    val player = GlobalAudioPlayer.getInstance()
    val amplitudes = remember { mutableStateListOf<Float>() }
    val recorder = remember {
        AudioRecorderManager(context) {
            player.totalAudioDurationMillis = 0L
            player.currentPlaybackPositionMillis = 0L
            player.stopAudio()
            amplitudes.clear()
        }
    }

    val currentSheetAudioPath = recorder.audioFilePath ?: initialAudioFilePath
    val isSheetAudioActive = player.currentFilePath == currentSheetAudioPath
    val isSheetAudioPlaying = player.isPlaying && isSheetAudioActive
    val isSheetAudioPaused = !player.isPlaying && isSheetAudioActive && player.currentPlaybackPositionMillis > 0

    var selectedTheme by rememberSaveable { mutableStateOf(initialTheme) }
    var colorMenuItemText by remember { mutableStateOf("Color") }
    val scope = rememberCoroutineScope()
    var isFadingOut by remember { mutableStateOf(false) }
    var colorChangeJob by remember { mutableStateOf<Job?>(null) }
    val availableThemes = remember { listOf("Default", "Red", "Orange", "Yellow", "Green", "Turquoise", "Blue", "Purple") }

    var showMenu by remember { mutableStateOf(false) }
    var isOffline by remember { mutableStateOf(false) }
    var showLabelDialog by remember { mutableStateOf(false) }
    var selectedLabelId by rememberSaveable { mutableStateOf(initialSelectedLabelId) }
    val isLabeled = selectedLabelId != null

    var recordingState by remember { mutableStateOf(RecordingState.IDLE) }


    val hasAudioContent = initialAudioFilePath != null || recorder.audioFilePath != null

    LaunchedEffect(hasAudioContent) {
        onHasUnsavedAudioChange(hasAudioContent)
    }

    // Sync state
    LaunchedEffect(recorder.currentRecordingState, player.currentRecordingState) {
        recordingState = when {
            player.currentRecordingState == RecordingState.PLAYING -> RecordingState.PLAYING
            recorder.currentRecordingState == RecordingState.RECORDING -> RecordingState.RECORDING
            recorder.currentRecordingState == RecordingState.PAUSED -> RecordingState.PAUSED
            recorder.currentRecordingState == RecordingState.STOPPED_UNSAVED -> RecordingState.STOPPED_UNSAVED
            recorder.currentRecordingState == RecordingState.VIEWING_SAVED_AUDIO -> RecordingState.VIEWING_SAVED_AUDIO
            else -> RecordingState.IDLE
        }
    }

    LaunchedEffect(initialAudioFilePath) {
        if (initialAudioFilePath != null && File(initialAudioFilePath).exists()) {
            recorder.setInitialAudioFilePath(initialAudioFilePath)
        }
    }

    // Recording timer
    LaunchedEffect(recordingState) {
        if (recordingState == RecordingState.RECORDING) {
            while (isActive) {
                delay(1000L)
                recorder.recordingDurationMillis += 1000L
            }
        }
    }

    // Amplitude updater
    LaunchedEffect(recordingState) {
        if (recordingState == RecordingState.RECORDING) {
            while (isActive) {
                amplitudes.add(recorder.getMaxAmplitude().toFloat())
                delay(100L)
            }
        }
    }

    // Playback position updater
    LaunchedEffect(player.isPlaying) {
        if (player.isPlaying) {
            while (isActive) {
                player.currentPlaybackPositionMillis = player.mediaPlayer?.currentPosition?.toLong() ?: 0L
                delay(50L)
            }
        }
    }

    // Save trigger
    LaunchedEffect(saveTrigger) {
        if (saveTrigger) {
            val hasAudio = recorder.audioFilePath != null || initialAudioFilePath != null
            if (audioTitle.isNotBlank() && hasAudio) {
                recorder.stopRecording() // safely stops if recording
                player.stopAudio()
                recorder.markAudioAsPersistent()
                val audioId = recorder.uniqueAudioId ?:
                initialAudioFilePath?.let { File(it).nameWithoutExtension } ?: ""
                onSave(audioTitle, audioId, selectedTheme, selectedLabelId)
            }
            onSaveTriggerConsumed()
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) recorder.startRecording()
    }

    DisposableEffect(Unit) {
        onDispose { recorder.dispose() }
    }

    @Suppress("DEPRECATION") val systemUiController = rememberSystemUiController()

    // === DURATION & VISIBILITY LOGIC ===
    val hasAudioFile = currentSheetAudioPath != null
    val isInRecordingMode = recordingState == RecordingState.RECORDING || recordingState == RecordingState.PAUSED

    // Recompute duration only when entering playback mode
    val sheetAudioDuration by remember(currentSheetAudioPath, recordingState) {
        mutableLongStateOf(
            if (hasAudioFile && !isInRecordingMode && currentSheetAudioPath != null) {
                player.getAudioDuration(currentSheetAudioPath!!)
            } else 0L
        )
    }

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
            targetValue = if (isFadingOut) MaterialTheme.colorScheme.onSurface.copy(alpha = 0f) else MaterialTheme.colorScheme.onSurface,
            animationSpec = tween(500)
        )

        DisposableEffect(systemUiController, isDarkTheme) {
            systemUiController.setStatusBarColor(Color.Transparent, darkIcons = !isDarkTheme)
            onDispose { systemUiController.setStatusBarColor(Color.Transparent) }
        }

        if (showLabelDialog) {
            LabelSelectionDialog(
                allLabels = allLabels,
                selectedLabelId = selectedLabelId,
                onLabelSelected = { selectedLabelId = it; onLabelSelected(it) },
                onAddNewLabel = onAddNewLabel,
                onDismiss = { showLabelDialog = false }
            )
        }

        val hazeThinColor = MaterialTheme.colorScheme.surfaceDim
        val labelColor = extendedMaterialColorScheme.label

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        ) {
            Column(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .hazeSource(hazeState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(68.dp))

                // Top Timer
                AudioTimerDisplay(
                    isRecording = recordingState == RecordingState.RECORDING || recordingState == RecordingState.PAUSED,
                    isPlaying = isSheetAudioPlaying,
                    recordingDurationMillis = recorder.recordingDurationMillis,
                    currentPlaybackPositionMillis = player.currentPlaybackPositionMillis,
                    totalAudioDurationMillis = if (hasAudioFile && !isInRecordingMode) sheetAudioDuration else 0L,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                // Waveform / Transcript
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedAudioViewType == AudioViewType.Waveform) {
                        WaveformDisplay(
                            modifier = Modifier.fillMaxSize(),
                            amplitudes = amplitudes,
                            isRecording = isInRecordingMode,
                            audioFilePath = currentSheetAudioPath
                        )
                    } else {
                        Text("Transcript coming soon...", style = MaterialTheme.typography.headlineMedium)
                    }
                }

                // PROGRESS BAR — Only when we have audio and not recording
                if (hasAudioFile && !isInRecordingMode && sheetAudioDuration > 0L) {
                    Spacer(Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .pointerInput(isSheetAudioActive) {
                                if (isSheetAudioActive && sheetAudioDuration > 0L) {
                                    detectTapGestures { offset ->
                                        val progress = (offset.x / size.width).coerceIn(0f, 1f)
                                        player.seekTo((sheetAudioDuration * progress).toLong())
                                    }
                                }
                            }
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            LinearProgressIndicator(
                                progress = {
                                    if (isSheetAudioActive && sheetAudioDuration > 0L)
                                        (player.currentPlaybackPositionMillis.toFloat() / sheetAudioDuration.coerceAtLeast(1L)).coerceIn(0f, 1f)
                                    else 0f
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                strokeCap = StrokeCap.Round
                            )

                            Spacer(Modifier.height(8.dp))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatDuration(player.currentPlaybackPositionMillis),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatDuration(sheetAudioDuration),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // CONTROLS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = WindowInsets.navigationBars
                            .asPaddingValues()
                            .calculateBottomPadding() + toolbarHeight + 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isActionActive = isSheetAudioPlaying || recordingState == RecordingState.RECORDING
                    val animatedRadius by animateDpAsState(if (isActionActive) 32.dp else 64.dp, tween(250))

                    when (recordingState) {
                        RecordingState.IDLE -> {
                            FilledIconButton(
                                onClick = {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        recorder.startRecording()
                                    } else {
                                        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                shape = RoundedCornerShape(64.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .height(136.dp)
                                    .weight(1f)
                                    .widthIn(max = 184.dp)
                            ) {
                                Icon(Icons.Rounded.Mic, "Start recording", Modifier.size(40.dp))
                            }
                        }

                        RecordingState.RECORDING -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledIconButton(
                                    onClick = { recorder.pauseRecording() },
                                    shape = RoundedCornerShape(animatedRadius),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .height(136.dp)
                                        .weight(1f)
                                        .widthIn(max = 184.dp)
                                ) { Icon(Icons.Rounded.Pause, "Pause", Modifier.size(40.dp)) }

                                FilledIconButton(
                                    onClick = { recorder.stopRecording() },
                                    shape = RoundedCornerShape(64.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ),
                                    modifier = Modifier
                                        .height(136.dp)
                                        .weight(1f)
                                        .widthIn(max = 184.dp)
                                ) { Icon(Icons.Rounded.Stop, "Stop", Modifier.size(40.dp)) }
                            }
                        }

                        RecordingState.PAUSED -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledIconButton(
                                    onClick = { recorder.startRecording() },
                                    shape = RoundedCornerShape(animatedRadius),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .height(136.dp)
                                        .weight(1f)
                                        .widthIn(max = 184.dp)
                                ) { Icon(Icons.Rounded.Mic, "Resume", Modifier.size(40.dp)) }

                                FilledIconButton(
                                    onClick = { recorder.stopRecording() },
                                    shape = RoundedCornerShape(64.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ),
                                    modifier = Modifier
                                        .height(136.dp)
                                        .weight(1f)
                                        .widthIn(max = 184.dp)
                                ) { Icon(Icons.Rounded.Stop, "Stop", Modifier.size(40.dp)) }
                            }
                        }

                        else -> { // Playback mode
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledIconButton(
                                    onClick = {
                                        currentSheetAudioPath?.let { path ->
                                            when {
                                                isSheetAudioPlaying -> player.pauseAudio()
                                                isSheetAudioPaused -> player.resumeAudio()
                                                else -> player.playAudio(path)
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(animatedRadius),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .height(136.dp)
                                        .weight(1f)
                                        .widthIn(max = 184.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSheetAudioPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (isSheetAudioPlaying) "Pause" else "Play",
                                        modifier = Modifier.size(40.dp)
                                    )
                                }

                                FilledIconButton(
                                    onClick = { player.stopAudio() },
                                    enabled = player.currentPlaybackPositionMillis > 0 || player.isPlaying,
                                    shape = RoundedCornerShape(64.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier
                                        .height(136.dp)
                                        .weight(1f)
                                        .widthIn(max = 184.dp)
                                ) { Icon(Icons.Rounded.Stop, "Stop", Modifier.size(40.dp)) }

                                if (!recorder.isPersistentAudio && currentSheetAudioPath != null) {
                                    FilledIconButton(
                                        onClick = {
                                            recorder.resetState()
                                            recorder.deleteRecording()
                                            player.stopAudio()
                                        },
                                        shape = RoundedCornerShape(64.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        ),
                                        modifier = Modifier
                                            .height(136.dp)
                                            .weight(0.5f)
                                            .widthIn(max = 184.dp)
                                    ) {
                                        Icon(Icons.Rounded.Clear, "Discard", Modifier.size(40.dp))
                                    }
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
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(100f))
                    .background(MaterialTheme.colorScheme.surfaceDim)
                    .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin(hazeThinColor)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, Modifier.padding(4.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }

                val titleTextStyle = MaterialTheme.typography.titleLarge.merge(
                    TextStyle(fontFamily = QuicksandTitleVariable, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
                )

                BasicTextField(
                    value = audioTitle,
                    onValueChange = onAudioTitleChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = titleTextStyle,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if (audioTitle.isEmpty()) {
                                Text("Title", style = titleTextStyle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            innerTextField()
                        }
                    }
                )

                Box {
                    IconButton(onClick = { showMenu = !showMenu }, modifier = Modifier.padding(4.dp)) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More options")
                    }
                    DropdownNoteMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        items = listOfNotNull(
                            MenuItem(text = "Label", onClick = { showLabelDialog = true; showMenu = false }, dismissOnClick = true, icon = {
                                if (isLabeled) Icon(Icons.Rounded.Bookmark, "Label", tint = labelColor)
                                else Icon(Icons.Rounded.BookmarkBorder, "Label")
                            }),
                            MenuItem(text = colorMenuItemText, onClick = {
                                val nextIndex = (availableThemes.indexOf(selectedTheme) + 1) % availableThemes.size
                                selectedTheme = availableThemes[nextIndex]
                                onThemeChange(selectedTheme)
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
                                Icon(Icons.Rounded.ColorLens, "Color", tint = if (selectedTheme == "Default") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
                            }, textColor = animatedTextColor),
                            MenuItem(text = if (isOffline) "Offline note" else "Online note", onClick = { isOffline = !isOffline }, dismissOnClick = false,
                                textColor = if (isOffline) MaterialTheme.colorScheme.error else null, icon = {
                                    if (isOffline) Icon(Icons.Rounded.CloudOff, "Offline note", tint = MaterialTheme.colorScheme.error)
                                    else Icon(Icons.Rounded.Cloud, "Online note")
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
    val time = if (isRecording) recordingDurationMillis else currentPlaybackPositionMillis
    val showTotal = totalAudioDurationMillis > 0L

    Text(
        text = formatDuration(time) + if (showTotal) " / ${formatDuration(totalAudioDurationMillis)}" else "",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
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
fun WaveformDisplay(
    modifier: Modifier = Modifier,
    amplitudes: List<Float>,
    isRecording: Boolean,
    audioFilePath: String?
) {
    val color = MaterialTheme.colorScheme.primary

    if (amplitudes.isNotEmpty() && isRecording) {
        val maxAmplitudeValue = 32767f // Max amplitude from MediaRecorder

        Canvas(modifier = modifier) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val middleY = canvasHeight / 2

            val barWidth = 3.dp.toPx()
            val spacing = 2.dp.toPx()
            val totalBarWidth = barWidth + spacing

            val maxBars = (canvasWidth / totalBarWidth).toInt()
            val startIndex = (amplitudes.size - maxBars).coerceAtLeast(0)
            val barsToDraw = amplitudes.subList(startIndex, amplitudes.size)

            barsToDraw.forEachIndexed { index, amplitude ->
                val x = index * totalBarWidth
                val normalized = (amplitude / maxAmplitudeValue).coerceIn(0f, 1f)
                val barHeight = normalized * canvasHeight * 0.8f // Use 80% of height

                if (barHeight > 0) {
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x = x, y = middleY - barHeight / 2),
                        size = Size(barWidth, barHeight.coerceAtLeast(2.dp.toPx())), // min height
                        cornerRadius = CornerRadius(barWidth / 2)
                    )
                }
            }
        }
    } else {
        Box(modifier, Alignment.Center) {
            Text(
                text = if (audioFilePath != null) "Waveform (coming soon)" else "Start recording...",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}