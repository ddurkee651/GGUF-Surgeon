#include "gguf_bridge.h"
#include <android/log.h>
#include <vector>
#include <map>
#include <string>
#include <cstring>
#include <fstream>

#define LOG_TAG "GGUFNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

struct GgufContext {
    struct llama_model* model;
    struct llama_context* ctx;
    std::string filepath;
    std::map<std::string, std::string> metadata;
    std::vector<llama_tensor_info> tensors;
};

// Native implementation
extern "C" {

JNIEXPORT jlong JNICALL
Java_com_ggufsurgeon_core_native_NativeGgufParser_nativeParseFile(JNIEnv* env, jobject thiz, jstring path) {
    const char* filepath = env->GetStringUTFChars(path, nullptr);
    
    llama_model_params model_params = llama_model_default_params();
    llama_model* model = llama_load_model_from_file(filepath, model_params);
    
    if (!model) {
        LOGE("Failed to load model: %s", filepath);
        env->ReleaseStringUTFChars(path, filepath);
        return 0;
    }
    
    auto* ctx = new GgufContext();
    ctx->model = model;
    ctx->filepath = std::string(filepath);
    
    // Extract metadata
    int n_metadata = llama_model_meta_count(model);
    for (int i = 0; i < n_metadata; i++) {
        char key[256];
        char value[4096];
        llama_model_meta_key_by_index(model, i, key, sizeof(key));
        llama_model_meta_val_str_by_index(model, i, value, sizeof(value));
        ctx->metadata[std::string(key)] = std::string(value);
    }
    
    // Extract tensor info
    int n_tensors = llama_model_tensors_count(model);
    for (int i = 0; i < n_tensors; i++) {
        llama_tensor_info info;
        llama_model_tensor_info_by_index(model, i, &info);
        ctx->tensors.push_back(info);
    }
    
    env->ReleaseStringUTFChars(path, filepath);
    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT void JNICALL
Java_com_ggufsurgeon_core_native_NativeGgufParser_nativeClose(JNIEnv* env, jobject thiz, jlong handle) {
    auto* ctx = reinterpret_cast<GgufContext*>(handle);
    if (ctx) {
        if (ctx->model) {
            llama_free_model(ctx->model);
        }
        if (ctx->ctx) {
            llama_free(ctx->ctx);
        }
        delete ctx;
    }
}

JNIEXPORT jobjectArray JNICALL
Java_com_ggufsurgeon_core_native_NativeGgufParser_nativeGetMetadata(JNIEnv* env, jobject thiz, jlong handle) {
    auto* ctx = reinterpret_cast<GgufContext*>(handle);
    if (!ctx) return nullptr;
    
    jobjectArray result = env->NewObjectArray(ctx->metadata.size() * 2, 
                                              env->FindClass("java/lang/String"), 
                                              nullptr);
    
    int index = 0;
    for (const auto& entry : ctx->metadata) {
        env->SetObjectArrayElement(result, index++, env->NewStringUTF(entry.first.c_str()));
        env->SetObjectArrayElement(result, index++, env->NewStringUTF(entry.second.c_str()));
    }
    
    return result;
}

JNIEXPORT jobjectArray JNICALL
Java_com_ggufsurgeon_core_native_NativeGgufParser_nativeGetTensors(JNIEnv* env, jobject thiz, jlong handle) {
    auto* ctx = reinterpret_cast<GgufContext*>(handle);
    if (!ctx) return nullptr;
    
    jclass tensorInfoClass = env->FindClass("com/ggufsurgeon/domain/TensorInfo");
    jmethodID constructor = env->GetMethodID(tensorInfoClass, "<init>", 
                                            "(Ljava/lang/String;[ILjava/lang/String;J)V");
    
    jobjectArray result = env->NewObjectArray(ctx->tensors.size(), tensorInfoClass, nullptr);
    
    for (size_t i = 0; i < ctx->tensors.size(); i++) {
        const auto& tensor = ctx->tensors[i];
        
        jstring name = env->NewStringUTF(tensor.name);
        
        jintArray shape = env->NewIntArray(tensor.n_dims);
        env->SetIntArrayRegion(shape, 0, tensor.n_dims, reinterpret_cast<const jint*>(tensor.dims));
        
        jstring type = env->NewStringUTF(llama_tensor_type_name(tensor.type));
        
        jobject tensorObj = env->NewObject(tensorInfoClass, constructor, 
                                          name, shape, type, 
                                          static_cast<jlong>(tensor.size));
        
        env->SetObjectArrayElement(result, i, tensorObj);
        
        env->DeleteLocalRef(name);
        env->DeleteLocalRef(shape);
        env->DeleteLocalRef(type);
        env->DeleteLocalRef(tensorObj);
    }
    
    return result;
}

JNIEXPORT jboolean JNICALL
Java_com_ggufsurgeon_core_native_NativeGgufMerger_nativeMergeLora(JNIEnv* env, jobject thiz,
                                                                   jstring base_path,
                                                                   jstring lora_path,
                                                                   jfloat alpha,
                                                                   jstring output_path,
                                                                   jobject progress_callback) {
    const char* base = env->GetStringUTFChars(base_path, nullptr);
    const char* lora = env->GetStringUTFChars(lora_path, nullptr);
    const char* output = env->GetStringUTFChars(output_path, nullptr);
    
    LOGI("Merging LoRA: base=%s, lora=%s, alpha=%f", base, lora, alpha);
    
    // Get callback method IDs
    jclass callbackClass = env->GetObjectClass(progress_callback);
    jmethodID onProgress = env->GetMethodID(callbackClass, "onProgress", "(ILjava/lang/String;)V");
    jmethodID onComplete = env->GetMethodID(callbackClass, "onComplete", "(Ljava/lang/String;)V");
    jmethodID onError = env->GetMethodID(callbackClass, "onError", "(Ljava/lang/String;)V");
    
    // Load base model
    llama_model_params model_params = llama_model_default_params();
    llama_model* base_model = llama_load_model_from_file(base, model_params);
    
    if (!base_model) {
        env->CallVoidMethod(progress_callback, onError, 
                           env->NewStringUTF("Failed to load base model"));
        env->ReleaseStringUTFChars(base_path, base);
        env->ReleaseStringUTFChars(lora_path, lora);
        env->ReleaseStringUTFChars(output_path, output);
        return JNI_FALSE;
    }
    
    // Load LoRA adapter
    if (!llama_model_apply_lora_from_file(base_model, lora, alpha, nullptr)) {
        env->CallVoidMethod(progress_callback, onError,
                           env->NewStringUTF("Failed to apply LoRA adapter"));
        llama_free_model(base_model);
        env->ReleaseStringUTFChars(base_path, base);
        env->ReleaseStringUTFChars(lora_path, lora);
        env->ReleaseStringUTFChars(output_path, output);
        return JNI_FALSE;
    }
    
    env->CallVoidMethod(progress_callback, onProgress, 50, 
                       env->NewStringUTF("LoRA applied, saving model..."));
    
    // Save merged model
    if (!llama_model_save(base_model, output)) {
        env->CallVoidMethod(progress_callback, onError,
                           env->NewStringUTF("Failed to save merged model"));
        llama_free_model(base_model);
        env->ReleaseStringUTFChars(base_path, base);
        env->ReleaseStringUTFChars(lora_path, lora);
        env->ReleaseStringUTFChars(output_path, output);
        return JNI_FALSE;
    }
    
    llama_free_model(base_model);
    
    env->CallVoidMethod(progress_callback, onComplete,
                       env->NewStringUTF(output));
    
    env->ReleaseStringUTFChars(base_path, base);
    env->ReleaseStringUTFChars(lora_path, lora);
    env->ReleaseStringUTFChars(output_path, output);
    
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_ggufsurgeon_core_native_NativeGgufQuantizer_nativeQuantize(JNIEnv* env, jobject thiz,
                                                                     jstring input_path,
                                                                     jstring output_path,
                                                                     jstring quant_type,
                                                                     jobject progress_callback) {
    const char* input = env->GetStringUTFChars(input_path, nullptr);
    const char* output = env->GetStringUTFChars(output_path, nullptr);
    const char* quant = env->GetStringUTFChars(quant_type, nullptr);
    
    LOGI("Quantizing: input=%s, output=%s, type=%s", input, output, quant);
    
    // Get callback method IDs
    jclass callbackClass = env->GetObjectClass(progress_callback);
    jmethodID onProgress = env->GetMethodID(callbackClass, "onProgress", "(ILjava/lang/String;)V");
    jmethodID onComplete = env->GetMethodID(callbackClass, "onComplete", "(Ljava/lang/String;)V");
    jmethodID onError = env->GetMethodID(callbackClass, "onError", "(Ljava/lang/String;)V");
    
    // Map quantization string to llama type
    llama_ftype ftype;
    if (strcmp(quant, "Q4_0") == 0) ftype = LLAMA_FTYPE_MOSTLY_Q4_0;
    else if (strcmp(quant, "Q4_1") == 0) ftype = LLAMA_FTYPE_MOSTLY_Q4_1;
    else if (strcmp(quant, "Q5_0") == 0) ftype = LLAMA_FTYPE_MOSTLY_Q5_0;
    else if (strcmp(quant, "Q5_1") == 0) ftype = LLAMA_FTYPE_MOSTLY_Q5_1;
    else if (strcmp(quant, "Q8_0") == 0) ftype = LLAMA_FTYPE_MOSTLY_Q8_0;
    else if (strcmp(quant, "Q2_K") == 0) ftype = LLAMA_FTYPE_MOSTLY_Q2_K;
    else if (strcmp(quant, "Q3_K") == 0) ftype = LLAMA_FTYPE_MOSTLY_Q3_K_M;
    else if (strcmp(quant, "Q4_K") == 0) ftype = LLAMA_FTYPE_MOSTLY_Q4_K_M;
    else if (strcmp(quant, "Q5_K") == 0) ftype = LLAMA_FTYPE_MOSTLY_Q5_K_M;
    else if (strcmp(quant, "Q6_K") == 0) ftype = LLAMA_FTYPE_MOSTLY_Q6_K;
    else ftype = LLAMA_FTYPE_MOSTLY_Q4_1;
    
    // Perform quantization
    bool success = llama_model_quantize(input, output, ftype, 1, nullptr);
    
    if (!success) {
        env->CallVoidMethod(progress_callback, onError,
                           env->NewStringUTF("Quantization failed"));
        env->ReleaseStringUTFChars(input_path, input);
        env->ReleaseStringUTFChars(output_path, output);
        env->ReleaseStringUTFChars(quant_type, quant);
        return JNI_FALSE;
    }
    
    env->CallVoidMethod(progress_callback, onComplete,
                       env->NewStringUTF(output));
    
    env->ReleaseStringUTFChars(input_path, input);
    env->ReleaseStringUTFChars(output_path, output);
    env->ReleaseStringUTFChars(quant_type, quant);
    
    return JNI_TRUE;
}

}