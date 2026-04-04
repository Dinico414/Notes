package com.xenonware.notes.util.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Decodes an audio file (MP3 / M4A / AAC / WAV / etc.) into the format
 * whisper.cpp expects: **16 kHz, mono, float32 PCM** in the range [-1, 1].
 *
 * Uses Android's [MediaExtractor] + [MediaCodec] — no external libraries needed.
 */
object AudioDecoder {

    private const val TAG = "AudioDecoder"
    private const val TARGET_SAMPLE_RATE = 16_000

    /**
     * @param filePath absolute path to the audio file
     * @return float array of 16 kHz mono PCM samples, or `null` on failure
     */
    fun decodeToFloat16kMono(filePath: String): FloatArray? {
        val extractor = MediaExtractor()

        try {
            extractor.setDataSource(filePath)

            // ── Find the first audio track ──────────────────────────────────
            var trackIndex = -1
            var trackFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    trackIndex = i
                    trackFormat = fmt
                    break
                }
            }

            if (trackIndex == -1 || trackFormat == null) {
                Log.e(TAG, "No audio track found in $filePath")
                return null
            }

            extractor.selectTrack(trackIndex)

            val mime       = trackFormat.getString(MediaFormat.KEY_MIME) ?: return null
            val sampleRate = trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels   = trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            Log.i(TAG, "Source: mime=$mime, rate=$sampleRate, ch=$channels")

            // ── Decode to raw PCM (16-bit signed) ───────────────────────────
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(trackFormat, null, null, 0)
            codec.start()

            val pcmBytes   = mutableListOf<ByteArray>()
            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone  = false
            var outputDone = false
            var totalBytes = 0

            while (!outputDone) {
                // Feed compressed data into the decoder
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(10_000)
                    if (inIdx >= 0) {
                        val inBuf = codec.getInputBuffer(inIdx) ?: continue
                        val sampleSize = extractor.readSampleData(inBuf, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(
                                inIdx, 0, sampleSize, extractor.sampleTime, 0
                            )
                            extractor.advance()
                        }
                    }
                }

                // Pull decoded PCM out
                val outIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                if (outIdx >= 0) {
                    if (bufferInfo.size > 0) {
                        val outBuf = codec.getOutputBuffer(outIdx) ?: continue
                        val chunk = ByteArray(bufferInfo.size)
                        outBuf.get(chunk)
                        pcmBytes.add(chunk)
                        totalBytes += chunk.size
                    }
                    codec.releaseOutputBuffer(outIdx, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputDone = true
                    }
                }
            }

            codec.stop()
            codec.release()
            extractor.release()

            Log.i(TAG, "Decoded $totalBytes bytes of raw PCM")

            // ── Assemble all chunks into a single byte array ────────────────
            val allBytes = ByteArray(totalBytes)
            var offset = 0
            for (chunk in pcmBytes) {
                System.arraycopy(chunk, 0, allBytes, 0 + offset, chunk.size)
                offset += chunk.size
            }

            // ── Convert bytes → Int16 samples ───────────────────────────────
            val shortBuf = ByteBuffer.wrap(allBytes)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asShortBuffer()
            val samples = ShortArray(shortBuf.remaining())
            shortBuf.get(samples)

            // ── Mix to mono ─────────────────────────────────────────────────
            val mono = if (channels > 1) {
                val out = ShortArray(samples.size / channels)
                for (i in out.indices) {
                    var sum = 0L
                    for (ch in 0 until channels) {
                        sum += samples[i * channels + ch]
                    }
                    out[i] = (sum / channels).toInt().toShort()
                }
                out
            } else {
                samples
            }

            // ── Resample to 16 kHz ──────────────────────────────────────────
            val resampled = resample(mono, sampleRate, TARGET_SAMPLE_RATE)

            // ── Convert to float [-1, 1] ────────────────────────────────────
            val floats = FloatArray(resampled.size) { resampled[it].toFloat() / 32768f }
            Log.i(TAG, "Output: ${floats.size} float samples at $TARGET_SAMPLE_RATE Hz")
            return floats

        } catch (e: Exception) {
            Log.e(TAG, "Decode failed for $filePath", e)
            try { extractor.release() } catch (_: Exception) {}
            return null
        }
    }

    /**
     * Simple linear-interpolation resampler.
     * Good enough for speech — no audible artifacts at these ratios.
     */
    private fun resample(input: ShortArray, fromRate: Int, toRate: Int): ShortArray {
        if (fromRate == toRate) return input
        if (input.isEmpty()) return input

        val ratio = fromRate.toDouble() / toRate
        val outLen = (input.size / ratio).toInt()
        val output = ShortArray(outLen)

        for (i in output.indices) {
            val srcPos = i * ratio
            val idx    = srcPos.toInt().coerceIn(0, input.size - 2)
            val frac   = srcPos - idx
            val a = input[idx]
            val b = input[(idx + 1).coerceAtMost(input.size - 1)]
            output[i] = (a * (1.0 - frac) + b * frac).toInt().toShort()
        }

        return output
    }
}