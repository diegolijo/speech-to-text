#include <jni.h>
#include <oboe/Oboe.h>
#include <android/log.h>

class AudioEngine : public oboe::AudioStreamCallback {
public:
    oboe::Result start();

    oboe::Result stop();

    oboe::AudioStream *stream = nullptr;

    void setupCallback(JNIEnv *env, jobject thiz);

    void calculateAndReportAmplitude(float *audioData, int32_t numFrames, long currentTime);

private:

    jobject jObj = nullptr;
    JavaVM *savedVM = nullptr;

    long nextCallbackTime = 0;     // Timestamp for next amplitude callback
    int callbackIntervalMs = 1000; // Call Java callback every 1000ms

    oboe::DataCallbackResult
    onAudioReady(oboe::AudioStream *oboeStream, void *audioData, int32_t numFrames) override {
        auto currentTime = std::chrono::steady_clock::now().time_since_epoch().count();

        // Si ha pasado el intervalo establecido, actualizamos la próxima hora de la llamada y calculamos la amplitud
        if (currentTime >= nextCallbackTime) {
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
Java_com_vayapedal_speechtotext_NativeLib_iniciarCapturaDeAudio(JNIEnv *env, jobject /* this */) {
    audioEngine.start();
}

JNIEXPORT void JNICALL
Java_com_vayapedal_speechtotext_NativeLib_detenerCapturaDeAudio(JNIEnv *env, jobject /* this */) {
    audioEngine.stop();
}

JNIEXPORT void JNICALL
Java_com_vayapedal_speechtotext_NativeLib_nativeSetup(JNIEnv *env, jobject instance) {
    audioEngine.setupCallback(env, instance);
}

oboe::Result AudioEngine::start() {
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Input);
    builder.setFormat(oboe::AudioFormat::Float); // Configura el formato de audio como Float
    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    builder.setSharingMode(oboe::SharingMode::Shared);
    // Personaliza los parámetros del stream de acuerdo a tus necesidades
    builder.setCallback(this);
    oboe::Result result = builder.openStream(&stream);
    if (result == oboe::Result::OK && stream) {
        result = stream->requestStart();
    }
    return result;
}
}

oboe::Result AudioEngine::stop() {
    if (stream != nullptr) {
        return stream->stop();
    }
    return oboe::Result::ErrorNull;
}

void AudioEngine::setupCallback(JNIEnv *env, jobject thiz) {
    // Store the JVM and the instance to use in the callback
    env->GetJavaVM(&savedVM);
    jObj = env->NewGlobalRef(thiz);
    // jclass cls = env->GetObjectClass(thiz);
    // jClass = env->NewGlobalRef(cls);
}

void
AudioEngine::calculateAndReportAmplitude(float *audioData, int32_t numFrames, long currentTime) {
    float amplitude = 0;

    for (int i = 0; i < numFrames; i++) {
        amplitude = std::max(amplitude, std::abs(audioData[i]));
    }

    JNIEnv *env;
    int stat = savedVM->GetEnv((void **) &env, JNI_VERSION_1_6);
    if (stat == JNI_EDETACHED)  // We are on a different thread, attach
        savedVM->AttachCurrentThread(reinterpret_cast<JNIEnv **>((void **) &env), nullptr);
    if (env == nullptr)
        return;  // Can't attach to java, bail

    jclass jClass = env->GetObjectClass(jObj);
    jmethodID method = env->GetMethodID(jClass, "onAmplitudeCallback", "(F)V");
    env->CallVoidMethod(jObj, method, amplitude);
    env->DeleteLocalRef(jClass);

    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        // Manejar la excepción según tus necesidades
        env->ExceptionClear();  // Limpia la excepción después de manejarla
    }
    savedVM->DetachCurrentThread();
}
