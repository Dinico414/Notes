package com.xenonware.notes.util.audio

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class SerializableTranscriptSegment(
    val text: String,
    val timestampMillis: Long,
    val confidence: Float = 1.0f
)
private fun TranscriptSegment.toSerializable() = SerializableTranscriptSegment(
    text = text,
    timestampMillis = timestampMillis,
    confidence = confidence
)
private fun SerializableTranscriptSegment.toTranscriptSegment() = TranscriptSegment(
    text = text,
    timestampMillis = timestampMillis,
    confidence = confidence
)
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