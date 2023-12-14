package com.vayapedal.speechtotext;

import androidx.annotation.NonNull;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

public class NativeLib {

  static {
    System.loadLibrary("native-lib");
  }

  private CallbackContext callbackContext;

  public void initCapture(CallbackContext callbackContext) throws JSONException {
    if (this.callbackContext == null) {
      this.callbackContext = callbackContext;
    }
    this.nativeSetup();
    this.iniciarCapturaDeAudio();
    this.sendCallback("ok", "init", true);
  }

  public void stopCapture() throws JSONException {
    this.detenerCapturaDeAudio();
    this.sendCallback("ok", "stop", false);
  }

  public void onAmplitudeCallback(float amplitude) throws JSONException {
    this.sendCallback(Float.toString(amplitude), "event", true);
  }

  private native void nativeSetup();

  private native void iniciarCapturaDeAudio();

  private native void detenerCapturaDeAudio();

  //******************************  CORDOVA COMUNICACION **************************************

  private void sendCallback(String result, String action, boolean setKeep) throws JSONException {
    PluginResult res = new PluginResult(PluginResult.Status.OK, getJson(result, action));
    res.setKeepCallback(setKeep);
    this.callbackContext.sendPluginResult(res);
  }

  @NonNull
  private JSONObject getJson(String result, String action) throws JSONException {
    JSONObject obj = new JSONObject();
    obj.put("action", action);
    obj.put("value", result);
    return obj;
  }

}
