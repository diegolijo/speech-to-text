package com.vayapedal.speechtotext;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.util.Log;

import java.util.Objects;

public class AudioPlayer {

  private static final String ASSETS_PATH = "www/assets/";
  private static final String DEFAULT_FILE = "sounds/ringtones-knightrider.mp3";
  private final Context context;
  private MediaPlayer m;

  public AudioPlayer(Context context) {
    this.context = context;
  }

  public void play(String path, float vol) {
    try {
      if (m != null  ) {
        m.stop();
        m.release();
        m.stop();
        m = null;
        return;
      }

      m = new MediaPlayer();
      AssetFileDescriptor descriptor = context.getAssets().openFd(ASSETS_PATH + (!"".equals(path) ? path : DEFAULT_FILE));
      m.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
      descriptor.close();

      m.prepare();
      m.setVolume(vol, vol);
      m.start();
      m.setOnCompletionListener(mp -> {
        m = null;
      });
    } catch (Exception e) {
      m = null;
      Log.e("*** error audio play: ", e.getMessage());
    }

  }
}
