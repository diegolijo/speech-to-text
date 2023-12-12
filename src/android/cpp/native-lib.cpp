#include <jni.h>
#include <oboe/Oboe.h>
#include <android/log.h>

class AudioEngine : public oboe::AudioStreamCallback
{
public:
    oboe::Result start();

    oboe::Result stop();

    oboe::AudioStream *stream = nullptr;

    long nextCallbackTime = 0;     // Timestamp for next amplitude callback
    int callbackIntervalMs = 1000; // Call Java callback every 100ms

    void setupCallback(JNIEnv *env, jobject javaInstance);

    void calculateAndReportAmplitude(float *audioData, int32_t numFrames, long currentTime);

private:
    JNIEnv *mEnv = nullptr;
    jobject mJavaInstance = nullptr;

    oboe::DataCallbackResult
    onAudioReady(oboe::AudioStream *oboeStream, void *audioData, int32_t numFrames) override
    {
        auto currentTime = std::chrono::steady_clock::now().time_since_epoch().count();

        // Si ha pasado el intervalo establecido, actualizamos la próxima hora de la llamada y calculamos la amplitud
        if (currentTime >= nextCallbackTime)
        {
            nextCallbackTime = currentTime + std::chrono::milliseconds(callbackIntervalMs).count();

            // Ten en cuenta que aquí estamos suponiendo que 'audioData' es de tipo 'float*', lo que
            // debes asegurarte que sea así configurando el formato del stream de audio adecuadamente.
            calculateAndReportAmplitude(static_cast<float *>(audioData), numFrames, currentTime);
        }

        return oboe::DataCallbackResult::Continue;
    }
};

AudioEngine audioEngine;

extern "C"
{
    JNIEXPORT void JNICALL
    Java_com_vayapedal_speechtotext_NativeLib_iniciarCapturaDeAudio(JNIEnv *env, jobject /* this */)
    {
        audioEngine.start();
    }

    JNIEXPORT void JNICALL
    Java_com_vayapedal_speechtotext_NativeLib_detenerCapturaDeAudio(JNIEnv *env, jobject /* this */)
    {
        audioEngine.stop();
    }

    JNIEXPORT void JNICALL
    Java_com_vayapedal_speechtotext_NativeLib_nativeSetup(JNIEnv *env, jobject instance)
    {
        audioEngine.setupCallback(env, instance);
    }

    oboe::Result AudioEngine::start()
    {
        oboe::AudioStreamBuilder builder;
        builder.setDirection(oboe::Direction::Output);
        builder.setFormat(oboe::AudioFormat::Float); // Configura el formato de audio como Float
        builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
        builder.setSharingMode(oboe::SharingMode::Exclusive);
        // Personaliza los parámetros del stream de acuerdo a tus necesidades
        builder.setCallback(this);
        oboe::Result result = builder.openStream(&stream);
        if (result == oboe::Result::OK && stream)
        {
            result = stream->requestStart();
        }
        return result;
    }

    oboe::Result AudioEngine::stop()
    {
        if (stream != nullptr)
        {
            return stream->stop();
        }
        return oboe::Result::ErrorNull;
    }

    void AudioEngine::setupCallback(JNIEnv *env, jobject instance)
    {
        // Store the JVM and the instance to use in the callback
        mEnv = env;
        mJavaInstance = env->NewGlobalRef(instance);
    }

    void
    AudioEngine::calculateAndReportAmplitude(float *audioData, int32_t numFrames, long currentTime)
    {
        float amplitude = 0;

        // Calcula la amplitud de alguna manera. Ejemplo simple, encuentra el valor máximo
        for (int i = 0; i < numFrames; i++)
        {
            amplitude = std::max(amplitude, std::abs(audioData[i]));
        }

        // Usa JNI para llamar a la función Java onAmplitudeCallback
        jclass clazz = mEnv->GetObjectClass(mJavaInstance);
        jmethodID callbackMethod = mEnv->GetMethodID(clazz, "onAmplitudeCallback", "(F)V");

        if (callbackMethod != nullptr)
        {
            mEnv->CallVoidMethod(mJavaInstance, callbackMethod, amplitude);
        }
        mEnv->DeleteLocalRef(clazz);
    }
}
