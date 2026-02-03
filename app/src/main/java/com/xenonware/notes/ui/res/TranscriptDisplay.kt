package com.xenonware.notes.ui.res

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenonware.notes.util.TranscriptSegment

/**
 * Displays the transcript of recorded audio with timestamps.
 * Shows real-time transcription during recording and saved transcripts during playback.
 */
@Composable
fun TranscriptDisplay(
    modifier: Modifier = Modifier,
    transcriptSegments: List<TranscriptSegment> = emptyList(),
    isRecording: Boolean = false,
    isTranscribing: Boolean = false,
    currentPartialText: String = "",
    errorMessage: String? = null,
    onCopyTranscript: (() -> Unit)? = null
) {
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current

    // Auto-scroll to bottom when new content arrives
    LaunchedEffect(transcriptSegments.size, currentPartialText) {
        if (transcriptSegments.isNotEmpty() || currentPartialText.isNotEmpty()) {
            listState.animateScrollToItem(
                maxOf(0, transcriptSegments.size + if (currentPartialText.isNotEmpty()) 1 else 0)
            )
        }
    }

    Box(modifier = modifier) {
        if (transcriptSegments.isEmpty() && currentPartialText.isEmpty() && !isRecording) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MicOff,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = colorScheme.onSurface.copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "No transcript available",
                    style = typography.bodyLarge,
                    color = colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Start recording to generate a transcript",
                    style = typography.bodySmall,
                    color = colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with copy button
                if (transcriptSegments.isNotEmpty() && onCopyTranscript != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Transcript",
                            style = typography.titleSmall,
                            color = colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val fullText = transcriptSegments.joinToString("\n") { it.text }
                                clipboardManager.setText(AnnotatedString(fullText))
                                onCopyTranscript()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy transcript",
                                tint = colorScheme.primary
                            )
                        }
                    }
                }

                // Transcript content
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(transcriptSegments) { segment ->
                        TranscriptSegmentItem(
                            segment = segment,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    // Current partial/in-progress text
                    if (currentPartialText.isNotEmpty()) {
                        item {
                            PartialTranscriptItem(
                                text = currentPartialText,
                                isTranscribing = isTranscribing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Bottom spacing
                    item {
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // Error message
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    errorMessage?.let {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colorScheme.errorContainer)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = it,
                                style = typography.bodySmall,
                                color = colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TranscriptSegmentItem(
    segment: TranscriptSegment,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top
    ) {
        // Timestamp
        Text(
            text = formatTranscriptTime(segment.timestampMillis),
            style = typography.bodySmall,
            color = colorScheme.onSurface.copy(alpha = 0.5f),
            fontFamily = QuicksandTitleVariable,
            modifier = Modifier.width(56.dp)
        )

        Spacer(Modifier.width(12.dp))

        // Text content
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(colorScheme.surfaceContainerHighest)
                .padding(12.dp)
        ) {
            Text(
                text = segment.text,
                style = typography.bodyMedium,
                color = colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PartialTranscriptItem(
    text: String,
    isTranscribing: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top
    ) {
        // Pulsing indicator
        Box(
            modifier = Modifier.width(56.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isTranscribing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    PulsingDot(delayMillis = 0)
                    Spacer(Modifier.width(4.dp))
                    PulsingDot(delayMillis = 150)
                    Spacer(Modifier.width(4.dp))
                    PulsingDot(delayMillis = 300)
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        // Partial text with different styling
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(colorScheme.primaryContainer.copy(alpha = 0.3f))
                .padding(12.dp)
        ) {
            Text(
                text = text,
                style = typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun PulsingDot(delayMillis: Int = 0) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMillis.toLong())
        while (true) {
            isVisible = true
            kotlinx.coroutines.delay(400)
            isVisible = false
            kotlinx.coroutines.delay(400)
        }
    }

    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(
                if (isVisible) colorScheme.primary else colorScheme.primary.copy(alpha = 0.3f)
            )
    )
}

/**
 * Formats milliseconds into MM:SS format for transcript timestamps
 */
private fun formatTranscriptTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}