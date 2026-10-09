#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_aegis7_app_bridge_AiBridge_nativeGetStatus(
        JNIEnv* env,
        jobject /* thiz */) {
    const std::string status = "Aegis-7 native engine is ready.";
    return env->NewStringUTF(status.c_str());
}
