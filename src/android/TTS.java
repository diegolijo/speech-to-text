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
  private Bundle b = new Bundle();
  private Voice v;
  private CallbackContext callbackSpeech;

  public TTS(Activity activity, SpeechToText speechToText) {
    this.activity = activity;
    tts = new android.speech.tts.TextToSpeech(this.activity, status -> {
      if (status != android.speech.tts.TextToSpeech.ERROR) {
        this.setVoice(DEFAULT_VOICE);
        this.setVolume(DEFAULT_VOLUME);
        tts.setSpeechRate(1f);
        tts.setPitch(1f);
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
          @Override
          public void onStart(String utteranceId) {
            LOG.e("voices", "onStart");
/*            try {
              sendCallback("speech start", true);
            } catch (JSONException e) {
              e.printStackTrace();
            }*/
          }

          @Override
          public void onDone(String utteranceId) {
            LOG.e("voices", "onDone");
/*            if (speechToText.replay) {
              try {
                sendCallback("speech done", false);
                if (speechToText.replay) {
                  speechToText.startRecognizer();
                }
                speechToText.replay = false;
              } catch (JSONException e) {
                e.printStackTrace();
              }
            }*/
          }

          @Override
          public void onError(String utteranceId) {
            LOG.e("voices", "onError");
 /*           try {
              sendCallback("speech error", false);
            } catch (JSONException e) {
              e.printStackTrace();
            }*/
          }
        });
      }
    }, "com.google.android.tts");
  }

  public void setVolume(float f) {
    b.putFloat(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME, f);
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


  public void speech(CallbackContext callbackSpeech, String text/*, SpeechToText speechToText*/) throws JSONException {
    this.callbackSpeech = callbackSpeech;
/*    if (speechToText.speechServiceIsPlaying) {
      speechToText.stopRecognizer();
      speechToText.replay = true;
    }*/
    tts.speak(text, TextToSpeech.QUEUE_FLUSH, b, UTTERANCE_ID); // TextToSpeech.QUEUE_FLUSH
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
    callbackSpeech.sendPluginResult(res);
  }

  @NonNull
  private JSONObject getJson(String result) throws JSONException {
    JSONObject obj = new JSONObject();
    obj.put("action", "speech");
    obj.put("result", result);
    return obj;
  }

}




