package com.xenonware.notes.ui.res

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.xenonware.notes.ui.layouts.QuicksandTitleVariable
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials

@OptIn(ExperimentalMaterial3Api::class)
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
) {
    val hazeState = remember { HazeState() }
    val hazeThinColor = colorScheme.surfaceDim

    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var isStopped by remember { mutableStateOf(false) }
    var hasRecording by remember { mutableStateOf(false) } // Indicates if an audio exists
    var selectedView by remember { mutableIntStateOf(0) } // 0 for waveform, 1 for transcript

    LaunchedEffect(saveTrigger) {
        if (saveTrigger) {
            onSave(audioTitle, "") // Audio notes currently don't have editable text content
            onSaveTriggerConsumed()
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

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SegmentedButton(
                    selected = selectedView == 0,
                    onClick = { selectedView = 0 },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) {
                    Text("Waveform")
                }
                SegmentedButton(
                    selected = selectedView == 1,
                    onClick = { selectedView = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) {
                    Text("Transcript")
                }
            }

            // Waveform/Transcript Display Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (selectedView == 0) {
                    Text(
                        text = "Waveform visualization coming soon...",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
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
                // Start/Play Button
                IconButton(
                    onClick = {
                        if (isStopped && hasRecording) {
                            // Play recording
                            isStopped = false
                            isRecording = false // Not recording, but playing
                            isPaused = false
                            // TODO: Implement playback
                        } else if (!isRecording && !isPaused) {
                            // Start recording
                            isRecording = true
                            isPaused = false
                            isStopped = false
                            hasRecording = true
                            // TODO: Implement start recording
                        } else if (isPaused) {
                            // Resume recording
                            isRecording = true
                            isPaused = false
                            // TODO: Implement resume recording
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
                            isPaused = true
                            isRecording = false
                            // TODO: Implement pause recording
                        }
                    }, enabled = isRecording
                ) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause recording")
                }

                // Stop Button
                IconButton(
                    onClick = {
                        if (isRecording || isPaused) {
                            isStopped = true
                            isRecording = false
                            isPaused = false
                            // TODO: Implement stop recording
                        }
                    }, enabled = isRecording || isPaused
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