//
// Created by diego on 01/03/2022.
//

#ifndef NATIVE_C_AUDIOENGINE_H
#define NATIVE_C_AUDIOENGINE_H

#include <oboe/Oboe.h>
#include <android/log.h>
#include <jni.h>


class AudioEngine : public oboe::AudioStreamCallback {

public:

    AudioEngine() {}

    virtual ~AudioEngine() = default;

    AudioEngine(jobject pJobject, JavaVM *savedVM) {
        this->savedVM = savedVM;
        this->pJobject = pJobject;
    }

    oboe::DataCallbackResult
    onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) override;

    void initInputStream();

    void setNativeCalback();

private :
    oboe::ManagedStream inStream = nullptr;

    JNIEnv *ENV = nullptr;
    JavaVM *savedVM = nullptr;
    jobject pJobject;

    float floatResult = 0;

    int channelCount;
    float chorizo[1920];
    double mPhaseIncrement;
    static float constexpr kAmplitude = 0.5f;
    static float constexpr kFrequency = 440;
    static double constexpr kTwoPi = M_PI * 2;
    float mPhase = 0.0;
};


#endif //NATIVE_C_AUDIOENGINE_H
