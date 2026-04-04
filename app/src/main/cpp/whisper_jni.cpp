#include <jni.h>
#include <android/log.h>
#include "whisper.h"
#include <string>
#include <vector>

#define TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

extern "C" {

// ── Load model and return an opaque context pointer ─────────────────────────
JNIEXPORT jlong JNICALL
Java_com_xenonware_notes_util_audio_WhisperLib_initContext(
        JNIEnv *env, jobject /* thiz */, jstring model_path) {

    const char *path = env->GetStringUTFChars(model_path, nullptr);
    LOGI("Loading model from: %s", path);

    struct whisper_context_params cparams = whisper_context_default_params();
    struct whisper_context *ctx = whisper_init_from_file_with_params(path, cparams);

    env->ReleaseStringUTFChars(model_path, path);

    if (ctx == nullptr) {
        LOGE("Failed to initialise whisper context");
        return 0;
    }

    LOGI("Model loaded successfully");
    return reinterpret_cast<jlong>(ctx);
}

// ── Run full transcription, return JSON string ──────────────────────────────
JNIEXPORT jstring JNICALL
Java_com_xenonware_notes_util_audio_WhisperLib_transcribeAudio(
        JNIEnv *env, jobject /* thiz */,
        jlong   context_ptr,
        jfloatArray audio_data,
        jstring language,
        jint    num_threads) {

    auto *ctx = reinterpret_cast<struct whisper_context *>(context_ptr);
    if (ctx == nullptr) {
        return env->NewStringUTF(R"({"error":"null context"})");
    }

    jfloat *audio     = env->GetFloatArrayElements(audio_data, nullptr);
    jsize   audio_len = env->GetArrayLength(audio_data);
    const char *lang  = env->GetStringUTFChars(language, nullptr);

    LOGI("Transcribing %d samples, lang=%s, threads=%d", audio_len, lang, num_threads);

    struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.n_threads        = num_threads;
    params.language         = lang;
    params.translate        = false;
    params.no_context       = true;
    params.single_segment   = false;
    params.print_special    = false;
    params.print_progress   = false;
    params.print_realtime   = false;
    params.print_timestamps = true;

    int result = whisper_full(ctx, params, audio, audio_len);

    env->ReleaseFloatArrayElements(audio_data, audio, 0);
    env->ReleaseStringUTFChars(language, lang);

    if (result != 0) {
        LOGE("Transcription failed with code: %d", result);
        return env->NewStringUTF(R"({"error":"transcription failed"})");
    }

    int n_segments = whisper_full_n_segments(ctx);
    LOGI("Got %d segment(s)", n_segments);

    // ── Build a compact JSON response ───────────────────────────────────────
    std::string json = R"({"segments":[)";

    for (int i = 0; i < n_segments; i++) {
        if (i > 0) json += ',';

        const char *text = whisper_full_get_segment_text(ctx, i);
        int64_t t0 = whisper_full_get_segment_t0(ctx, i);   // centiseconds
        int64_t t1 = whisper_full_get_segment_t1(ctx, i);

        // Escape the segment text so the JSON stays valid
        std::string escaped;
        for (const char *p = text; *p; ++p) {
            switch (*p) {
                case '"':  escaped += "\\\""; break;
                case '\\': escaped += "\\\\"; break;
                case '\n': escaped += "\\n";  break;
                case '\r': escaped += "\\r";  break;
                case '\t': escaped += "\\t";  break;
                default:   escaped += *p;     break;
            }
        }

        json += R"({"text":")" + escaped + R"(",)"
                + R"("start_ms":)" + std::to_string(t0 * 10) + ','
                + R"("end_ms":)"   + std::to_string(t1 * 10) + '}';
    }

    json += "]}";
    return env->NewStringUTF(json.c_str());
}

// ── Free the context ────────────────────────────────────────────────────────
JNIEXPORT void JNICALL
Java_com_xenonware_notes_util_audio_WhisperLib_freeContext(
        JNIEnv * /* env */, jobject /* thiz */, jlong context_ptr) {

auto *ctx = reinterpret_cast<struct whisper_context *>(context_ptr);
if (ctx != nullptr) {
whisper_free(ctx);
LOGI("Context freed");
}
}

} // extern "C"