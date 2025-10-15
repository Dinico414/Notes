package com.xenonware.notes.ui.res

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xenon.mylibrary.QuicksandTitleVariable
import com.xenonware.notes.ui.values.LargestPadding
import com.xenonware.notes.ui.values.MediumCornerRadius
import com.xenonware.notes.viewmodel.classes.NotesItems
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CellAudioNote(
    item: NotesItems,
    isSelected: Boolean,
    isSelectionModeActive: Boolean,
    onSelectItem: () -> Unit,
    onEditItem: (NotesItems) -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "Border Color Animation"
    )

    val context = LocalContext.current
    val playerManager = remember { AudioPlayerManager() }
    var recordingState by remember { mutableStateOf(RecordingState.IDLE) }

    LaunchedEffect(playerManager.currentRecordingState) {
        recordingState = playerManager.currentRecordingState
    }

    LaunchedEffect(
        recordingState, playerManager.isPlaying, playerManager.totalAudioDurationMillis
    ) {
        if (recordingState == RecordingState.PLAYING && playerManager.isPlaying) {
            while (isActive && playerManager.isPlaying) {
                playerManager.currentPlaybackPositionMillis =
                    (playerManager.currentPlaybackPositionMillis + 10L).coerceAtMost(playerManager.totalAudioDurationMillis)
                delay(10L)
            }
        } else if (recordingState == RecordingState.PAUSED) {
        } else {
            if (playerManager.currentPlaybackPositionMillis > 0L && playerManager.currentPlaybackPositionMillis >= playerManager.totalAudioDurationMillis) {
                playerManager.currentPlaybackPositionMillis = 0L
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            playerManager.dispose()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MediumCornerRadius))
            .background(MaterialTheme.colorScheme.surfaceBright)
            .border(
                width = 2.dp, color = borderColor, shape = RoundedCornerShape(MediumCornerRadius)
            )
            .combinedClickable(
                onClick = {
                    if (isSelectionModeActive) {
                        onSelectItem()
                    } else {
                        onEditItem(item)
                    }
                }, onLongClick = onSelectItem
            )
    ) {
        Column(
            modifier = Modifier.padding(top = LargestPadding)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = QuicksandTitleVariable,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = LargestPadding)
            )

            if (!item.description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start
                ) {
                    val currentProgress = if (playerManager.totalAudioDurationMillis > 0) {
                        playerManager.currentPlaybackPositionMillis.toFloat() / playerManager.totalAudioDurationMillis
                    } else {
                        0f
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .padding(horizontal = LargestPadding)
                            .background(Color.Transparent)
                            .pointerInput(playerManager) {
                                detectTapGestures { offset ->
                                    val newProgress = offset.x / size.width
                                    val newPositionMillis =
                                        (playerManager.totalAudioDurationMillis * newProgress).toLong()
                                    playerManager.seekTo(newPositionMillis)
                                }
                            }
                    ) {
                        LinearProgressIndicator(
                            progress = { currentProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            strokeCap = StrokeCap.Round
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, bottom = 4.dp, end = 40.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val uniqueAudioId = item.description
                                val audioFilePath =
                                    File(context.filesDir, "$uniqueAudioId.mp3").absolutePath
                                if (playerManager.isPlaying) {
                                    playerManager.pauseAudio()
                                } else {
                                    if (recordingState == RecordingState.PAUSED) {
                                        playerManager.resumeAudio()
                                    } else {
                                        playerManager.playAudio(audioFilePath)
                                    }
                                }
                            }) {
                            Icon(
                                imageVector = if (playerManager.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playerManager.isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(MediumCornerRadius))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${formatDuration(playerManager.currentPlaybackPositionMillis)} / ${
                                    formatDuration(
                                        playerManager.totalAudioDurationMillis
                                    )
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isSelectionModeActive,
            modifier = Modifier.align(Alignment.TopStart),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .padding(6.dp)
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.surfaceBright, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(targetState = isSelected, label = "Selection Animation") {
                    if (it) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .size(20.dp)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 4.dp, end = 4.dp)
                .size(32.dp), contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.onSurface, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Audio",
                    tint = MaterialTheme.colorScheme.surfaceBright,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(24.dp)
                )
            }
        }
    }
}