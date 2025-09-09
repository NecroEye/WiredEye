#include <jni.h>
#include <android/log.h>

#define LOG_TAG "WiredeyeNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jint JNICALL
Java_com_muratcangzm_core_NativeBridge_add(
        JNIEnv* /*env*/, jobject /*thiz*/, jint a, jint b) {
    LOGI("add(%d, %d)", a, b);
    return a + b;
}