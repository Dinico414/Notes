@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xenon.mylibrary.QuicksandTitleVariable
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.MediumCornerRadius
import com.xenonware.notes.ui.theme.LocalIsDarkTheme
import com.xenonware.notes.ui.theme.XenonTheme
import com.xenonware.notes.ui.theme.noteBlueDark
import com.xenonware.notes.ui.theme.noteBlueLight
import com.xenonware.notes.ui.theme.noteGreenDark
import com.xenonware.notes.ui.theme.noteGreenLight
import com.xenonware.notes.ui.theme.noteOrangeDark
import com.xenonware.notes.ui.theme.noteOrangeLight
import com.xenonware.notes.ui.theme.notePurpleDark
import com.xenonware.notes.ui.theme.notePurpleLight
import com.xenonware.notes.ui.theme.noteRedDark
import com.xenonware.notes.ui.theme.noteRedLight
import com.xenonware.notes.ui.theme.noteTurquoiseDark
import com.xenonware.notes.ui.theme.noteTurquoiseLight
import com.xenonware.notes.ui.theme.noteYellowDark
import com.xenonware.notes.ui.theme.noteYellowLight
import com.xenonware.notes.viewmodel.classes.NotesItems
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File

private fun togglePlayback(
    playerManager: AudioPlayerManager,
    audioFilePath: String?,
    recordingState: RecordingState
) {
    if (audioFilePath == null) return

    when {
        playerManager.isPlaying && playerManager.currentFilePath == audioFilePath -> {
            playerManager.pauseAudio()
        }
        playerManager.currentFilePath == audioFilePath -> {
            playerManager.playAudio(audioFilePath)
        }
        else -> {
            playerManager.playAudio(audioFilePath)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteAudioCard(
    item: NotesItems,
    isSelected: Boolean,
    isSelectionModeActive: Boolean,
    onSelectItem: () -> Unit,
    onEditItem: (NotesItems) -> Unit,
    modifier: Modifier = Modifier,
    isNoteSheetOpen: Boolean,
) {
    val isDarkTheme = LocalIsDarkTheme.current
    val context = LocalContext.current
    val player = GlobalAudioPlayer.getInstance()

    // Map color to theme name
    val colorToThemeName = remember {
        mapOf(
            noteRedLight.value to "Red", noteRedDark.value to "Red",
            noteOrangeLight.value to "Orange", noteOrangeDark.value to "Orange",
            noteYellowLight.value to "Yellow", noteYellowDark.value to "Yellow",
            noteGreenLight.value to "Green", noteGreenDark.value to "Green",
            noteTurquoiseLight.value to "Turquoise", noteTurquoiseDark.value to "Turquoise",
            noteBlueLight.value to "Blue", noteBlueDark.value to "Blue",
            notePurpleLight.value to "Purple", notePurpleDark.value to "Purple"
        )
    }
    val selectedTheme = item.color?.let { colorToThemeName[it.toULong()] } ?: "Default"

    // Resolve actual file path
    val audioFilePath = remember(item.description) {
        item.description?.let { uniqueId ->
            File(context.filesDir, "$uniqueId.mp3").takeIf { it.exists() }?.absolutePath
        }
    }

    // Duration of THIS specific audio file (cached once)
    val thisAudioDuration by remember(audioFilePath) {
        mutableLongStateOf(audioFilePath?.let { player.getAudioDuration(it) } ?: 0L)
    }

    // Determine if THIS card is the one currently active
    val isThisAudioActive = player.currentFilePath == audioFilePath
    val isPlayingThis = player.isPlaying && isThisAudioActive
    val isPausedThis = !player.isPlaying && isThisAudioActive && player.currentPlaybackPositionMillis > 0

    // Live playback position updater
    LaunchedEffect(player.isPlaying) {
        if (player.isPlaying) {
            while (isActive) {
                player.currentPlaybackPositionMillis = player.mediaPlayer?.currentPosition?.toLong() ?: 0L
                delay(100L)
            }
        }
    }

    // Sync recording state if needed (you can remove if not used here)
    var recordingState by remember { mutableStateOf(RecordingState.IDLE) }
    LaunchedEffect(player.currentRecordingState) {
        recordingState = player.currentRecordingState
    }

    // Clean up on dispose (optional — singleton handles it)
    DisposableEffect(Unit) { onDispose { /* no-op — singleton manages lifecycle */ } }

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
        val borderColor by animateColorAsState(
            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
            label = "Border Color"
        )
        val backgroundColor = if (selectedTheme == "Default")
            MaterialTheme.colorScheme.surfaceBright else MaterialTheme.colorScheme.inversePrimary

        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MediumCornerRadius))
                .background(backgroundColor)
                .border(2.dp, borderColor, RoundedCornerShape(MediumCornerRadius))
                .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.075f), RoundedCornerShape(MediumCornerRadius))
                .combinedClickable(
                    enabled = !isNoteSheetOpen,
                    onClick = { if (isSelectionModeActive) onSelectItem() else onEditItem(item) },
                    onLongClick = onSelectItem
                )
        ) {
            Column(modifier = Modifier.padding(top = LargestPadding)) {
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

                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Calculate progress for THIS file only
                        val currentProgress = when {
                            isThisAudioActive -> player.currentPlaybackPositionMillis.toFloat() / thisAudioDuration.coerceAtLeast(1L)
                            else -> 0f
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val hasEnoughSpace = maxWidth >= 220.dp // Rough threshold

                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Seekable progress bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(16.dp)
                                        .padding(horizontal = 8.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .pointerInput(Unit) {
                                            if (thisAudioDuration > 0L && isThisAudioActive) {
                                                detectTapGestures { offset ->
                                                    val progress = (offset.x / size.width).coerceIn(0f, 1f)
                                                    val seekTo = (thisAudioDuration * progress).toLong()
                                                    player.seekTo(seekTo)
                                                }
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

                                if (hasEnoughSpace) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 4.dp, end = 40.dp, bottom = 4.dp),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Play/Pause Button — only shows Pause if THIS file is playing
                                        FilledIconButton(
                                            onClick = { togglePlayback(player, audioFilePath, recordingState) },
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            shape = CircleShape,
                                            modifier = Modifier.size(42.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isPlayingThis) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                                contentDescription = if (isPlayingThis) "Pause" else "Play",
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(MediumCornerRadius))
                                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = when {
                                                    isThisAudioActive -> "${formatDuration(player.currentPlaybackPositionMillis)} / ${formatDuration(thisAudioDuration)}"
                                                    else -> "00:00 / ${formatDuration(thisAudioDuration)}"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    // Compact layout
                                    Column(modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .padding(bottom = 4.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(MediumCornerRadius))
                                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = if (isThisAudioActive)
                                                    "${formatDuration(player.currentPlaybackPositionMillis)} / ${formatDuration(thisAudioDuration)}"
                                                else "00:00 / ${formatDuration(thisAudioDuration)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        FilledIconButton(
                                            onClick = { togglePlayback(player, audioFilePath, recordingState) },
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            shape = RoundedCornerShape(MediumCornerRadius),
                                            modifier = Modifier
                                                .padding(end = 32.dp)
                                                .fillMaxWidth()
                                                .height(42.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isPlayingThis) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                                contentDescription = if (isPlayingThis) "Pause" else "Play",
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Selection checkbox
            AnimatedVisibility(
                visible = isSelectionModeActive,
                modifier = Modifier.align(Alignment.TopStart),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier.padding(6.dp).size(24.dp).background(backgroundColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(targetState = isSelected) { selected ->
                        if (selected) {
                            Icon(Icons.Rounded.CheckCircle, "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        } else {
                            Box(Modifier.padding(2.dp).size(20.dp).border(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), CircleShape))
                        }
                    }
                }
            }

            // Mic badge
            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).size(26.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(22.dp).background(MaterialTheme.colorScheme.onSurface, CircleShape))
                Icon(
                    Icons.Rounded.Mic,
                    contentDescription = "Audio",
                    tint = backgroundColor,
                    modifier = Modifier.align(Alignment.Center).size(18.dp)
                )
            }
        }
    }
}