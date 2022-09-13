/**
 * Created by Parrino on 23/03/2022.
 */


#include "AudioEngine.h"
#include <android/log.h>

void AudioEngine::initInputStream() {
    oboe::AudioStreamBuilder builder;
    /** builder.setSharingMode(SharingMode::Shared);
    builder.setPerformanceMode(PerformanceMode::LowLatency);*/
    builder.setFormat(oboe::AudioFormat::Float);
    builder.setDirection(oboe::Direction::Input);
    builder.setCallback(this);
    oboe::Result result = builder.openManagedStream(inStream);
    if (result != oboe::Result::OK) {
        // todo send error
    }
    channelCount = inStream->getChannelCount();
    result = inStream->requestStart();
    if (result != oboe::Result::OK) {
        // todo send error
    }
    setNativeCalback();

    // __android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "%s", 'Pruebassssssssssssssds');
    /**    constexpr int kMillisToRecord = 2;
     const int32_t requestFrames = (int32_t) (kMillisToRecord * mStream->getSampleRate() /
                                              kMillisPerSecond);
     float myBuffer[requestFrames];

     constexpr int64_t kTimeoutValue = 3 * kNanosPerMillisecond;

     int framesRead;
     do {
         auto r = mStream->read(myBuffer, mStream->getBufferSizeInFrames(), 0);
         if (r != Result::OK)break;
         framesRead = r.value();
     } while (framesRead !

       while (1){
     auto r = mStream->read(myBuffer, requestFrames, kTimeoutValue);
     if (r == Result::OK) {
         r.value();
     } else {
         log('error');
             }
          }

        __android_log_print(ANDROID_LOG_ERROR,
                              "AudioEngine",
                              "Error opening stream %s",
                              convertToText(result));
     */

    // mStream->close();
}

oboe::DataCallbackResult
AudioEngine::onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
    oboe::DataCallbackResult callbackResult = oboe::DataCallbackResult::Continue;
    auto *floatData = static_cast<float *>(audioData);
    int count = 0;
    for (int i = 0; i < numFrames; ++i) {
        for (int j = 0; j < channelCount; j++) {
            count++;
            int index = i * channelCount + j;
            //  chorizo[index] = floatData[index];
            if (count % 10 < 0.5) {
                floatData[index] = 1;
            } else {
                floatData[index] = 0;
            }
        }
    }
    //   setNativeCalback(chorizo[0]);
    return callbackResult;
}

/******************* recuperamos el objeto  NativeListener *******************/
void AudioEngine::setNativeCalback() {
    /**Get current thread JNIEnv*/
    int stat = savedVM->GetEnv((void **) &ENV, JNI_VERSION_1_6);
    if (stat == JNI_EDETACHED)  //We are on a different thread, attach
        savedVM->AttachCurrentThread(reinterpret_cast<JNIEnv **>((void **) &ENV), NULL);
    if (ENV == NULL)
        return;  /**Cant attach to java, bail*/
    /**Get the Listener class reference*/
    jclass listenerClassRef = ENV->GetObjectClass(
            pJobject); // todo otra forma de recuperar: jclass listenerClassRef = ENV->FindClass("com/vayapedal/speechtotext/NativeListener");
    /**Use Listener class reference to load the eventOccurred method*/
    jmethodID listenerEventOccured = ENV->GetMethodID(listenerClassRef, "eventOccurred",
                                                      "(F)V");
    /**invoke listener eventOccurred*/
    ENV->CallVoidMethod(pJobject, listenerEventOccured, floatResult);
}








