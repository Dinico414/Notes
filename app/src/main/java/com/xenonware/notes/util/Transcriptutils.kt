package com.xenonware.notes.ui.res

import android.content.Context
import com.xenonware.notes.util.TranscriptSegment
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Serializable version of TranscriptSegment for storage
 */
@Serializable
data class SerializableTranscriptSegment(
    val text: String,
    val timestampMillis: Long,
    val confidence: Float = 1.0f
)

/**
 * Converts TranscriptSegment to serializable version
 */
private fun TranscriptSegment.toSerializable() = SerializableTranscriptSegment(
    text = text,
    timestampMillis = timestampMillis,
    confidence = confidence
)

/**
 * Converts serializable version back to TranscriptSegment
 */
private fun SerializableTranscriptSegment.toTranscriptSegment() = TranscriptSegment(
    text = text,
    timestampMillis = timestampMillis,
    confidence = confidence
)

/**
 * Save transcript segments to a JSON file
 * File is named: {audioId}_transcript.json
 */
fun saveTranscript(
    context: Context,
    audioId: String?,
    segments: List<TranscriptSegment>
) {
    if (audioId == null || segments.isEmpty()) return

    try {
        val serializableSegments = segments.map { it.toSerializable() }
        val json = Json { prettyPrint = true }
        val jsonString = json.encodeToString(serializableSegments)

        val file = File(context.filesDir, "${audioId}_transcript.json")
        file.writeText(jsonString)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Load transcript segments from a JSON file
 * Returns empty list if file doesn't exist or on error
 */
fun loadTranscript(
    context: Context,
    audioId: String?
): List<TranscriptSegment> {
    if (audioId == null) return emptyList()

    return try {
        val file = File(context.filesDir, "${audioId}_transcript.json")
        if (!file.exists()) return emptyList()

        val jsonString = file.readText()
        val json = Json { ignoreUnknownKeys = true }
        val serializableSegments = json.decodeFromString<List<SerializableTranscriptSegment>>(jsonString)

        serializableSegments.map { it.toTranscriptSegment() }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

/**
 * Delete transcript file for given audio ID
 */
fun deleteTranscript(
    context: Context,
    audioId: String?
) {
    if (audioId == null) return

    try {
        val file = File(context.filesDir, "${audioId}_transcript.json")
        if (file.exists()) {
            file.delete()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Export transcript as plain text
 */
fun exportTranscriptAsText(segments: List<TranscriptSegment>): String {
    return segments.joinToString("\n\n") { segment ->
        val timestamp = formatTranscriptTimestamp(segment.timestampMillis)
        "[$timestamp] ${segment.text}"
    }
}

/**
 * Format timestamp for export (e.g., "01:23")
 */
private fun formatTranscriptTimestamp(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}