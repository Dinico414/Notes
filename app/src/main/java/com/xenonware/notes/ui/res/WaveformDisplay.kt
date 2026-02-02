package com.xenonware.notes.ui.res

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
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
        Box(modifier, Alignment.Center) {
            val text = if (recordingState == RecordingState.VIEWING_SAVED_AUDIO) {
                "No waveform available"
            } else {
                "Start recording..."
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val color = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
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
