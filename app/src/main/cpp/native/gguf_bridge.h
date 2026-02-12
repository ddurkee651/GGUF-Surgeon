#ifndef GGUF_BRIDGE_H
#define GGUF_BRIDGE_H

#include <jni.h>
#include <string>
#include <vector>
#include <map>
#include "llama.h"

#ifdef __cplusplus
extern "C" {
#endif

// JNI function declarations
JNIEXPORT jlong JNICALL
Java_com_ggufsurgeon_core_native_NativeGgufParser_nativeParseFile(JNIEnv* env, jobject thiz, jstring path);

JNIEXPORT void JNICALL
Java_com_ggufsurgeon_core_native_NativeGgufParser_nativeClose(JNIEnv* env, jobject thiz, jlong handle);

JNIEXPORT jobjectArray JNICALL
Java_com_ggufsurgeon_core_native_NativeGgufParser_nativeGetMetadata(JNIEnv* env, jobject thiz, jlong handle);

JNIEXPORT jobjectArray JNICALL
Java_com_ggufsurgeon_core_native_NativeGgufParser_nativeGetTensors(JNIEnv* env, jobject thiz, jlong handle);

JNIEXPORT jboolean JNICALL
Java_com_ggufsurgeon_core_native_NativeGgufMerger_nativeMergeLora(JNIEnv* env, jobject thiz,
                                                                   jstring base_path,
                                                                   jstring lora_path,
                                                                   jfloat alpha,
                                                                   jstring output_path,
                                                                   jobject progress_callback);

JNIEXPORT jboolean JNICALL
Java_com_ggufsurgeon_core_native_NativeGgufQuantizer_nativeQuantize(JNIEnv* env, jobject thiz,
                                                                     jstring input_path,
                                                                     jstring output_path,
                                                                     jstring quant_type,
                                                                     jobject progress_callback);

#ifdef __cplusplus
}
#endif

#endif // GGUF_BRIDGE_H