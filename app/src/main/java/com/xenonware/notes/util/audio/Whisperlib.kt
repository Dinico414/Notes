package com.xenonware.notes.util.audio


/**
 * JNI bridge to the whisper.cpp native library.
 *
 * The native library is built by CMake from `app/src/main/cpp/`.
 */
object WhisperLib {

    init {
        System.loadLibrary("whisper_jni")
    }

    /**
     * Native whisper.cpp version string, e.g. "1.9.5".
     *
     * Read from the compiled shared library rather than BuildConfig so it can be
     * used to prove which source was actually linked in.
     */
    external fun nativeVersion(): String

    /**
     * Load a ggml model file and return an opaque context pointer.
     * Returns 0 on failure.
     */
    external fun initContext(modelPath: String): Long

    /**
     * Run full transcription on 16 kHz mono float PCM audio.
     *
     * @param contextPtr  pointer returned by [initContext]
     * @param audioData   16 kHz, mono, float32 PCM samples in [-1, 1]
     * @param language    ISO-639-1 code, e.g. "en", "de"
     * @param numThreads  CPU threads to use (2–4 recommended on mobile)
     * @return JSON string: `{"segments":[{"text":"...","start_ms":0,"end_ms":1000}, ...]}`
     */
    external fun transcribeAudio(
        contextPtr: Long,
        audioData: FloatArray,
        language: String,
        numThreads: Int,
    ): String

    /**
     * Free a previously loaded context. Safe to call with 0.
     */
    external fun freeContext(contextPtr: Long)
}