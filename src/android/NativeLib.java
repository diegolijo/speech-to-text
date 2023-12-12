package com.vayapedal.speechtotext;

public class NativeLib {

  static {
    System.loadLibrary("native-lib");
  }

  public native void iniciarCapturaDeAudio();
  public native void detenerCapturaDeAudio();

  public void onAmplitudeCallback(float amplitude) {

  }
}
