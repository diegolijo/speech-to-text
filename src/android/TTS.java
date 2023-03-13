package com.vayapedal.speechtotext;

import static android.speech.tts.Voice.LATENCY_VERY_LOW;

import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

import androidx.annotation.NonNull;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class TTS {

  private static final String DEFAULT_VOICE = "es-us-x-esf-network";
  private final float DEFAULT_VOLUME = 1f;
  private static final String UTTERANCE_ID = "ID";
  private final Activity activity;
  private android.speech.tts.TextToSpeech tts;
  private Bundle bundle = new Bundle();
  private Voice v;
  private CallbackContext callbackSynthesizer;

  public TTS(Activity activity/*, SpeechToText speechToText*/) {
    this.activity = activity;
    tts = new android.speech.tts.TextToSpeech(this.activity, status -> {
      if (status != android.speech.tts.TextToSpeech.ERROR) {
        this.setVoice(DEFAULT_VOICE);
        this.setVolume(DEFAULT_VOLUME);
        tts.setSpeechRate(1f);
        tts.setPitch(1f);

      }
    }, "com.google.android.tts");
    tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
      @Override
      public void onStart(String utteranceId) {
        LOG.e("voices", "onStart");
        try {
          sendCallback("speech start", true);
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onDone(String utteranceId) {
        LOG.e("voices", "onDone");
          try {
            sendCallback("speech done", false);
          } catch (JSONException e) {
            e.printStackTrace();
          }
       // }
      }

      @Override
      public void onError(String utteranceId) {
        LOG.e("voices", "onError");
        try {
          sendCallback("speech error", false);
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    });
  }

  public void setVolume(float f) {
    bundle.putFloat(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME, f);
  }

  public void setPitch(float f) {
    tts.setPitch(f);
  }

  public void setVoice(String name) {
    Voice v = new Voice(
      name,
      Locale.forLanguageTag("es-*"),
      1,
      LATENCY_VERY_LOW,
      false,
      null);
    tts.setVoice(v);
  }

  public JSONArray getVoices() throws JSONException {
    JSONArray arr = new JSONArray();
    for (Voice voice : tts.getVoices()) {
      LOG.e("voice", String.valueOf(voice)); // male -> es-us-x-esf-local es-us-x-esf-network es-us-x-esd-network es-us-x-esd-local
      JSONObject obj = new JSONObject();
      obj.put("name", voice.getName());
      obj.put("locale", voice.getLocale().toLanguageTag());
      obj.put("requiresNetwork", voice.isNetworkConnectionRequired());
      obj.put("latency", voice.getLatency());
      obj.put("quality", voice.getQuality());
      arr.put(obj);
    }
    return arr;
  }

  public void speech(CallbackContext callback, String text, boolean flush) throws JSONException {
    this.callbackSynthesizer = callback;
    tts.speak(text, flush? TextToSpeech.QUEUE_FLUSH: TextToSpeech.QUEUE_ADD, bundle, UTTERANCE_ID);
  }

  //******************************  CORDOVA COMUNICACION **************************************
  private void sendCallback(
    String result,
    boolean keepCallback
  ) throws JSONException {
    PluginResult res = new PluginResult(
      PluginResult.Status.OK,
      getJson(result)
    );
    res.setKeepCallback(keepCallback);
    callbackSynthesizer.sendPluginResult(res);
  }

  @NonNull
  private JSONObject getJson(String result) throws JSONException {
    JSONObject obj = new JSONObject();
    obj.put("action", "speech");
    obj.put("result", result);
    return obj;
  }

}




