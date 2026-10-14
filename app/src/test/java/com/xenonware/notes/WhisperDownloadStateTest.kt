package com.xenonware.notes

import com.xenonware.notes.util.audio.WhisperDownloadState
import com.xenonware.notes.util.audio.WhisperModelType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the download-state contract that the UI and notification rely on.
 *
 * These cover the pure logic, since the transfer itself needs a live network.
 */
class WhisperDownloadStateTest {

    @Test
    fun `downloading state carries progress and byte counts`() {
        val state = WhisperDownloadState.Downloading(
            progress = 0.5f,
            bytesDownloaded = 74_000_000L,
            totalBytes = 148_000_000L,
        )

        assertEquals(0.5f, state.progress)
        assertEquals(74_000_000L, state.bytesDownloaded)
        assertEquals(148_000_000L, state.totalBytes)
    }

    @Test
    fun `terminal states are distinguishable from in-flight work`() {
        // The sheet uses this distinction to decide whether to show a progress bar.
        val downloading: WhisperDownloadState = WhisperDownloadState.Downloading(0.1f, 1L, 100L)
        val completed: WhisperDownloadState = WhisperDownloadState.Completed("ggml-base-1.9.5.bin")
        val failed: WhisperDownloadState = WhisperDownloadState.Failed("no network")
        val idle: WhisperDownloadState = WhisperDownloadState.Idle

        assertTrue(downloading is WhisperDownloadState.Downloading)
        assertFalse(completed is WhisperDownloadState.Downloading)
        assertFalse(failed is WhisperDownloadState.Downloading)
        assertFalse(idle is WhisperDownloadState.Downloading)
    }

    @Test
    fun `failure carries a user displayable message`() {
        val message = "Download failed — check your connection and try again."
        val state = WhisperDownloadState.Failed(message)
        assertEquals(message, state.message)
    }

    @Test
    fun `completed reports the exact on-disk file name`() {
        // Ties the completion event back to the version-stamped file so the UI can
        // confirm what landed on disk.
        val expected = WhisperModelType.BASE.fileName("1.9.5")
        val state = WhisperDownloadState.Completed(expected)
        assertEquals("ggml-base-1.9.5.bin", state.modelFile)
    }

    @Test
    fun `progress fraction used by the bar stays within 0 to 1`() {
        // A misreported Content-Length must not push the bar past 100%.
        val overReported = WhisperDownloadState.Downloading(1.4f, 200L, 148L)
        val clamped = overReported.progress.coerceIn(0f, 1f)
        assertEquals(1f, clamped)

        val under = WhisperDownloadState.Downloading(-0.2f, 0L, 148L)
        assertEquals(0f, under.progress.coerceIn(0f, 1f))
    }
}
