//
// Created by diego on 01/03/2022.
//

#ifndef NATIVE_C_AUDIOENGINE_H
#define NATIVE_C_AUDIOENGINE_H

#include <oboe/Oboe.h>

using namespace oboe;

class AudioEngine : public AudioStreamCallback {

public:


    AudioEngine() {
        AudioStreamBuilder builder;

        builder.setSharingMode(SharingMode::Shared);
        builder.setPerformanceMode(PerformanceMode::LowLatency);
        builder.setFormat(AudioFormat::Float);
        builder.setDirection(Direction::Output);
        builder.setCallback(this);
        Result result = builder.openStream(&mStream);
        if (result != Result::OK) {

        }
        int bytes = mStream->getBytesPerSample();
        channelCount = mStream->getChannelCount();
        mPhaseIncrement = kFrequency * kTwoPi / mStream->getSampleRate();
        mStream->requestStart();

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
