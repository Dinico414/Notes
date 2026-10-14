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
import androidx.compose.material.icons.rounded.NewReleases
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xenonware.notes.util.audio.WhisperLib
import com.xenonware.notes.util.audio.WhisperModelManager
import com.xenonware.notes.util.audio.WhisperModelType
import kotlinx.coroutines.launch

/**
 * Settings section for managing local Whisper models.
 *
 * Drop-in replacement for [OpenAiApiKeySection].
 *
 * Model files are version-stamped, so bumping `whisper` in `libs.versions.toml`
 * makes existing models show up as "Update available" rather than silently
 * continuing to serve weights from an older build.
 */
@Composable
fun WhisperModelSection(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val modelManager = remember { WhisperModelManager(context) }
    val scope = rememberCoroutineScope()

    // Bumped whenever a model is written or deleted, to re-read the on-disk state.
    var refreshKey by remember { mutableIntStateOf(0) }

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
                    refreshKey = refreshKey,
                    onDownload = { type, onProgress ->
                        scope.launch {
                            modelManager.downloadModel(type, onProgress)
                            refreshKey++
                        }
                    },
                    onDelete = { type ->
                        modelManager.deleteModel(type)
                        refreshKey++
                    },
                )
                if (modelType != WhisperModelType.entries.last()) {
                    Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.height(12.dp))
            SystemInfoFooter(modelManager = modelManager, refreshKey = refreshKey)
        }
    }
}

/**
 * Shows which whisper.cpp build is actually linked into the app plus the version of
 * each model currently on disk, so a version bump is verifiable at a glance.
 */
@Composable
private fun SystemInfoFooter(
    modelManager: WhisperModelManager,
    refreshKey: Int,
) {
    // Reading this from the compiled .so proves which source was linked, unlike a
    // constant that could be stale.
    val nativeVersion = remember(refreshKey) {
        runCatching { WhisperLib.nativeVersion() }.getOrDefault("unavailable")
    }
    val expected = WhisperModelManager.WHISPER_VERSION

    val modelSummary = remember(refreshKey, nativeVersion) {
        WhisperModelType.entries.joinToString(" · ") { type ->
            val installed = if (modelManager.isModelDownloaded(type)) {
                modelManager.installedVersion(type) ?: expected
            } else {
                null
            }
            when {
                installed == null && modelManager.isModelOutdated(type) -> "${type.displayName} outdated"
                installed == null -> "${type.displayName} —"
                installed == expected -> "${type.displayName} $installed"
                else -> "${type.displayName} $installed (update)"
            }
        }
    }

    Text(
        text = "Native whisper.cpp $nativeVersion · $modelSummary",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ModelRow(
    modelType: WhisperModelType,
    modelManager: WhisperModelManager,
    refreshKey: Int,
    onDownload: (WhisperModelType, (Float) -> Unit) -> Unit,
    onDelete: (WhisperModelType) -> Unit,
) {
    // Re-derive state from disk whenever refreshKey changes.
    val isDownloaded = remember(refreshKey) { modelManager.isModelDownloaded(modelType) }
    val isOutdated = remember(refreshKey) { modelManager.isModelOutdated(modelType) }
    val installedVersion = remember(refreshKey) { modelManager.installedVersion(modelType) }

    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    val label = when (modelType) {
        WhisperModelType.TINY -> "Base"
        WhisperModelType.BASE -> "Pro (better quality)"
    }

    val sizeLabel = remember { modelType.sizeLabel }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when {
            isDownloaded -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            isOutdated -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
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
                        text = when {
                            isDownloaded -> "${modelType.displayName} · v$installedVersion"
                            isOutdated -> "${modelType.displayName} · v$installedVersion installed · $sizeLabel"
                            else -> modelType.displayName
                        },
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
                            onClick = { onDelete(modelType) },
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
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            // An outdated model gets the "new release" glyph, so the
                            // prompt reads as an update rather than a fresh install.
                            imageVector = if (isOutdated) {
                                Icons.Rounded.NewReleases
                            } else {
                                Icons.Rounded.Download
                            },
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(16.dp),
                        )
                        Text(if (isOutdated) "Update" else "Download")
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

            // Explain an update before the user taps it, especially for the ~142 MB
            // Pro model where re-downloading costs real mobile data.
            AnimatedVisibility(visible = isOutdated && !isDownloading && !isDownloaded) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (modelType.isBundled) {
                            "A newer model is available for whisper.cpp " +
                                "${WhisperModelManager.WHISPER_VERSION} and will be extracted on update."
                        } else {
                            "A newer model is available for whisper.cpp " +
                                "${WhisperModelManager.WHISPER_VERSION}. Updating re-downloads " +
                                "$sizeLabel."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
