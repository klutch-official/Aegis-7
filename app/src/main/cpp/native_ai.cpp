#include <jni.h>
#include <cstdint>
#include <llama.h>

#include <algorithm>
#include <mutex>
#include <string>
#include <vector>

namespace {
std::mutex g_mutex;
llama_model *g_model = nullptr;
llama_context *g_context = nullptr;
const llama_vocab *g_vocab = nullptr;

void unloadModelLocked() {
    if (g_context) {
        llama_free(g_context);
        g_context = nullptr;
    }
    if (g_model) {
        llama_model_free(g_model);
        g_model = nullptr;
    }
    g_vocab = nullptr;
}

jstring toJavaString(JNIEnv *env, const std::string &value) {
    return env->NewStringUTF(value.c_str());
}
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_aegis7_app_bridge_AiBridge_nativeGetStatus(
        JNIEnv *env, jobject) {
    return toJavaString(env, "Aegis-7 native engine is ready.");
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_aegis7_app_bridge_AiBridge_nativeLoadModel(
        JNIEnv *env, jobject, jstring jpath) {
    if (!jpath) {
        return toJavaString(env, "Error: model path is empty.");
    }

    const char *pathChars = env->GetStringUTFChars(jpath, nullptr);
    if (!pathChars) {
        return toJavaString(env, "Error: could not read model path.");
    }
    const std::string path(pathChars);
    env->ReleaseStringUTFChars(jpath, pathChars);

    std::lock_guard<std::mutex> lock(g_mutex);
    unloadModelLocked();

    llama_backend_init();

    llama_model_params modelParams = llama_model_default_params();
    modelParams.n_gpu_layers = 0;

    g_model = llama_model_load_from_file(path.c_str(), modelParams);
    if (!g_model) {
        return toJavaString(env, "Error: model could not be loaded. Check the GGUF file and available memory.");
    }

    llama_context_params contextParams = llama_context_default_params();
    contextParams.n_ctx = 512;
    contextParams.n_batch = 128;
    contextParams.n_ubatch = 64;
    contextParams.n_threads = 2;
    contextParams.n_threads_batch = 2;

    g_context = llama_init_from_model(g_model, contextParams);
    if (!g_context) {
        unloadModelLocked();
        return toJavaString(env, "Error: could not create model context. The model may need more memory.");
    }

    g_vocab = llama_model_get_vocab(g_model);
    if (!g_vocab) {
        unloadModelLocked();
        return toJavaString(env, "Error: model vocabulary is unavailable.");
    }

    return toJavaString(env, "OK: model loaded.");
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_aegis7_app_bridge_AiBridge_nativeGenerate(
        JNIEnv *env, jobject, jstring jprompt) {
    if (!jprompt) {
        return toJavaString(env, "Error: prompt is empty.");
    }

    const char *promptChars = env->GetStringUTFChars(jprompt, nullptr);
    if (!promptChars) {
        return toJavaString(env, "Error: could not read prompt.");
    }
    const std::string prompt(promptChars);
    env->ReleaseStringUTFChars(jprompt, promptChars);

    if (prompt.empty()) {
        return toJavaString(env, "Error: prompt is empty.");
    }

    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_model || !g_context || !g_vocab) {
        return toJavaString(env, "No model loaded. Import a GGUF model first.");
    }

    if (prompt.size() > static_cast<size_t>(INT32_MAX)) {
        return toJavaString(env, "Error: prompt is too long.");
    }

    const int32_t promptLength = static_cast<int32_t>(prompt.size());
    int32_t needed = llama_tokenize(
            g_vocab, prompt.c_str(), promptLength,
            nullptr, 0, true, false);

    if (needed == INT32_MIN) {
        return toJavaString(env, "Error: prompt token count overflow.");
    }

    if (needed < 0) {
        needed = -needed;
    }

    if (needed <= 0 || needed > 4096) {
        return toJavaString(env, "Error: prompt could not be tokenized or is too long.");
    }

    std::vector<llama_token> tokens(static_cast<size_t>(needed));
    int32_t tokenCount = llama_tokenize(
            g_vocab, prompt.c_str(), promptLength,
            tokens.data(), static_cast<int32_t>(tokens.size()),
            true, false);

    if (tokenCount < 0) {
        if (tokenCount == INT32_MIN) {
            return toJavaString(env, "Error: prompt token count overflow.");
        }

        const int32_t required = -tokenCount;
        if (required <= 0 || required > 4096) {
            return toJavaString(env, "Error: prompt is too long.");
        }

        tokens.resize(static_cast<size_t>(required));
        tokenCount = llama_tokenize(
                g_vocab, prompt.c_str(), promptLength,
                tokens.data(), static_cast<int32_t>(tokens.size()),
                true, false);
    }

    if (tokenCount <= 0 ||
        static_cast<size_t>(tokenCount) > tokens.size()) {
        return toJavaString(env, "Error: prompt tokenization failed.");
    }

    tokens.resize(static_cast<size_t>(tokenCount));

    const int32_t maxContext = static_cast<int32_t>(llama_n_ctx(g_context));
    if (tokenCount >= maxContext) {
        return toJavaString(env, "Error: prompt is longer than the current context window.");
    }

    llama_memory_clear(llama_get_memory(g_context), true);

    llama_batch batch = llama_batch_get_one(
            tokens.data(), static_cast<int32_t>(tokens.size()));

    if (llama_decode(g_context, batch) != 0) {
        return toJavaString(env, "Error: model failed to process the prompt.");
    }

    llama_sampler *sampler = llama_sampler_init_greedy();
    if (!sampler) {
        return toJavaString(env, "Error: could not initialize text sampler.");
    }

    std::string output;
    const int32_t maxNewTokens = std::min(128, maxContext - tokenCount - 1);

    for (int32_t i = 0; i < maxNewTokens; ++i) {
        llama_token token = llama_sampler_sample(sampler, g_context, -1);
        if (llama_vocab_is_eog(g_vocab, token)) {
            break;
        }

        char piece[256];
        const int32_t pieceSize = llama_token_to_piece(
                g_vocab, token, piece, sizeof(piece), 0, false);

        if (pieceSize > 0) {
            output.append(piece, static_cast<size_t>(pieceSize));
        }

        llama_batch nextBatch = llama_batch_get_one(&token, 1);
        if (llama_decode(g_context, nextBatch) != 0) {
            break;
        }
    }

    llama_sampler_free(sampler);

    if (output.empty()) {
        return toJavaString(env, "The model returned no text. Try another prompt or model.");
    }
    return toJavaString(env, output);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_aegis7_app_bridge_AiBridge_nativeUnloadModel(
        JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    unloadModelLocked();
}
