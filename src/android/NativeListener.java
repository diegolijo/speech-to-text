package com.vayapedal.speechtotext;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONObject;

public class NativeListener {


  private final CallbackContext callbackContect;

  public NativeListener(CallbackContext callbackContext) {
    this.callbackContect = callbackContext;
  }

  public void eventOccurred(float result) {
    PluginResult cordovaResult = new PluginResult(PluginResult.Status.OK,
      result);
    cordovaResult.setKeepCallback(true);
    this.callbackContect.sendPluginResult(cordovaResult);
  }


}
