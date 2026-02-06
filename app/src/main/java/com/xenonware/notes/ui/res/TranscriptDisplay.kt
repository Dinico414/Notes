package com.xenonware.notes.ui.res

import android.content.ClipData
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xenonware.notes.util.audio.RecordingState
import com.xenonware.notes.util.audio.TranscriptSegment
import kotlinx.coroutines.launch

@Composable
fun TranscriptDisplay(
    modifier: Modifier = Modifier,
    transcriptSegments: List<TranscriptSegment> = emptyList(),
    isRecording: Boolean = false,
    isTranscribing: Boolean = false,
    currentPartialText: String = "",
    errorMessage: String? = null,
    onCopyTranscript: (() -> Unit)? = null,
    recordingState: RecordingState
) {
    val listState = rememberLazyListState()
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(transcriptSegments.size, currentPartialText) {
        if (transcriptSegments.isNotEmpty() || currentPartialText.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Box(modifier = modifier) {
        if (transcriptSegments.isEmpty() && currentPartialText.isEmpty() && !isRecording) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MicOff,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "No transcript available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))

                val hint = if (recordingState == RecordingState.VIEWING_SAVED_AUDIO) {
                    ""
                } else {
                    "Start recording to generate a transcript"
                }
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                if ((transcriptSegments.isNotEmpty() || currentPartialText.isNotBlank()) && onCopyTranscript != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Transcript",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            val fullText = buildString {
                                append(transcriptSegments.joinToString(" ") { it.text })
                                if (currentPartialText.isNotBlank()) {
                                    append(" $currentPartialText")
                                }
                            }.trim()
                            coroutineScope.launch {
                                val clipData = ClipData.newPlainText("Transcript", fullText)
                                clipboard.setClipEntry(clipData.toClipEntry())
                            }
                            onCopyTranscript.invoke()
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy transcript",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    item(key = "main") {
                        Column {
                            if (transcriptSegments.isNotEmpty()) {
                                val joined = transcriptSegments.joinToString(" ") { it.text }
                                Text(
                                    text = joined,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            if (currentPartialText.isNotBlank()) {
                                CurrentLiveText(
                                    text = currentPartialText,
                                    isTranscribing = isTranscribing,
                                    modifier = Modifier
                                        .padding(top = if (transcriptSegments.isEmpty()) 0.dp else 6.dp)
                                )
                            }

                            Spacer(Modifier.height(100.dp))
                        }
                    }
                }

                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    errorMessage?.let { err ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentLiveText(
    text: String,
    isTranscribing: Boolean,
    modifier: Modifier = Modifier
) {
    val words = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.20f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isTranscribing) {
            PulsingDotRow()
            Spacer(Modifier.width(12.dp))
        }

        FlowRow(
            modifier = Modifier.weight(1f),
        ) {
            words.forEachIndexed { index, word ->
                val isRecent = index >= words.size - 2
                val isCurrent = index == words.lastIndex

                val alpha = if (isRecent) 1.00f else 0.60f
                val weight = when {
                    isCurrent -> FontWeight.Bold
                    isRecent -> FontWeight.Medium
                    else -> FontWeight.Normal
                }
                val color = if (isCurrent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)

                Text(
                    text = word,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = weight,
                        fontStyle = if (isRecent) FontStyle.Italic else FontStyle.Normal
                    ),
                    color = color
                )

                if (index < words.lastIndex) {
                    Text(" ", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun PulsingDotRow() {
    Row {
        PulsingDot(0)
        Spacer(Modifier.width(8.dp))
        PulsingDot(140)
        Spacer(Modifier.width(8.dp))
        PulsingDot(280)
    }
}

@Composable
private fun PulsingDot(delay: Int) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        while (true) {
            visible = true
            kotlinx.coroutines.delay(420)
            visible = false
            kotlinx.coroutines.delay(420)
        }
    }

    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(
                if (visible) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            )
    )
}

@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        var y = 0
        var rowWidth = 0
        val heights = mutableListOf<Int>()

        placeables.forEach { p ->
            if (rowWidth + p.width > constraints.maxWidth && rowWidth > 0) {
                y += heights.lastOrNull() ?: p.height
                rowWidth = 0
            }
            rowWidth += p.width
            heights.add(p.height)
        }

        layout(constraints.maxWidth, y + (heights.lastOrNull() ?: 0)) {
            var x = 0
            var cy = 0
            var ri = 0
            placeables.forEach { p ->
                if (x + p.width > constraints.maxWidth && x > 0) {
                    x = 0
                    cy += heights.getOrElse(ri) { p.height }
                    ri++
                }
                p.placeRelative(x, cy)
                x += p.width
            }
        }
    }
}