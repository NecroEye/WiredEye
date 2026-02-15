#include <jni.h>
#include <memory>
#include <mutex>
#include <string>

#include "leak_analyzer.h"

static std::mutex gMu;
static std::unique_ptr<LeakAnalyzer> gAnalyzer;

static std::string jstringToStd(JNIEnv* env, jstring s) {
    if (!s) return {};
    const char* chars = env->GetStringUTFChars(s, nullptr);
    std::string out(chars ? chars : "");
    env->ReleaseStringUTFChars(s, chars);
    return out;
}

extern "C" {

JNIEXPORT void JNICALL
Java_com_muratcangzm_core_leak_NativeLeakAnalyzer_nativeInit(
        JNIEnv*,
        jobject,
        jlong windowMs
) {
    std::lock_guard<std::mutex> lg(gMu);
    const int64_t w = (windowMs <= 0) ? 600000 : static_cast<int64_t>(windowMs);
    gAnalyzer = std::make_unique<LeakAnalyzer>(w);
}

JNIEXPORT void JNICALL
Java_com_muratcangzm_core_leak_NativeLeakAnalyzer_nativeSetWindowMs(
        JNIEnv*,
        jobject,
        jlong windowMs
) {
    std::lock_guard<std::mutex> lg(gMu);
    if (!gAnalyzer) gAnalyzer = std::make_unique<LeakAnalyzer>(600000);
    gAnalyzer->setWindowMs(static_cast<int64_t>(windowMs));
}

JNIEXPORT void JNICALL
Java_com_muratcangzm_core_leak_NativeLeakAnalyzer_nativeReset(
        JNIEnv*,
        jobject
) {
    std::lock_guard<std::mutex> lg(gMu);
    if (gAnalyzer) gAnalyzer->reset();
}

JNIEXPORT void JNICALL
Java_com_muratcangzm_core_leak_NativeLeakAnalyzer_nativeOnDns(
        JNIEnv* env,
        jobject,
        jlong tsMs,
        jint uid,
        jstring qname,
        jint qtype,
        jstring serverIp
) {
    std::lock_guard<std::mutex> lg(gMu);
    if (!gAnalyzer) gAnalyzer = std::make_unique<LeakAnalyzer>(600000);

    const std::string q = jstringToStd(env, qname);
    const std::string ip = jstringToStd(env, serverIp);

    gAnalyzer->onDns(
            static_cast<int64_t>(tsMs),
            static_cast<int32_t>(uid),
            q,
            static_cast<int32_t>(qtype),
            ip
    );
}

JNIEXPORT jstring JNICALL
Java_com_muratcangzm_core_leak_NativeLeakAnalyzer_nativeSnapshotJson(
        JNIEnv* env,
        jobject,
        jint topN
) {
    std::lock_guard<std::mutex> lg(gMu);
    if (!gAnalyzer) gAnalyzer = std::make_unique<LeakAnalyzer>(600000);

    const int32_t n = (topN <= 0) ? 10 : static_cast<int32_t>(topN);
    LeakSnapshot snap = gAnalyzer->snapshot(n);
    return env->NewStringUTF(snap.json.c_str());
}

}
