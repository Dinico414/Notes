package com.xenonware.notes.data

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xenonware.notes.util.audio.WhisperModelManager
import com.xenonware.notes.util.audio.WhisperModelType
import kotlinx.coroutines.launch

/**
 * Settings section for managing local Whisper models.
 *
 * Drop-in replacement for [OpenAiApiKeySection].
 */
@Composable
fun WhisperModelSection(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val modelManager = remember { WhisperModelManager(context) }
    val scope = rememberCoroutineScope()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize()
        ) {
            // ── Header ──────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Rounded.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Whisper Models",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Download a model for local audio transcription. One model handles all languages.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            // ── Model rows ──────────────────────────────────────────────
            WhisperModelType.entries.forEach { modelType ->
                ModelRow(
                    modelType = modelType,
                    modelManager = modelManager,
                    onDownload = { type, onProgress ->
                        scope.launch {
                            modelManager.downloadModel(type, onProgress)
                        }
                    },
                    onDelete = { type ->
                        modelManager.deleteModel(type)
                    },
                )
                if (modelType != WhisperModelType.entries.last()) {
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ModelRow(
    modelType: WhisperModelType,
    modelManager: WhisperModelManager,
    onDownload: (WhisperModelType, (Float) -> Unit) -> Unit,
    onDelete: (WhisperModelType) -> Unit,
) {
    var isDownloaded by remember { mutableStateOf(modelManager.isModelDownloaded(modelType)) }
    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    val label = when (modelType) {
        WhisperModelType.TINY -> "Tiny"
        WhisperModelType.BASE -> "Base (better quality)"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDownloaded)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else
            MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = modelType.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (isDownloaded) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "Ready",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        IconButton(
                            onClick = {
                                onDelete(modelType)
                                isDownloaded = false
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Delete model",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                } else if (isDownloading) {
                    IconButton(
                        onClick = { /* cancellation could be added later */ },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Cancel",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                } else {
                    FilledTonalButton(
                        onClick = {
                            isDownloading = true
                            progress = 0f
                            onDownload(modelType) { p ->
                                progress = p
                                if (p >= 1f) {
                                    isDownloading = false
                                    isDownloaded = modelManager.isModelDownloaded(modelType)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Download,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(16.dp),
                        )
                        Text("Download")
                    }
                }
            }

            // Progress bar
            AnimatedVisibility(visible = isDownloading) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}