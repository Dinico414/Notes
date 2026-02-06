package com.xenonware.notes.ui.res

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xenonware.notes.util.RecordingState
import java.io.File
import java.io.IOException


@Composable
fun WaveformDisplay(
    modifier: Modifier = Modifier,
    amplitudes: List<Float>,
    isRecording: Boolean,
    progress: Float,
    recordingState: RecordingState
) {
    if (amplitudes.isEmpty()) {
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
                text = "No waveform available",
                style = typography.bodyLarge,
                color = colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))

            val text = if (recordingState == RecordingState.VIEWING_SAVED_AUDIO) {
                ""
            } else {
                "Start recording to generate a waveform"
            }
            Text(
                text = text,
                style = typography.bodySmall,
                color = colorScheme.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }

    val color = colorScheme.primary
    val inactiveColor = colorScheme.onSurface.copy(alpha = 0.2f)
    val maxAmplitudeValue = 32767f

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val middleY = canvasHeight / 2

        if (isRecording) {
            val barWidth = 3.dp.toPx()
            val spacing = 2.dp.toPx()
            val totalBarWidth = barWidth + spacing
            val maxBars = (canvasWidth / totalBarWidth).toInt()
            val startIndex = (amplitudes.size - maxBars).coerceAtLeast(0)
            val barsToDraw = amplitudes.subList(startIndex, amplitudes.size)

            barsToDraw.forEachIndexed { index, amplitude ->
                val x = index * totalBarWidth
                val normalized = (amplitude / maxAmplitudeValue).coerceIn(0f, 1f)
                val barHeight = normalized * canvasHeight * 0.8f

                if (barHeight > 0) {
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x = x, y = middleY - barHeight / 2),
                        size = Size(barWidth, barHeight.coerceAtLeast(2.dp.toPx())),
                        cornerRadius = CornerRadius(barWidth / 2)
                    )
                }
            }
        } else {
            val maxBars = (canvasWidth / (3.dp.toPx() + 2.dp.toPx())).toInt()
            val barsToDraw = if (amplitudes.size > maxBars) {
                val groupSize = amplitudes.size.toFloat() / maxBars
                (0 until maxBars).map { i ->
                    val start = (i * groupSize).toInt()
                    val end = ((i + 1) * groupSize).toInt().coerceAtMost(amplitudes.size)
                    amplitudes.subList(start, end).average().toFloat()
                }
            } else {
                amplitudes
            }

            val totalBarWidth = canvasWidth / barsToDraw.size
            val barWidth = (totalBarWidth * 0.6f).coerceAtLeast(1.dp.toPx())
            val progressX = progress * canvasWidth

            barsToDraw.forEachIndexed { index, amplitude ->
                val x = index * totalBarWidth
                val normalized = (amplitude / maxAmplitudeValue).coerceIn(0f, 1f)
                val barHeight = normalized * canvasHeight * 0.8f
                val barColor = if (x < progressX) color else inactiveColor

                if (barHeight > 0) {
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x = x, y = middleY - barHeight / 2),
                        size = Size(barWidth, barHeight.coerceAtLeast(2.dp.toPx())),
                        cornerRadius = CornerRadius(barWidth / 2)
                    )
                }
            }
        }
    }
}

fun saveAmplitudes(context: Context, uniqueId: String, amplitudes: List<Float>) {
    val file = File(context.filesDir, "$uniqueId.amp")
    try {
        file.bufferedWriter().use { out ->
            out.write(amplitudes.joinToString(","))
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

fun loadAmplitudes(context: Context, uniqueId: String): List<Float> {
    val file = File(context.filesDir, "$uniqueId.amp")
    if (!file.exists()) return emptyList()
    return try {
        file.bufferedReader().use { it.readText() }
            .split(',')
            .mapNotNull { it.toFloatOrNull() }
    } catch (e: IOException) {
        e.printStackTrace()
        emptyList()
    }
}
