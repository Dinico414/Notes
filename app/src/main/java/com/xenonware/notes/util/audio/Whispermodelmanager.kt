package com.xenonware.notes.util.audio

import android.content.Context
import android.util.Log
import com.xenonware.notes.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Base URL of the upstream ggml model repository.
 *
 * Single place to change if the models ever move hosts.
 */
private const val UPSTREAM_BASE_URL =
    "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/"

/**
 * Available ggml model sizes.
 * Both are multilingual — one model handles English, German, and 90+ other languages.
 *
 * Model files are **version-stamped** with the whisper.cpp version from
 * `libs.versions.toml` (via [BuildConfig.WHISPER_CPP_VERSION]), e.g. `ggml-tiny-1.9.5.bin`.
 * A version bump therefore makes every existing file automatically stale, and the
 * manager re-extracts / re-downloads it without any migration code.
 */
enum class WhisperModelType(
    /** Prefix of the on-disk file name, before the version stamp. */
    val baseName: String,
    val displayName: String,
    /**
     * Upstream file name, as served by HuggingFace.
     *
     * Deliberately NOT version-stamped: upstream publishes a single `ggml-base.bin`
     * that does not change per whisper.cpp release. The version stamp exists only on
     * the *local* copy (see [fileName]) so staleness can be detected.
     */
    val upstreamFileName: String,
    /** True when this model ships inside the APK and is never downloaded. */
    val bundledInApk: Boolean,
    val approximateBytes: Long,
) {
    TINY(
        baseName = "ggml-tiny",
        displayName = "Base",
        upstreamFileName = "ggml-tiny.bin",
        bundledInApk = true,
        // Measured from the shipped asset (77,691,713 bytes).
        approximateBytes = 77_700_000L,
    ),
    BASE(
        baseName = "ggml-base",
        displayName = "Pro",
        upstreamFileName = "ggml-base.bin",
        bundledInApk = false,
        // Verified via HTTP HEAD: 147,951,465 bytes.
        approximateBytes = 148_000_000L,
    );

    /** Version-stamped on-disk file name, e.g. `ggml-tiny-1.9.5.bin`. */
    fun fileName(version: String): String = "$baseName-$version.bin"

    /**
     * Download URL for this model, or empty when it ships inside the APK.
     *
     * Note there is no `version` parameter: upstream serves one file name for all
     * whisper.cpp releases, so the version must not leak into the URL. The local copy
     * gets the version stamp via [fileName] instead.
     */
    val downloadUrl: String
        get() = if (bundledInApk) "" else "$UPSTREAM_BASE_URL$upstreamFileName"

    /** True when this model is bundled in the APK rather than downloaded. */
    val isBundled: Boolean get() = bundledInApk

    /**
     * Human-readable download size, e.g. `~148 MB`, derived from [approximateBytes].
     *
     * Single source of truth for every UI string that mentions the model size, so the
     * figure is never duplicated (and never drifts) across screens.
     */
    val sizeLabel: String get() = "~%d MB".format(approximateBytes / 1_000_000)
}

/**
 * Manages whisper.cpp model files.
 *
 * • **Tiny** is shipped inside the APK and extracted to internal storage on first use.
 * • **Base** is an optional upgrade the user can download at runtime.
 *
 * Every file name embeds the whisper.cpp version, so bumping the version in
 * `gradle/libs.versions.toml` invalidates existing models. [isModelOutdated] reports
 * that to the UI; [pruneOldVersions] removes the superseded file once the new one
 * is safely in place.
 */
class WhisperModelManager(private val context: Context) {

    companion object {
        private const val TAG = "WhisperModelMgr"
        private const val MODELS_DIR = "whisper_models"

        /** whisper.cpp version this build expects. Sourced from libs.versions.toml. */
        val WHISPER_VERSION: String = BuildConfig.WHISPER_CPP_VERSION

        /** Asset name of the bundled tiny model, stamped for the current version. */
        @JvmStatic
        fun assetNameFor(type: WhisperModelType, version: String = WHISPER_VERSION): String =
            type.fileName(version)
    }

    private val modelsDir: File =
        File(context.filesDir, MODELS_DIR).apply { mkdirs() }

    /** The file this build expects for [type]. */
    fun getModelFile(type: WhisperModelType): File =
        File(modelsDir, type.fileName(WHISPER_VERSION))

    // ── Version-aware helpers ───────────────────────────────────────────────

    /**
     * All installed files for [type], regardless of version, newest-looking first.
     * Used both for stale detection and for cleanup.
     */
    fun installedVersions(type: WhisperModelType): List<File> =
        modelsDir.listFiles { f ->
            f.isFile &&
                f.name.startsWith("${type.baseName}-") &&
                f.name.endsWith(".bin")
        }?.toList().orEmpty()

    /**
     * A model is outdated when *some* version of it is installed, but not the one
     * this build expects. This is what drives the "Update available" affordance.
     */
    fun isModelOutdated(type: WhisperModelType): Boolean =
        !isModelDownloaded(type) && installedVersions(type).isNotEmpty()

    /** Version string of the currently installed file for [type], if any. */
    fun installedVersion(type: WhisperModelType): String? =
        installedVersions(type).firstOrNull()?.name
            ?.substringAfter("${type.baseName}-")
            ?.substringBefore(".bin")

    fun isModelDownloaded(type: WhisperModelType): Boolean =
        getModelFile(type).let { it.exists() && it.length() > 0 }

    /**
     * Deletes superseded versions of [type], keeping only the current one.
     * Called **after** the new file is in place so a failed refresh never leaves
     * the user without a usable model.
     */
    fun pruneOldVersions(type: WhisperModelType) {
        val keep = getModelFile(type).name
        installedVersions(type)
            .filter { it.name != keep }
            .forEach { stale ->
                Log.i(TAG, "Pruning stale model ${stale.name}")
                stale.delete()
            }
    }

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
     * Copies the bundled tiny model from APK assets into internal storage.
     *
     * Re-runs automatically when the version stamp changed, which is free because
     * the asset ships inside the APK. Fast no-op when already current.
     */
    suspend fun ensureTinyModelExtracted(): Boolean {
        val tinyFile = getModelFile(WhisperModelType.TINY)
        if (tinyFile.exists() && tinyFile.length() > 0) return true

        return withContext(Dispatchers.IO) {
            val assetName = assetNameFor(WhisperModelType.TINY)
            try {
                Log.i(TAG, "Extracting tiny model from assets ($assetName) …")

                // Write to a temp file first so an interrupted copy never leaves a
                // truncated model that isModelDownloaded() would wrongly accept.
                val tempFile = File(modelsDir, "$assetName.tmp")
                context.assets.open(assetName).use { input ->
                    tempFile.outputStream().buffered().use { output ->
                        input.copyTo(output, bufferSize = 8192)
                    }
                }

                if (tinyFile.exists()) tinyFile.delete()
                val renamed = tempFile.renameTo(tinyFile)
                if (!renamed) {
                    tempFile.delete()
                    Log.e(TAG, "Failed to move extracted tiny model into place")
                    return@withContext false
                }

                Log.i(TAG, "Tiny model extracted (${tinyFile.length()} bytes)")
                pruneOldVersions(WhisperModelType.TINY)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to extract tiny model '$assetName' from assets", e)
                File(modelsDir, "$assetName.tmp").delete()
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
        val tempFile = File(modelsDir, "${targetFile.name}.tmp")
        val url = type.downloadUrl

        try {
            Log.i(TAG, "Starting Base model download for whisper $WHISPER_VERSION: $url")

            val connection =
                (java.net.URL(url).openConnection() as java.net.HttpURLConnection)
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

            // Only discard the previous version once the new one is usable.
            if (renamed) pruneOldVersions(type)
            renamed
        } catch (e: Exception) {
            Log.e(TAG, "Base model download failed", e)
            tempFile.delete()
            false
        }
    }

    fun deleteModel(type: WhisperModelType) {
        // Remove every version, including stale ones left behind by a bump.
        installedVersions(type).forEach { it.delete() }
        getModelFile(type).delete()
    }
}
