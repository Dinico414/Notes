@file:Suppress("AssignedValueIsNeverRead", "unused", "DEPRECATION")

package com.xenonware.notes.ui.res.sheets

import android.Manifest.permission.RECORD_AUDIO
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenonware.notes.data.SharedPreferenceManager
import com.xenonware.notes.ui.res.LabelSelectionDialog
import com.xenonware.notes.ui.res.MenuItem
import com.xenonware.notes.ui.res.TranscriptDisplay
import com.xenonware.notes.ui.res.WaveformDisplay
import com.xenonware.notes.ui.res.XenonDropDown
import com.xenonware.notes.ui.res.loadAmplitudes
import com.xenonware.notes.ui.res.saveAmplitudes
import com.xenonware.notes.ui.theme.LocalIsDarkTheme
import com.xenonware.notes.ui.theme.XenonTheme
import com.xenonware.notes.ui.theme.extendedMaterialColorScheme
import com.xenonware.notes.util.AVAILABLE_MODELS
import com.xenonware.notes.util.AudioRecorderManager
import com.xenonware.notes.util.GlobalAudioPlayer
import com.xenonware.notes.util.RecordingState
import com.xenonware.notes.util.TranscriptSegment
import com.xenonware.notes.util.VoskSpeechRecognitionManager
import com.xenonware.notes.util.loadTranscript
import com.xenonware.notes.util.saveTranscript
import com.xenonware.notes.viewmodel.NoteEditingViewModel
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

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalHazeMaterialsApi::class
)
@Composable
fun NoteAudioSheet(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String?, Boolean) -> Unit,
    toolbarHeight: Dp,
    saveTrigger: Boolean,
    onSaveTriggerConsumed: () -> Unit,
    selectedAudioViewType: AudioViewType,
    initialAudioFilePath: String? = null,
    allLabels: List<Label>,
    onAddNewLabel: (String) -> Unit,
    noteEditingViewModel: NoteEditingViewModel,
    onHasUnsavedAudioChange: (Boolean) -> Unit = {},
    isBlackThemeActive: Boolean = false,
    isCoverModeActive: Boolean = false,
) {
    val hazeState = remember { HazeState() }
    val isDarkTheme = LocalIsDarkTheme.current

    val title by noteEditingViewModel.audioTitle.collectAsStateWithLifecycle()
    val theme by noteEditingViewModel.audioTheme.collectAsStateWithLifecycle()
    val labelId by noteEditingViewModel.audioLabelId.collectAsStateWithLifecycle()
    val isOffline by noteEditingViewModel.audioIsOffline.collectAsStateWithLifecycle()
    val isLandscape = LocalConfiguration.current.screenWidthDp > LocalConfiguration.current.screenHeightDp

    val context = LocalContext.current
    val player = GlobalAudioPlayer.getInstance()
    val amplitudes = remember { mutableStateListOf<Float>() }
    val scope = rememberCoroutineScope()

    val cachedAudioUniqueId by noteEditingViewModel.audioUniqueId.collectAsStateWithLifecycle()
    val cachedAmplitudes by noteEditingViewModel.audioAmplitudes.collectAsStateWithLifecycle()
    val cachedRecordingDuration by noteEditingViewModel.audioRecordingDuration.collectAsStateWithLifecycle()
    val cachedIsPersistent by noteEditingViewModel.audioIsPersistent.collectAsStateWithLifecycle()
    val transcriptSegments by noteEditingViewModel.audioTranscriptSegments.collectAsStateWithLifecycle()

    val recorder = remember {
        AudioRecorderManager(context) {
            player.totalAudioDurationMillis = 0L
            player.currentPlaybackPositionMillis = 0L
            player.stopAudio()
            amplitudes.clear()
        }
    }

    val prefsManager = remember { SharedPreferenceManager(context) }
    var currentModelKey by remember { mutableStateOf(prefsManager.voskModelKey) }

    val speechRecognitionManager = remember(currentModelKey) {
        VoskSpeechRecognitionManager(context, language = currentModelKey.substringBefore('-')).apply {
            switchModel(
                modelKey = currentModelKey,
                onSuccess = { android.util.Log.i("Vosk", "Model ready: $currentModelKey") },
                onError = { msg: String -> android.util.Log.e("Vosk", "Model error: $msg") }
            )
            onTranscriptUpdate = { segments ->
                noteEditingViewModel.setAudioTranscriptSegments(segments)
            }
        }
    }

    DisposableEffect(speechRecognitionManager) {
        onDispose { speechRecognitionManager.dispose() }
    }

    val currentSheetAudioPath = recorder.audioFilePath ?: initialAudioFilePath
    val isSheetAudioActive = player.currentFilePath == currentSheetAudioPath
    val isSheetAudioPlaying = player.isPlaying && isSheetAudioActive
    val isSheetAudioPaused = !player.isPlaying && isSheetAudioActive && player.currentPlaybackPositionMillis > 0

    val selectedTheme = theme.ifEmpty { "Default" }
    val isLabeled = labelId != null

    var showMenu by remember { mutableStateOf(false) }
    var showLabelDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    var colorMenuItemText by remember { mutableStateOf("Color") }
    var isFadingOut by remember { mutableStateOf(false) }
    var colorChangeJob by remember { mutableStateOf<Job?>(null) }

    val availableThemes = remember {
        listOf("Default", "Red", "Orange", "Yellow", "Green", "Turquoise", "Blue", "Purple")
    }

    var recordingState by remember { mutableStateOf(RecordingState.IDLE) }

    val hasAudioContent = remember(initialAudioFilePath, recorder.audioFilePath, cachedAudioUniqueId) {
        derivedStateOf { initialAudioFilePath != null || recorder.audioFilePath != null || cachedAudioUniqueId != null }
    }.value

    val systemUiController = rememberSystemUiController()
    val originalStatusBarColor = Color.Transparent

    DisposableEffect(systemUiController, isDarkTheme) {
        systemUiController.setStatusBarColor(Color.Transparent, darkIcons = !isDarkTheme)
        onDispose { systemUiController.setStatusBarColor(originalStatusBarColor) }
    }

    LaunchedEffect(hasAudioContent) { onHasUnsavedAudioChange(hasAudioContent) }

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

    LaunchedEffect(Unit) {
        if (initialAudioFilePath != null && File(initialAudioFilePath).exists()) {
            recorder.setInitialAudioFilePath(initialAudioFilePath)
            val audioId = File(initialAudioFilePath).nameWithoutExtension
            val loadedAmplitudes = loadAmplitudes(context, audioId)
            if (loadedAmplitudes.isNotEmpty()) {
                amplitudes.clear()
                amplitudes.addAll(loadedAmplitudes)
            }
            val loadedTranscript = loadTranscript(context, audioId)
            if (loadedTranscript.isNotEmpty()) {
                speechRecognitionManager.loadTranscript(loadedTranscript)
                noteEditingViewModel.setAudioTranscriptSegments(loadedTranscript)
            }
        } else if (cachedAudioUniqueId != null) {
            recorder.restoreCachedState(cachedAudioUniqueId, cachedRecordingDuration, cachedIsPersistent)
            if (cachedAmplitudes.isNotEmpty()) {
                amplitudes.clear()
                amplitudes.addAll(cachedAmplitudes)
            }
            val loadedTranscript = loadTranscript(context, cachedAudioUniqueId)
            if (loadedTranscript.isNotEmpty()) {
                speechRecognitionManager.loadTranscript(loadedTranscript)
                noteEditingViewModel.setAudioTranscriptSegments(loadedTranscript)
            }
        }
    }

    LaunchedEffect(recordingState) {
        if (recordingState == RecordingState.RECORDING) {
            while (isActive) {
                delay(1000L)
                recorder.recordingDurationMillis += 1000L
            }
        }
    }

    LaunchedEffect(recordingState) {
        if (recordingState == RecordingState.RECORDING) {
            while (isActive) {
                amplitudes.add(recorder.getMaxAmplitude().toFloat())
                delay(100L)
            }
        }
    }

    LaunchedEffect(player.isPlaying) {
        if (player.isPlaying) {
            while (isActive) {
                player.currentPlaybackPositionMillis = player.mediaPlayer?.currentPosition?.toLong() ?: 0L
                delay(50L)
            }
        }
    }

    LaunchedEffect(recorder.uniqueAudioId, recorder.recordingDurationMillis, recorder.isPersistentAudio) {
        noteEditingViewModel.setAudioUniqueId(recorder.uniqueAudioId)
        noteEditingViewModel.setAudioRecordingDuration(recorder.recordingDurationMillis)
        noteEditingViewModel.setAudioIsPersistent(recorder.isPersistentAudio)
    }

    LaunchedEffect(amplitudes.toList()) {
        if (amplitudes.isNotEmpty()) noteEditingViewModel.setAudioAmplitudes(amplitudes.toList())
    }

    LaunchedEffect(saveTrigger) {
        if (saveTrigger) {
            val audioId = recorder.uniqueAudioId
                ?: cachedAudioUniqueId
                ?: initialAudioFilePath?.let { File(it).nameWithoutExtension }

            val hasAudio = audioId != null

            if (title.isNotBlank() && hasAudio) {
                if (recorder.currentRecordingState == RecordingState.RECORDING ||
                    recorder.currentRecordingState == RecordingState.PAUSED) {
                    recorder.stopRecording()
                    speechRecognitionManager.stopListening()
                }
                player.stopAudio()

                recorder.markAudioAsPersistent()

                if (amplitudes.isNotEmpty()) saveAmplitudes(context, audioId, amplitudes)
                if (transcriptSegments.isNotEmpty()) saveTranscript(context, audioId, transcriptSegments)

                onSave(title, audioId, selectedTheme, labelId, isOffline)
            }
            onSaveTriggerConsumed()
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            recorder.startRecording()
            if (speechRecognitionManager.isAvailable()) {
                speechRecognitionManager.startListening(System.currentTimeMillis())
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recorder.dispose()
            speechRecognitionManager.dispose()
        }
    }

    val hasAudioFile = currentSheetAudioPath != null
    val isInRecordingMode = recordingState == RecordingState.RECORDING || recordingState == RecordingState.PAUSED

    @Suppress("SENSELESS_COMPARISON")
    val sheetAudioDuration by remember(currentSheetAudioPath, recordingState) {
        mutableLongStateOf(
            if (hasAudioFile && !isInRecordingMode && currentSheetAudioPath != null)
                player.getAudioDuration(currentSheetAudioPath)
            else 0L
        )
    }

    val currentModelInfo = remember(currentModelKey) {
        AVAILABLE_MODELS.find { it.key == currentModelKey }
    }

    val langName = when (currentModelInfo?.lang) {
        "en" -> "English"
        "de" -> "Deutsch"
        else -> "Voice"
    }
    val sizeName = if ((currentModelInfo?.approxSizeMB ?: 0) > 100) "Large" else "Small"
    val isLargeSelected = (currentModelInfo?.approxSizeMB ?: 0) > 100
    val isInstalled = currentModelInfo != null &&
            File(context.filesDir, currentModelInfo.folderName).exists() &&
            File(context.filesDir, currentModelInfo.folderName).listFiles()?.isNotEmpty() == true

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
            animationSpec = tween(500)
        )

        if (showLabelDialog) {
            LabelSelectionDialog(
                allLabels = allLabels,
                selectedLabelId = labelId,
                onLabelSelected = noteEditingViewModel::setAudioLabelId,
                onAddNewLabel = onAddNewLabel,
                onDismiss = { showLabelDialog = false }
            )
        }

        val hazeThinColor = colorScheme.surfaceDim
        val labelColor = extendedMaterialColorScheme.label
        val backgroundColor = if (isCoverModeActive || isBlackThemeActive) Color.Black else colorScheme.surfaceContainer

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .hazeSource(hazeState)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))
                    .padding(horizontal = 20.dp)
                    .padding(bottom = toolbarHeight + 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(68.dp))

                    Button(
                        onClick = { showModelDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.surfaceVariant,
                            contentColor = colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Mic,
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "$langName ($sizeName)",
                                    style = typography.labelLarge,
                                    color = colorScheme.onSurface
                                )
                            }

                            if (isLargeSelected && !isInstalled) {
                                FilledIconButton(
                                    onClick = {
                                        speechRecognitionManager.switchModel(
                                            modelKey = currentModelKey,
                                            onSuccess = {
                                                currentModelKey = currentModelInfo!!.key
                                                prefsManager.voskModelKey = currentModelInfo.key
                                                if (recordingState == RecordingState.RECORDING) {
                                                    if (ActivityCompat.checkSelfPermission(
                                                            this as Context,
                                                            RECORD_AUDIO
                                                        ) != PackageManager.PERMISSION_GRANTED
                                                    )
                                                    speechRecognitionManager.restartListening()
                                                }
                                            },
                                            onError = { msg: String ->
                                                android.util.Log.e("ModelDownload", msg)
                                            }
                                        )
                                        // Prevent dialog open when tapping download
                                    },
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = colorScheme.secondaryContainer,
                                        contentColor = colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Download,
                                        contentDescription = "Download large model",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    AudioTimerDisplay(
                        isRecording = isInRecordingMode,
                        isPlaying = isSheetAudioPlaying,
                        recordingDurationMillis = recorder.recordingDurationMillis,
                        currentPlaybackPositionMillis = player.currentPlaybackPositionMillis,
                        totalAudioDurationMillis = if (hasAudioFile && !isInRecordingMode) sheetAudioDuration else 0L
                    )

                    Spacer(Modifier.height(16.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (speechRecognitionManager.isModelLoading) {
                            Column(
                                Modifier.fillMaxSize(),
                                Arrangement.Center,
                                Alignment.CenterHorizontally
                            ) {
                                LinearProgressIndicator(modifier = Modifier.width(240.dp))
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "Loading voice model...",
                                    style = typography.titleMedium
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "(First time may take 10–120 seconds)",
                                    style = typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            val progress = if (isSheetAudioActive && sheetAudioDuration > 0L)
                                (player.currentPlaybackPositionMillis.toFloat() / sheetAudioDuration.coerceAtLeast(1L)).coerceIn(0f, 1f)
                            else 0f

                            AudioContentDisplay(
                                selectedAudioViewType = selectedAudioViewType,
                                amplitudes = amplitudes,
                                isRecordingMode = isInRecordingMode,
                                recordingState = recordingState,
                                progress = progress,
                                transcriptSegments = transcriptSegments,
                                isTranscribing = speechRecognitionManager.isTranscribing,
                                currentPartialText = speechRecognitionManager.currentPartialText,
                                errorMessage = speechRecognitionManager.errorMessage,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    if (!isLandscape && hasAudioFile && !isInRecordingMode && sheetAudioDuration > 0L) {
                        AudioProgressBar(
                            currentPositionMillis = player.currentPlaybackPositionMillis,
                            totalDurationMillis = sheetAudioDuration,
                            isActive = isSheetAudioActive,
                            onSeek = { player.seekTo(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    AudioControlButtons(
                        recordingState = recordingState,
                        isSheetAudioPlaying = isSheetAudioPlaying,
                        isSheetAudioPaused = isSheetAudioPaused,
                        currentSheetAudioPath = currentSheetAudioPath,
                        hasUnsavedRecording = !recorder.isPersistentAudio,
                        toolbarHeight = toolbarHeight,
                        onRecordClick = {
                            if (ContextCompat.checkSelfPermission(context, RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                recorder.startRecording()
                                if (speechRecognitionManager.isAvailable()) {
                                    speechRecognitionManager.startListening(System.currentTimeMillis())
                                }
                            } else {
                                requestPermissionLauncher.launch(RECORD_AUDIO)
                            }
                        },
                        onPauseRecordingClick = { recorder.pauseRecording(); speechRecognitionManager.stopListening() },
                        onResumeRecordingClick = { recorder.startRecording(); if (speechRecognitionManager.isAvailable()) speechRecognitionManager.restartListening() },
                        onStopRecordingClick = { recorder.stopRecording(); speechRecognitionManager.stopListening() },
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
                            speechRecognitionManager.clearTranscript()
                            speechRecognitionManager.cancel()
                            noteEditingViewModel.setAudioTranscriptSegments(emptyList())
                        }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(100f))
                    .background(colorScheme.surfaceDim)
                    .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin(hazeThinColor)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, Modifier.padding(4.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }

                val titleTextStyle = typography.titleLarge.merge(
                    TextStyle(fontFamily = QuicksandTitleVariable, textAlign = TextAlign.Center, color = colorScheme.onSurface)
                )

                BasicTextField(
                    value = title,
                    onValueChange = noteEditingViewModel::setAudioTitle,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = titleTextStyle,
                    cursorBrush = SolidColor(colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if (title.isEmpty()) {
                                Text("Title", style = titleTextStyle, color = colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            innerTextField()
                        }
                    }
                )

                Box {
                    IconButton(onClick = { showMenu = !showMenu }, Modifier.padding(4.dp)) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More options")
                    }
                    XenonDropDown(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        items = listOfNotNull(
                            MenuItem(text = "Label", onClick = { showLabelDialog = true; showMenu = false }, dismissOnClick = true, icon = {
                                if (isLabeled) Icon(Icons.Rounded.Bookmark, null, tint = labelColor)
                                else Icon(Icons.Rounded.BookmarkBorder, null)
                            }),
                            MenuItem(text = colorMenuItemText, onClick = {
                                val currentIndex = availableThemes.indexOf(selectedTheme)
                                val nextIndex = (currentIndex + 1) % availableThemes.size
                                val newTheme = availableThemes[nextIndex]
                                noteEditingViewModel.setAudioTheme(newTheme)
                                colorChangeJob?.cancel()
                                colorChangeJob = scope.launch {
                                    colorMenuItemText = newTheme
                                    isFadingOut = false
                                    delay(2500)
                                    isFadingOut = true
                                    delay(500)
                                    colorMenuItemText = "Color"
                                    isFadingOut = false
                                }
                            }, dismissOnClick = false, icon = {
                                Icon(Icons.Rounded.ColorLens, null, tint = if (selectedTheme == "Default") colorScheme.onSurfaceVariant else colorScheme.primary)
                            }, textColor = animatedTextColor),
                            MenuItem(text = if (isOffline) "Offline note" else "Online note", onClick = {
                                noteEditingViewModel.setAudioIsOffline(!isOffline)
                            }, dismissOnClick = false, textColor = if (isOffline) colorScheme.error else null, icon = {
                                if (isOffline) Icon(Icons.Rounded.CloudOff, null, tint = colorScheme.error)
                                else Icon(Icons.Rounded.Cloud, null)
                            })
                        ),
                        hazeState = hazeState
                    )
                }
            }
        }

        if (showModelDialog) {
            AlertDialog(
                onDismissRequest = { showModelDialog = false },
                title = { Text("Select Voice Model") },
                text = {
                    LazyColumn {
                        items(AVAILABLE_MODELS) { modelInfo ->
                            ListItem(
                                headlineContent = { Text(modelInfo.name) },
                                supportingContent = { Text("~${modelInfo.approxSizeMB} MB") },
                                leadingContent = {
                                    if (modelInfo.key == currentModelKey) {
                                        Icon(Icons.Rounded.CheckCircle, null, tint = colorScheme.primary)
                                    } else {
                                        Icon(Icons.Rounded.RadioButtonUnchecked, null)
                                    }
                                },
                                modifier = Modifier.clickable {
                                    speechRecognitionManager.switchModel(
                                        modelKey = modelInfo.key,
                                        onSuccess = {
                                            currentModelKey = modelInfo.key
                                            prefsManager.voskModelKey = modelInfo.key
                                            if (recordingState == RecordingState.RECORDING) {
                                                speechRecognitionManager.restartListening()
                                            }
                                            showModelDialog = false
                                        },
                                        onError = { msg: String ->
                                            android.util.Log.e("ModelSwitch", msg)
                                            showModelDialog = false
                                        }
                                    )
                                }
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showModelDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioControlButtons(
    modifier: Modifier = Modifier,
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
) {
    val isActionActive = isSheetAudioPlaying || recordingState == RecordingState.RECORDING
    val animatedRadius by animateDpAsState(
        targetValue = if (isActionActive) 32.dp else 64.dp,
        animationSpec = tween(250),
        label = "controlButtonRadius"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (recordingState) {
            RecordingState.IDLE -> {
                FilledIconButton(
                    onClick = onRecordClick,
                    shape = RoundedCornerShape(64.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary
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
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
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
                            containerColor = colorScheme.error, contentColor = colorScheme.onError
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
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
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
                            containerColor = colorScheme.error, contentColor = colorScheme.onError
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

            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledIconButton(
                        onClick = onPlayPauseClick,
                        shape = RoundedCornerShape(animatedRadius),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
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
                            containerColor = colorScheme.primaryContainer,
                            contentColor = colorScheme.onPrimaryContainer
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
                                containerColor = colorScheme.errorContainer,
                                contentColor = colorScheme.onErrorContainer
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

enum class AudioViewType {
    Waveform, Transcript
}

@Composable
fun AudioContentDisplay(
    modifier: Modifier = Modifier,
    selectedAudioViewType: AudioViewType,
    amplitudes: List<Float>,
    isRecordingMode: Boolean,
    recordingState: RecordingState,
    progress: Float,
    transcriptSegments: List<TranscriptSegment> = emptyList(),
    isTranscribing: Boolean = false,
    currentPartialText: String = "",
    errorMessage: String? = null,
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

            AudioViewType.Transcript -> TranscriptDisplay(
                modifier = Modifier.fillMaxSize(),
                transcriptSegments = transcriptSegments,
                isRecording = isRecordingMode,
                isTranscribing = isTranscribing,
                currentPartialText = currentPartialText,
                errorMessage = errorMessage,
                onCopyTranscript = {}
            )
        }
    }
}

@Composable
fun AudioProgressBar(
    modifier: Modifier = Modifier,
    currentPositionMillis: Long,
    totalDurationMillis: Long,
    isActive: Boolean = true,
    onSeek: (Long) -> Unit,
) {
    require(totalDurationMillis > 0L) { "totalDurationMillis must be > 0 when showing progress bar" }

    val progress =
        (currentPositionMillis.toFloat() / totalDurationMillis.coerceAtLeast(1L)).coerceIn(0f, 1f)

    Box(
        modifier = modifier
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
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(100f))
                .background(colorScheme.primary.copy(alpha = 0.1f)),
            color = colorScheme.primary,
            trackColor = Color.Transparent,
            strokeCap = StrokeCap.Round
        )
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
        style = typography.headlineSmall,
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