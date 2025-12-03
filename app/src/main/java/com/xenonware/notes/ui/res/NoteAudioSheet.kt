@file:Suppress("AssignedValueIsNeverRead", "unused")

package com.xenonware.notes.ui.res

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.xenonware.notes.util.AudioRecorderManager
import com.xenonware.notes.util.GlobalAudioPlayer
import com.xenonware.notes.util.RecordingState
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
import java.util.concurrent.TimeUnit


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
    onHasUnsavedAudioChange: (Boolean) -> Unit = {},
    isCoverModeActive: Boolean = false,
) {
    val isLandscape =
        LocalConfiguration.current.screenWidthDp > LocalConfiguration.current.screenHeightDp
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
    val isSheetAudioPaused =
        !player.isPlaying && isSheetAudioActive && player.currentPlaybackPositionMillis > 0

    var selectedTheme by rememberSaveable { mutableStateOf(initialTheme) }
    var colorMenuItemText by remember { mutableStateOf("Color") }
    val scope = rememberCoroutineScope()
    var isFadingOut by remember { mutableStateOf(false) }
    var colorChangeJob by remember { mutableStateOf<Job?>(null) }
    val availableThemes = remember {
        listOf(
            "Default", "Red", "Orange", "Yellow", "Green", "Turquoise", "Blue", "Purple"
        )
    }

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
            val audioId = File(initialAudioFilePath).nameWithoutExtension
            val loadedAmplitudes = loadAmplitudes(context, audioId)
            if (loadedAmplitudes.isNotEmpty()) {
                amplitudes.addAll(loadedAmplitudes)
            }
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
                player.currentPlaybackPositionMillis =
                    player.mediaPlayer?.currentPosition?.toLong() ?: 0L
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
                val audioId = recorder.uniqueAudioId
                    ?: initialAudioFilePath?.let { File(it).nameWithoutExtension } ?: ""

                if (amplitudes.isNotEmpty()) {
                    saveAmplitudes(context, audioId, amplitudes)
                }

                onSave(audioTitle, audioId, selectedTheme, selectedLabelId)
            }
            onSaveTriggerConsumed()
        }
    }

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) recorder.startRecording()
        }

    DisposableEffect(Unit) {
        onDispose { recorder.dispose() }
    }

    @Suppress("DEPRECATION") val systemUiController = rememberSystemUiController()

    // === DURATION & VISIBILITY LOGIC ===
    val hasAudioFile = currentSheetAudioPath != null
    val isInRecordingMode =
        recordingState == RecordingState.RECORDING || recordingState == RecordingState.PAUSED

    // Recompute duration only when entering playback mode
    @Suppress("SENSELESS_COMPARISON") val sheetAudioDuration by remember(
        currentSheetAudioPath, recordingState
    ) {
        mutableLongStateOf(
            if (hasAudioFile && !isInRecordingMode && currentSheetAudioPath != null) {
                player.getAudioDuration(currentSheetAudioPath)
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
                onDismiss = { showLabelDialog = false })
        }

        val hazeThinColor = MaterialTheme.colorScheme.surfaceDim
        val labelColor = extendedMaterialColorScheme.label
        val backgroundColor =
            if (isCoverModeActive) Color.Black else MaterialTheme.colorScheme.surfaceContainer

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        ) {
            // ────── MAIN CONTENT (rounded top corners + haze) ──────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 4.dp) // small gap from status bar
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .hazeSource(hazeState)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))
                    .padding(horizontal = 20.dp)
                    .padding(bottom = toolbarHeight + 16.dp) // space for bottom toolbar
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(68.dp))
                    val density = LocalDensity.current
                    val positionTextHeight =
                        with(density) { MaterialTheme.typography.labelMedium.fontSize.toDp() }
                    val totalHeight = 60.dp + positionTextHeight

                    if (isLandscape) {
                        // ────── LANDSCAPE: 2×2 grid exactly as you asked ──────
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Top row – 30% of height
                            Row(
                                modifier = Modifier
                                    .weight(0.3f)
                                    .fillMaxWidth()
                            ) {
                                // Start Top – Timer centered
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AudioTimerDisplay(
                                        isRecording = isInRecordingMode,
                                        isPlaying = isSheetAudioPlaying,
                                        recordingDurationMillis = recorder.recordingDurationMillis,
                                        currentPlaybackPositionMillis = player.currentPlaybackPositionMillis,
                                        totalAudioDurationMillis = if (hasAudioFile && !isInRecordingMode) sheetAudioDuration else 0L,
                                        modifier = Modifier.padding(vertical = 16.dp)
                                    )
                                }

                                // End Top – Progress bar
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(horizontal = 24.dp, vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (hasAudioFile && !isInRecordingMode && sheetAudioDuration > 0L) {
                                        AudioProgressBar(
                                            currentPositionMillis = player.currentPlaybackPositionMillis,
                                            totalDurationMillis = sheetAudioDuration,
                                            isActive = isSheetAudioActive,
                                            onSeek = { player.seekTo(it) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            // Bottom row – 70% of height
                            Row(
                                modifier = Modifier
                                    .weight(0.7f)
                                    .fillMaxWidth()
                            ) {
                                // Start Bottom – Waveform / Transcript (full height)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val progress = if (isSheetAudioActive && sheetAudioDuration > 0L)
                                        (player.currentPlaybackPositionMillis.toFloat() / sheetAudioDuration.coerceAtLeast(1L)).coerceIn(0f, 1f)
                                    else 0f

                                    AudioContentDisplay(
                                        selectedAudioViewType = selectedAudioViewType,
                                        amplitudes = amplitudes,
                                        isRecordingMode = isInRecordingMode,
                                        recordingState = recordingState,
                                        progress = progress,
                                        modifier = Modifier
                                            .fillMaxSize()
                                    )
                                }

                                // End Bottom – Controls vertically centered
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AudioControlButtons(
                                        recordingState = recordingState,
                                        isSheetAudioPlaying = isSheetAudioPlaying,
                                        isSheetAudioPaused = isSheetAudioPaused,
                                        currentSheetAudioPath = currentSheetAudioPath,
                                        hasUnsavedRecording = !recorder.isPersistentAudio,
                                        toolbarHeight = toolbarHeight,
                                        onRecordClick = {
                                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                                == PackageManager.PERMISSION_GRANTED
                                            ) {
                                                recorder.startRecording()
                                            } else {
                                                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        },
                                        onPauseRecordingClick = { recorder.pauseRecording() },
                                        onResumeRecordingClick = { recorder.startRecording() },
                                        onStopRecordingClick = { recorder.stopRecording() },
                                        onPlayPauseClick = {
                                            currentSheetAudioPath?.let { path ->
                                                when {
                                                    isSheetAudioPlaying -> player.pauseAudio()
                                                    isSheetAudioPaused -> player.resumeAudio()
                                                    else -> player.playAudio(path)
                                                }
                                            }
                                        },
                                        onStopPlaybackClick = { player.stopAudio() },
                                        onDiscardClick = {
                                            recorder.resetState()
                                            recorder.deleteRecording()
                                            player.stopAudio()
                                        }
                                    )
                                }
                            }
                        }
                    }else {
                        // ────── PORTRAIT: vertical stack ──────
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AudioTimerDisplay(
                                isRecording = isInRecordingMode,
                                isPlaying = isSheetAudioPlaying,
                                recordingDurationMillis = recorder.recordingDurationMillis,
                                currentPlaybackPositionMillis = player.currentPlaybackPositionMillis,
                                totalAudioDurationMillis = if (hasAudioFile && !isInRecordingMode) sheetAudioDuration else 0L,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )

                            val progress = if (isSheetAudioActive && sheetAudioDuration > 0L)
                                (player.currentPlaybackPositionMillis.toFloat() / sheetAudioDuration.coerceAtLeast(1L)).coerceIn(0f, 1f)
                            else 0f

                            AudioContentDisplay(
                                selectedAudioViewType = selectedAudioViewType,
                                amplitudes = amplitudes,
                                isRecordingMode = isInRecordingMode,
                                recordingState = recordingState,
                                progress = progress,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            )

                            if (hasAudioFile && !isInRecordingMode && sheetAudioDuration > 0L) {
                                AudioProgressBar(
                                    currentPositionMillis = player.currentPlaybackPositionMillis,
                                    totalDurationMillis = sheetAudioDuration,
                                    isActive = isSheetAudioActive,
                                    onSeek = { player.seekTo(it) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Spacer(modifier = Modifier.height(totalHeight))
                            }
                        }

                        // Controls at bottom
                        AudioControlButtons(
                            recordingState = recordingState,
                            isSheetAudioPlaying = isSheetAudioPlaying,
                            isSheetAudioPaused = isSheetAudioPaused,
                            currentSheetAudioPath = currentSheetAudioPath,
                            hasUnsavedRecording = !recorder.isPersistentAudio,
                            toolbarHeight = toolbarHeight,
                            onRecordClick = {
                                if (ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    recorder.startRecording()
                                } else {
                                    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            onPauseRecordingClick = { recorder.pauseRecording() },
                            onResumeRecordingClick = { recorder.startRecording() }, // resumes
                            onStopRecordingClick = { recorder.stopRecording() },
                            onPlayPauseClick = {
                                currentSheetAudioPath?.let { path ->
                                    when {
                                        isSheetAudioPlaying -> player.pauseAudio()
                                        isSheetAudioPaused -> player.resumeAudio()
                                        else -> player.playAudio(path)
                                    }
                                }
                            },
                            onStopPlaybackClick = { player.stopAudio() },
                            onDiscardClick = { recorder.resetState(); recorder.deleteRecording(); player.stopAudio() }
                        )
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
                        TextStyle(
                            fontFamily = QuicksandTitleVariable,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
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
                                    Text(
                                        "Title",
                                        style = titleTextStyle,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                innerTextField()
                            }
                        })

                    Box {
                        IconButton(
                            onClick = { showMenu = !showMenu }, modifier = Modifier.padding(4.dp)
                        ) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "More options")
                        }
                        DropdownNoteMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            items = listOfNotNull(
                                MenuItem(
                                text = "Label",
                                onClick = { showLabelDialog = true; showMenu = false },
                                dismissOnClick = true,
                                icon = {
                                    if (isLabeled) Icon(
                                        Icons.Rounded.Bookmark, "Label", tint = labelColor
                                    )
                                    else Icon(Icons.Rounded.BookmarkBorder, "Label")
                                }), MenuItem(text = colorMenuItemText, onClick = {
                                val nextIndex =
                                    (availableThemes.indexOf(selectedTheme) + 1) % availableThemes.size
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
                                Icon(
                                    Icons.Rounded.ColorLens,
                                    "Color",
                                    tint = if (selectedTheme == "Default") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                                )
                            }, textColor = animatedTextColor), MenuItem(
                                text = if (isOffline) "Offline note" else "Online note",
                                onClick = { isOffline = !isOffline },
                                dismissOnClick = false,
                                textColor = if (isOffline) MaterialTheme.colorScheme.error else null,
                                icon = {
                                    if (isOffline) Icon(
                                        Icons.Rounded.CloudOff,
                                        "Offline note",
                                        tint = MaterialTheme.colorScheme.error
                                    )
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioControlButtons(
    recordingState: RecordingState,
    isSheetAudioPlaying: Boolean,
    isSheetAudioPaused: Boolean,
    currentSheetAudioPath: String?,
    hasUnsavedRecording: Boolean = false,
    toolbarHeight: Dp,
    onRecordClick: () -> Unit,
    onPauseRecordingClick: () -> Unit,
    onResumeRecordingClick: () -> Unit,
    onStopRecordingClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onStopPlaybackClick: () -> Unit,
    onDiscardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isActionActive = isSheetAudioPlaying || recordingState == RecordingState.RECORDING
    val animatedRadius by animateDpAsState(
        targetValue = if (isActionActive) 32.dp else 64.dp,
        animationSpec = tween(250),
        label = "controlButtonRadius"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
//            .padding(
//                bottom = WindowInsets.navigationBars.asPaddingValues()
//                    .calculateBottomPadding() + toolbarHeight + 16.dp
//            ),
        ,horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (recordingState) {
            RecordingState.IDLE -> {
                FilledIconButton(
                    onClick = onRecordClick,
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
                        onClick = onPauseRecordingClick,
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
                        Icon(Icons.Rounded.Pause, "Pause recording", Modifier.size(40.dp))
                    }

                    FilledIconButton(
                        onClick = onStopRecordingClick,
                        shape = RoundedCornerShape(64.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier
                            .height(136.dp)
                            .weight(1f)
                            .widthIn(max = 184.dp)
                    ) {
                        Icon(Icons.Rounded.Stop, "Stop recording", Modifier.size(40.dp))
                    }
                }
            }

            RecordingState.PAUSED -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledIconButton(
                        onClick = onResumeRecordingClick,
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
                        Icon(Icons.Rounded.Mic, "Resume recording", Modifier.size(40.dp))
                    }

                    FilledIconButton(
                        onClick = onStopRecordingClick,
                        shape = RoundedCornerShape(64.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier
                            .height(136.dp)
                            .weight(1f)
                            .widthIn(max = 184.dp)
                    ) {
                        Icon(Icons.Rounded.Stop, "Stop recording", Modifier.size(40.dp))
                    }
                }
            }

            else -> { // Playback mode (VIEWING_SAVED_AUDIO, STOPPED_UNSAVED, etc.)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledIconButton(
                        onClick = onPlayPauseClick,
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
                        onClick = onStopPlaybackClick,
                        enabled = isSheetAudioPlaying || isSheetAudioPaused,
                        shape = RoundedCornerShape(64.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .height(136.dp)
                            .weight(1f)
                            .widthIn(max = 184.dp)
                    ) {
                        Icon(Icons.Rounded.Stop, "Stop playback", Modifier.size(40.dp))
                    }

                    if (hasUnsavedRecording && currentSheetAudioPath != null) {
                        FilledIconButton(
                            onClick = onDiscardClick,
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

// Alternative version (sometimes nicer)
@Composable
fun AudioContentDisplay(
    selectedAudioViewType: AudioViewType,
    amplitudes: List<Float>,
    isRecordingMode: Boolean,
    recordingState: RecordingState,
    progress: Float,  // pre-calculated
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (selectedAudioViewType) {
            AudioViewType.Waveform -> WaveformDisplay(
                modifier = Modifier.fillMaxSize(),
                amplitudes = amplitudes,
                isRecording = isRecordingMode,
                progress = progress,
                recordingState = recordingState
            )

            AudioViewType.Transcript -> TranscriptDisplay(Modifier.fillMaxSize())
        }
    }
}

@Composable
fun AudioProgressBar(
    currentPositionMillis: Long,
    totalDurationMillis: Long,
    isActive: Boolean = true, // whether this audio is the one currently controlled
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    require(totalDurationMillis > 0L) { "totalDurationMillis must be > 0 when showing progress bar" }

    val progress =
        (currentPositionMillis.toFloat() / totalDurationMillis.coerceAtLeast(1L)).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(isActive, totalDurationMillis) {
                if (isActive && totalDurationMillis > 0L) {
                    detectTapGestures { offset ->
                        val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        val seekTo = (totalDurationMillis * newProgress).toLong()
                        onSeek(seekTo)
                    }
                }
            }) {
        Column(Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(100f))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent, // fully transparent track
                strokeCap = StrokeCap.Round
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(currentPositionMillis),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDuration(totalDurationMillis),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.height(16.dp))
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
        fontFamily = QuicksandTitleVariable,
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
