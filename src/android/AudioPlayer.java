package com.vayapedal.speechtotext;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.util.Log;

public class AudioPlayer {

  private static final String ASSETS_PATH = "www/assets/sounds/";
  private final Context context;
  private MediaPlayer m;

  public AudioPlayer(Context context) {
    this.context = context;
  }

  public void play() {
    try {
      if (m != null && m.isPlaying()) {
        m.stop();
        m.release();
      }

      m = new MediaPlayer();
      AssetFileDescriptor descriptor = context.getAssets().openFd(ASSETS_PATH + "bip_bip.mp3"); // TODO ENVIAR DESDE EL JS
      m.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
      descriptor.close();

      m.prepare();
      m.setVolume(1f, 1f);
      m.start();
    } catch (Exception e) {
      Log.e("*** error audio play: ", e.getMessage());
    }

  }
}
