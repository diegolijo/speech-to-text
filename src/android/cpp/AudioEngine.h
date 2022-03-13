//
// Created by diego on 01/03/2022.
//

#ifndef NATIVE_C_AUDIOENGINE_H
#define NATIVE_C_AUDIOENGINE_H

#include <oboe/Oboe.h>

using namespace oboe;

class AudioEngine : public AudioStreamCallback {

public:

    AudioEngine(jobject pJobject, JavaVM *savedVM) {
        AudioStreamBuilder builder;

        builder.setSharingMode(SharingMode::Shared);
        builder.setPerformanceMode(PerformanceMode::LowLatency);
        builder.setFormat(AudioFormat::Float);
        builder.setDirection(Direction::Input);
        builder.setCallback(this);
        Result result = builder.openStream(&mStream);
        if (result != Result::OK) {

        }
        // int bytes = mStream->getBytesPerSample();
        channelCount = mStream->getChannelCount();
        // mPhaseIncrement = kFrequency * kTwoPi / mStream->getSampleRate();
        result = mStream->requestStart();
        if (result != Result::OK) {

        }
        constexpr int kMillisToRecord = 2;
        const int32_t requestFrames = (int32_t) (kMillisToRecord * mStream->getSampleRate() /
                                                 kMillisPerSecond);
        int16_t myBuffer[requestFrames];

        constexpr int64_t kTimeoutValue = 3 * kNanosPerMillisecond;

        int framesRead = 0;

        do {
            auto r = mStream->read(myBuffer, mStream->getBufferSizeInFrames(), 0);
            if (r != Result::OK)break;
            framesRead = r.value();
        } while (framesRead != 0);

        // while (1){
        auto r = mStream->read(myBuffer, requestFrames, kTimeoutValue);
        if (r != Result::OK) {
            r.value();
        } else {
            log('error');
        }
        //  }

        /**************** recuperamos el objeto  NativeListener **************/

        //Get current thread JNIEnv
        JNIEnv *ENV;
        int stat = savedVM->GetEnv((void **) &ENV, JNI_VERSION_1_6);
        if (stat == JNI_EDETACHED)  //We are on a different thread, attach
            savedVM->AttachCurrentThread(reinterpret_cast<JNIEnv **>((void **) &ENV), NULL);
        if (ENV == NULL)
            return;  //Cant attach to java, bail

        //Get the Listener class reference
        jclass listenerClassRef = ENV->GetObjectClass(pJobject);
        //jclass listenerClassRef = ENV->FindClass("com/vayapedal/speechtotext/NativeListener");

        //Use Listener class reference to load the eventOccurred method
        jmethodID listenerEventOccured = ENV->GetMethodID(listenerClassRef, "eventOccurred",
                                                          "(F)V");

        //invoke listener eventOccurred
        ENV->CallVoidMethod(pJobject, listenerEventOccured, 0.9f);


        mStream->close();
    }

    DataCallbackResult
    onAudioReady(AudioStream *audioStream, void *audioData, int32_t numFrames) override {
        float *floatData = static_cast<float *>(audioData);
        for (int i = 0; i < numFrames; ++i) {
            float sampleValue = kAmplitude * sinf(mPhase);
            for (int j = 0; j < channelCount; j++) {
                floatData[i * channelCount + j] = sampleValue;
            }
            mPhase += mPhaseIncrement;
            if (mPhase >= kTwoPi) mPhase -= kTwoPi;
        }
        return DataCallbackResult::Continue;
    }


private :
    AudioStream *mStream = nullptr;
    double mPhaseIncrement;
    static float constexpr kAmplitude = 0.5f;
    static float constexpr kFrequency = 440;
    static double constexpr kTwoPi = M_PI * 2;
    float mPhase = 0.0;
    int channelCount;
};


#endif //NATIVE_C_AUDIOENGINE_H
