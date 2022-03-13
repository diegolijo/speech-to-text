#include <jni.h>
#include "AudioEngine.h"


using namespace oboe;

static AudioEngine *audioEngine = nullptr;
JavaVM *savedVM;
jobject saved_listener_instance;

extern "C"

JNIEXPORT jboolean JNICALL
Java_com_vayapedal_speechtotext_SpeechToText_initAudioStream(JNIEnv *env, jobject thiz,
                                                             jobject listener_instance) {
    env->GetJavaVM(&savedVM);
    //save listener_instance for use later
    saved_listener_instance = listener_instance;
    audioEngine = new AudioEngine(saved_listener_instance, savedVM);

    return 1;
}




extern "C"
JNIEXPORT jboolean JNICALL
Java_com_vayapedal_speechtotext_SpeechToText_playSeno(JNIEnv *env, jobject thiz, jobject enable) {
    // TODO: implement playSeno()
}
