package com.xenonware.notes.util.audio

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

data class TranscriptSegment(
    val text: String,
    val timestampMillis: Long,
    val confidence: Float = 1.0f,
)

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

private val lenientJson = Json {
    ignoreUnknownKeys = true
    isLenient = true          // tolerates unquoted strings from very old builds
    coerceInputValues = true  // fills in missing confidence with default
}

fun saveTranscript(
    context: Context,
    audioId: String?,
    segments: List<TranscriptSegment>
) {
    if (audioId == null || segments.isEmpty()) return
    try {
        val jsonString = Json { prettyPrint = true }
            .encodeToString(segments.map { it.toSerializable() })
        File(context.filesDir, "${audioId}_transcript.json").writeText(jsonString)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun loadTranscript(
    context: Context,
    audioId: String?
): List<TranscriptSegment> {
    if (audioId == null) return emptyList()

    val file = File(context.filesDir, "${audioId}_transcript.json")
    if (!file.exists()) return emptyList()

    val jsonString = try { file.readText() } catch (e: Exception) { return emptyList() }

    // 1. Try the current format: JSON array of SerializableTranscriptSegment
    tryParse<List<SerializableTranscriptSegment>>(jsonString) { raw ->
        lenientJson.decodeFromString(raw)
    }?.let { return it.map { s -> s.toTranscriptSegment() } }

    // 2. Fallback: very old apps saved a single object instead of an array
    tryParse<SerializableTranscriptSegment>(jsonString) { raw ->
        lenientJson.decodeFromString(raw)
    }?.let { return listOf(it.toTranscriptSegment()) }

    return emptyList()
}

private inline fun <reified T> tryParse(
    json: String,
    block: (String) -> T
): T? = try { block(json) } catch (_: Exception) { null }