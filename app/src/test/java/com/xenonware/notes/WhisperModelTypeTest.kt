package com.xenonware.notes

import com.xenonware.notes.util.audio.WhisperModelType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-unit tests for the version-stamping contract on [WhisperModelType].
 *
 * These guard the mechanism that makes a `libs.versions.toml` bump invalidate the
 * on-device models, without needing an Android device or Robolectric.
 */
class WhisperModelTypeTest {

    @Test
    fun `file names embed the version stamp`() {
        assertEquals("ggml-tiny-1.9.5.bin", WhisperModelType.TINY.fileName("1.9.5"))
        assertEquals("ggml-base-1.9.5.bin", WhisperModelType.BASE.fileName("1.9.5"))
    }

    @Test
    fun `a version bump produces a different file name`() {
        // This is what makes stale detection a pure filename comparison.
        assertTrue(
            WhisperModelType.BASE.fileName("1.9.5") !=
                WhisperModelType.BASE.fileName("1.8.4")
        )
    }

    @Test
    fun `file names are prefixed with base name so scanning can find them`() {
        WhisperModelType.entries.forEach { type ->
            val name = type.fileName("1.9.5")
            assertTrue(
                "expected '$name' to start with '${type.baseName}-'",
                name.startsWith("${type.baseName}-")
            )
        }
    }

    @Test
    fun `file names end with bin for asset and download consistency`() {
        WhisperModelType.entries.forEach { type ->
            assertTrue(type.fileName("1.9.5").endsWith(".bin"))
        }
    }

    @Test
    fun `only bundled models have an empty download url`() {
        // TINY ships in the APK; BASE is fetched at runtime.
        assertTrue(WhisperModelType.TINY.isBundled)
        assertEquals("", WhisperModelType.TINY.downloadUrl)

        assertFalse(WhisperModelType.BASE.isBundled)
        assertTrue(WhisperModelType.BASE.downloadUrl.startsWith("https://"))
    }

    @Test
    fun `download url points at the upstream file name, not the version`() {
        // REGRESSION GUARD. The URL previously interpolated the version, producing
        // ".../1.9.5.bin" and a HTTP 404. Upstream serves one file name regardless of
        // whisper.cpp version, so the version must never appear in the URL.
        val url = WhisperModelType.BASE.downloadUrl

        assertTrue(
            "expected url to end with the upstream file name but was '$url'",
            url.endsWith("/ggml-base.bin")
        )
        assertFalse(
            "version must not leak into the download url but was '$url'",
            url.contains("1.9.5")
        )
    }

    @Test
    fun `the on-disk name is version stamped while the url is not`() {
        // These are deliberately different concepts and conflating them was the bug.
        val onDisk = WhisperModelType.BASE.fileName("1.9.5")
        val remote = WhisperModelType.BASE.downloadUrl.substringAfterLast('/')

        assertEquals("ggml-base-1.9.5.bin", onDisk)
        assertEquals("ggml-base.bin", remote)
        assertTrue(onDisk != remote)
    }

    @Test
    fun `display names stay stable for the settings UI`() {
        assertEquals("Base", WhisperModelType.TINY.displayName)
        assertEquals("Pro", WhisperModelType.BASE.displayName)
    }

    @Test
    fun `size labels reflect the verified download sizes`() {
        // BASE was verified against the HuggingFace blob listing (148 MB, was a stale
        // 142 MB guess); TINY was measured from the shipped asset.
        assertEquals("~148 MB", WhisperModelType.BASE.sizeLabel)
        assertEquals("~77 MB", WhisperModelType.TINY.sizeLabel)
    }

    @Test
    fun `size label is derived from approximateBytes so it cannot drift`() {
        WhisperModelType.entries.forEach { type ->
            val expected = "~%d MB".format(type.approximateBytes / 1_000_000)
            assertEquals(expected, type.sizeLabel)
        }
    }

    @Test
    fun `installedVersion parsing round-trips the stamp`() {
        // Mirrors WhisperModelManager.installedVersion()'s substring logic so a
        // change to the naming scheme fails loudly here.
        val type = WhisperModelType.BASE
        val fileName = type.fileName("1.9.5")
        val parsed = fileName
            .substringAfter("${type.baseName}-")
            .substringBefore(".bin")
        assertEquals("1.9.5", parsed)
    }
}
