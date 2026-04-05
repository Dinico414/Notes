package com.xenonware.notes.util.audio

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Available ggml model sizes.
 * Both are multilingual — one model handles English, German, and 90+ other languages.
 */
enum class WhisperModelType(
    val fileName: String,
    val displayName: String,
    val downloadUrl: String,
    val approximateBytes: Long,
) {
    TINY(
        fileName = "ggml-tiny.bin",
        displayName = "Base",
        downloadUrl = "",                       // bundled in assets — never downloaded
        approximateBytes = 75_000_000L,
    ),
    BASE(
        fileName = "ggml-base.bin",
        displayName = "Pro",
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin",
        approximateBytes = 142_000_000L,
    ),
}

/**
 * Manages whisper.cpp model files.
 *
 * • **Tiny** is shipped inside the APK (`assets/ggml-tiny.bin`) and extracted
 *   to internal storage on first use.
 * • **Base** is an optional upgrade the user can download at runtime.
 */
class WhisperModelManager(private val context: Context) {

    companion object {
        private const val TAG = "WhisperModelMgr"
        private const val MODELS_DIR = "whisper_models"
        private const val ASSET_TINY = "ggml-tiny.bin"
    }

    private val modelsDir: File =
        File(context.filesDir, MODELS_DIR).apply { mkdirs() }

    fun getModelFile(type: WhisperModelType): File =
        File(modelsDir, type.fileName)

    fun isModelDownloaded(type: WhisperModelType): Boolean =
        getModelFile(type).let { it.exists() && it.length() > 0 }

    /**
     * Unified entry point to "get" a model.
     * For TINY, it extracts from assets. For BASE, it downloads from the web.
     */
    suspend fun downloadModel(
        type: WhisperModelType,
        onProgress: (Float) -> Unit
    ): Boolean = when (type) {
        WhisperModelType.TINY -> {
            onProgress(0f)
            val success = ensureTinyModelExtracted()
            onProgress(1f)
            success
        }
        WhisperModelType.BASE -> {
            downloadBaseModel(onProgress)
        }
    }

    // ── Tiny: extract from assets on first launch ───────────────────────────

    /**
     * Copies `ggml-tiny.bin` from APK assets into internal storage if needed.
     * Call this early (e.g. on sheet open). Fast no-op when already extracted.
     */
    suspend fun ensureTinyModelExtracted(): Boolean {
        val tinyFile = getModelFile(WhisperModelType.TINY)
        if (tinyFile.exists() && tinyFile.length() > 0) return true

        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Extracting tiny model from assets …")
                context.assets.open(ASSET_TINY).use { input ->
                    tinyFile.outputStream().buffered().use { output ->
                        input.copyTo(output, bufferSize = 8192)
                    }
                }
                Log.i(TAG, "Tiny model extracted (${tinyFile.length()} bytes)")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to extract tiny model from assets", e)
                tinyFile.delete()
                false
            }
        }
    }

    // ── Base: optional download from HuggingFace ────────────────────────────

    /**
     * Download the Base model.  Reports 0 f → 1 f via [onProgress].
     * Returns `true` on success.
     */
    suspend fun downloadBaseModel(
        onProgress: (Float) -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        val type = WhisperModelType.BASE
        val targetFile = getModelFile(type)
        val tempFile = File(modelsDir, "${type.fileName}.tmp")

        try {
            Log.i(TAG, "Starting Base model download")

            val connection =
                (java.net.URL(type.downloadUrl).openConnection() as java.net.HttpURLConnection)
                    .apply {
                        connectTimeout = 30_000
                        readTimeout = 120_000
                        instanceFollowRedirects = true
                    }
            connection.connect()

            if (connection.responseCode != java.net.HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "HTTP ${connection.responseCode}")
                return@withContext false
            }

            val totalBytes = connection.contentLengthLong
                .takeIf { it > 0 } ?: type.approximateBytes
            var downloaded = 0L

            connection.inputStream.buffered().use { input ->
                tempFile.outputStream().buffered().use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress((downloaded.toFloat() / totalBytes).coerceIn(0f, 1f))
                    }
                }
            }

            if (targetFile.exists()) targetFile.delete()
            val renamed = tempFile.renameTo(targetFile)
            Log.i(TAG, "Base model ready (renamed=$renamed)")
            renamed
        } catch (e: Exception) {
            Log.e(TAG, "Base model download failed", e)
            tempFile.delete()
            false
        }
    }

    fun deleteModel(type: WhisperModelType) {
        getModelFile(type).delete()
    }
}