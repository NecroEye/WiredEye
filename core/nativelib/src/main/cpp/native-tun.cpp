// Library adı: wiredeye_native
// app/src/main/cpp/native_tun.cpp

#include <jni.h>
#include <android/log.h>
#include <unistd.h>
#include <fcntl.h>
#include <atomic>
#include <thread>
#include <vector>
#include <sys/epoll.h>

#define LOG_TAG "WiredeyeNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static JavaVM *gVm = nullptr;
static jobject gListener = nullptr;
static jmethodID gOnBatch = nullptr;

static std::atomic<bool> gRunning(false);
static std::thread gThread;

static void loop_read_tun(int tunFd, int mtu, int readTimeoutMs,
        int maxBatch, int maxBatchBytes, int flushTimeoutMs) {
    JNIEnv *env = nullptr;
    bool needDetach = false;
    if (gVm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        if (gVm->AttachCurrentThread(&env, nullptr) == JNI_OK) needDetach = true;
    }
    if (!env) {
        LOGE("AttachCurrentThread failed");
        return;
    }

    int flags = fcntl(tunFd, F_GETFL, 0);
    fcntl(tunFd, F_SETFL, flags | O_NONBLOCK);

    int ep = epoll_create1(0);
    if (ep < 0) {
        LOGE("epoll_create1 failed");
        close(tunFd);
        if (needDetach) gVm->DetachCurrentThread();
        return;
    }

    epoll_event ev{.events = EPOLLIN, .data = {.fd = tunFd}};
    epoll_ctl(ep, EPOLL_CTL_ADD, tunFd, &ev);

    std::vector <jbyte> buf;
    buf.resize(std::max(2000, mtu + 64));

    LOGI("loop_read_tun: entering");
    while (gRunning.load()) {
        epoll_event outEv{};
        int n = epoll_wait(ep, &outEv, 1, readTimeoutMs);
        if (n < 0) {
            LOGW("epoll_wait error");
            break;
        }
        if (n == 0) continue;

        if ((outEv.events & EPOLLIN) && outEv.data.fd == tunFd) {
            ssize_t r = read(tunFd, buf.data(), (int) buf.size());
            if (r > 0 && gListener && gOnBatch) {
                jbyteArray arr = env->NewByteArray((jsize) r);
                if (arr) {
                    env->SetByteArrayRegion(arr, 0, (jsize) r, buf.data());
                    // packetCount=0: tek ham frame
                    env->CallVoidMethod(gListener, gOnBatch, arr, (jint) r, (jint) 0);
                    env->DeleteLocalRef(arr);
                }
                if (env->ExceptionCheck()) {
                    env->ExceptionDescribe();
                    env->ExceptionClear();
                    LOGE("Exception calling onNativeBatch");
                }
            }
        }
    }

    LOGI("loop_read_tun: exiting");
    close(ep);
    close(tunFd);
    if (needDetach) gVm->DetachCurrentThread();
}

extern "C" JNIEXPORT void JNICALL
Java_com_muratcangzm_core_NativeTun_nativeSetListener(
        JNIEnv
* env, jclass /*clazz*/,
jobject listener
) {
if (gListener) {
env->
DeleteGlobalRef(gListener);
gListener = nullptr;
}
gOnBatch = nullptr;

if (listener) {
gListener = env->NewGlobalRef(listener);
jclass cls = env->GetObjectClass(listener);
gOnBatch = env->GetMethodID(cls, "onNativeBatch", "([BII)V");
if (!gOnBatch)
LOGE("Failed to resolve onNativeBatch([BII)V");
}
LOGI("nativeSetListener done");
}

extern "C" JNIEXPORT jboolean
JNICALL
        Java_com_muratcangzm_core_NativeTun_nativeStart(
        JNIEnv * /*env*/, jclass /*clazz*/,
        jint
tunFdDetached,
jint mtu,
        jint
maxBatch,
jint maxBatchBytes,
        jint
flushTimeoutMs,
jint readTimeoutMs
) {

if (gRunning.

load()

) return
JNI_TRUE;
int fd = tunFdDetached;
if (fd < 0) {
LOGE("invalid tun fd"); return
JNI_FALSE;
}

gRunning.store(true);
try {
gThread = std::thread(loop_read_tun, fd, (int) mtu, (int) readTimeoutMs,
        (int) maxBatch, (int) maxBatchBytes, (int) flushTimeoutMs);
} catch (...) {
gRunning.store(false);
LOGE("failed to start thread");
close(fd);
return
JNI_FALSE;
}
LOGI("nativeStart ok");
return
JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_muratcangzm_core_NativeTun_nativeStop(
        JNIEnv
* /*env*/, jclass /*clazz*/) {
if (!gRunning.exchange(false)) return;
if (gThread.

joinable()

) gThread.

join();

LOGI("nativeStop done");
}

jint JNI_OnLoad(JavaVM *vm, void *) {
    gVm = vm;
    return JNI_VERSION_1_6;
}