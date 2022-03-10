#include <jni.h>
#include "AudioEngine.h"


using namespace oboe;

static AudioEngine *audioEngine = nullptr;

extern "C"

JNIEXPORT jboolean JNICALL
Java_com_vayapedal_speechtotext_SpeechToText_initAudioStream(JNIEnv *env, jobject thiz) {
    audioEngine = new AudioEngine();
    return 1;
}




extern "C"
JNIEXPORT jboolean JNICALL
Java_com_vayapedal_speechtotext_SpeechToText_playSeno(JNIEnv *env, jobject thiz, jobject enable) {
    // TODO: implement playSeno()
}
