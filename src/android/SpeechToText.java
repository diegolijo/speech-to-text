
/*   Diego Santiago 20-02-2022    */

package com.vayapedal.speechtotext;

import android.Manifest;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;

import java.io.File;
import java.io.IOException;

public class SpeechToText extends CordovaPlugin implements RecognitionListener {

  /* Used to handle permission request */
  private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 0;
  private Model model;
  private SpeechService speechService;
  private boolean speechServiceIsEnable = false;
  public boolean speechServiceIsPlaying = false;
  public boolean replay = false;
  private static final float SAMPLE_RATE = 44100.0f;

  private final String[] permissions = {Manifest.permission.RECORD_AUDIO};

  private CallbackContext callbackContextPlaying;
  private CallbackContext callbackContextEnabled;
  private CallbackContext callbackContextDownload;
  private CallbackContext callbackSynthesizer;

  private Downloads downloads;
  private FileManager fileManager;
  private TTS tts;
  private AudioPlayer audio;

  private String locale = "";
  private String soundPath;
  private float playVolume;


  /**
   * Called after plugin construction and fields have been initialized.
   * Prefer to use pluginInitialize instead since there is no value in
   * having parameters on the initialize() function.
   */
  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    downloads = new Downloads(cordova.getActivity());
    fileManager = new FileManager(cordova.getActivity());
    tts = new TTS(cordova.getActivity()/*, this*/);
    audio = new AudioPlayer(cordova.getContext());
    // TODO permisos micrófono
  }

  /**
   * Called after plugin construction and fields have been initialized.
   */
  @Override
  protected void pluginInitialize() {
    super.pluginInitialize();
  }


  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (action.equalsIgnoreCase("enable")) {
      cordova.getThreadPool().execute(() -> {
        try {
          this.callbackContextEnabled = callbackContext;
          String locale = args.get(0).toString();
          enableRecognizer(locale);
        } catch (Exception e) {
          LOG.e("execute.enable", e.getMessage());
          serdError(this.callbackContextEnabled, "execute.enable", e.getMessage());
        }
      });
    } else if (action.equalsIgnoreCase("start")) {
      cordova.getThreadPool().execute(() -> {
        try {
          this.callbackContextPlaying = callbackContext;
          startRecognizer();
        } catch (Exception e) {
          LOG.e("execute.start", e.getMessage());
          serdError(this.callbackContextPlaying, "execute.start", e.getMessage());
        }
      });
    } else if (action.equalsIgnoreCase("stop")) {
      cordova.getThreadPool().execute(() -> {
        try {
          this.callbackContextPlaying = callbackContext;
          stopRecognizer();
        } catch (Exception e) {
          LOG.e("execute.stop", e.getMessage());
          serdError(this.callbackContextPlaying, "execute.stop", e.getMessage());
        }
      });
    } else if (action.equalsIgnoreCase("isEnable")) {
      cordova.getThreadPool().execute(() -> {
        try {
          callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, this.speechServiceIsEnable));
        } catch (Exception e) {
          LOG.e("execute.isEnable", e.getMessage());
          serdError(callbackContext, "execute.isEnable", e.getMessage());
        }
      });
    } else if (action.equalsIgnoreCase("isPlaying")) {
      cordova.getThreadPool().execute(() -> {
        try {
          callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,
            this.speechServiceIsPlaying));
        } catch (Exception e) {
          LOG.e("execute.isPlaying", e.getMessage());
          serdError(callbackContext, "execute.isPlaying", e.getMessage());
        }
      });
    } else if (action.equalsIgnoreCase("download")) {
      cordova.getThreadPool().execute(() -> {
        try {
          String locale = args.get(0).toString();
          this.callbackContextDownload = callbackContext;
          donwnload(locale);
        } catch (Exception e) {
          LOG.e("execute.download", e.getMessage());
          serdError(callbackContext, "execute.download", e.getMessage());
        }
      });
    } else if (action.equalsIgnoreCase("getDownloadedLanguages")) {
      cordova.getThreadPool().execute(() -> {
        try {
          callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,
            fileManager.getSavedModels()));
        } catch (Exception e) {
          LOG.e("execute.getDownloadedLanguages", e.getMessage());
          serdError(callbackContext, "execute.getDownloadedLanguages", e.getMessage());
        }
      });
    } else if (action.equalsIgnoreCase("getAvailableLanguages")) {
      cordova.getThreadPool().execute(() -> {
        try {
          callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,
            new JSONObject(FileManager.MODEL_PATHS)));
        } catch (Exception e) {
          LOG.e("execute.getAvailableLanguages", e.getMessage());
          serdError(callbackContext, "execute.getAvailableLanguages", e.getMessage());
        }
      });
    } else if (action.equalsIgnoreCase("speechText")) {
      cordova.getThreadPool().execute(() -> {
        try {
          String text = args.get(0).toString();
          boolean flush = (boolean) args.get(1);
          this.callbackSynthesizer = callbackContext;
          this.speech(text, flush);
        } catch (Exception e) {
          LOG.e("execute.speechText", e.getMessage());
          serdError(callbackContext, "execute.speechText", e.getMessage());
        }
      });
    } else if (action.equalsIgnoreCase("getSpeechVoices")) {
      cordova.getThreadPool().execute(() -> {
        try {
          callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,
            tts.getVoices()));
        } catch (Exception e) {
          LOG.e("execute.getSpeechVoices", e.getMessage());
          serdError(callbackContext, "execute.getSpeechVoices", e.getMessage());
        }
      });
    } else if (action.equalsIgnoreCase("setSpeechVolume")) {
      cordova.getThreadPool().execute(() -> {
        try {
          float value = Float.parseFloat(args.get(0).toString());
          tts.setVolume(value);
          callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, "setSpeechVolume ok"));
        } catch (Exception e) {
          LOG.e("execute.setSpeechVolume", e.getMessage());
          serdError(callbackContext, "execute.setSpeechVolume", e.getMessage());
        }
      });
    } else if (action.equalsIgnoreCase("setSpeechVoice")) {
      cordova.getThreadPool().execute(() -> {
        try {
          String name = args.get(0).toString();
          tts.setVoice(name);
          callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, "getSpeechVoices ok"));
        } catch (Exception e) {
          LOG.e("execute.setSpeechVoice", e.getMessage());
          serdError(callbackContext, "execute.setSpeechVoice", e.getMessage());
        }
      });
    } else if (action.equalsIgnoreCase("setSpeechPitch")) {
      cordova.getThreadPool().execute(() -> {
        try {
          float value = Float.parseFloat(args.get(0).toString());
          tts.setPitch(value);
          callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, "setSpeechPitch ok"));
        } catch (Exception e) {
          LOG.e("execute.setSpeechPitch", e.getMessage());
          serdError(callbackContext, "execute.setSpeechPitch", e.getMessage());
        }
      });
    } else if (action.equalsIgnoreCase("playSound")) {
      cordova.getThreadPool().execute(() -> {
        try {
          soundPath = args.get(0).toString();
          playVolume = Float.parseFloat(args.get(1).toString());
          audio.play(soundPath, playVolume);
          callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, "playSound ok"));
        } catch (Exception e) {
          LOG.e("execute.setSpeechPitch", e.getMessage());
          serdError(callbackContext, "execute.setSpeechPitch", e.getMessage());
        }
      });
    }
    return true;
  }


  // ****************************** CLICLO DE VIDA APP *********************************

  @Override
  public void onPause(boolean multitasking) {
    super.onPause(multitasking);
    try {
      if (speechServiceIsPlaying && speechService != null) {
        speechService.stop();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  @Override
  public void onResume(boolean multitasking) {
    super.onResume(multitasking);
    if (speechServiceIsPlaying) {
      try {
        this.startRecognizer();
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
  }


  @Override
  public void onStart() {
    super.onStart();
  }


  @Override
  public void onStop() {
    super.onStop();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (speechService != null) {
      speechService.stop();
      speechService = null;
    }
  }

  // ********************************** TEXT-TO-SPEECH *********************************
  private void speech(String text, boolean flush) throws JSONException {
    tts.speech(callbackSynthesizer, text, flush);
  }

  //******************************** CONFIG SPEECH TO TEXT *************************************
  private void enableRecognizer(String locale) {
    this.locale = locale;
    if (!hasPermisssion()) {
      requestPermissions(PERMISSIONS_REQUEST_RECORD_AUDIO);
    } else {
      File f = fileManager.getModelDirectory(FileManager.MODEL_PATHS.get(locale));
      if (f != null && f.isDirectory()) {
        loadModel(locale);
      } else {
        //initDefaultModel();
        serdError(this.callbackContextEnabled, "initRecognize", "No se encuentra el model en la carpeta de descargas");
      }
    }
  }

  private void loadModel(String locale) {
    try {
      this.model = new Model(fileManager.loadModelDirectory(locale).getAbsolutePath());
      initRecognize();
    } catch (JSONException e) {
      serdError(this.callbackContextEnabled, "initRecognize", e.getMessage());
      e.printStackTrace();
    }
  }

  private void initRecognize() throws JSONException {
    try {
      if (speechService != null) {
        speechService.stop();
        speechService = null;
        speechServiceIsEnable = false;
        speechServiceIsPlaying = false;
      }
      speechService = new SpeechService(new Recognizer(this.model, SAMPLE_RATE), SAMPLE_RATE);
      this.callbackContextEnabled.sendPluginResult(new PluginResult(PluginResult.Status.OK, getJson("enabled")));
      speechServiceIsEnable = true;
    } catch (IOException | JSONException e) {
      LOG.e("initRecognize", e.getMessage());
      serdError(this.callbackContextEnabled, "initRecognize", e.getMessage());
    }
  }

  public void startRecognizer() throws JSONException {
    if (speechService != null) {
      speechService.startListening(this);
      speechServiceIsPlaying = true;
    }
    try {
      PluginResult result = new PluginResult(PluginResult.Status.OK, getJson("play"));
      result.setKeepCallback(true);
      this.callbackContextPlaying.sendPluginResult(result);
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  public void stopRecognizer() throws JSONException {
    if (!this.speechServiceIsPlaying) {
      PluginResult result = new PluginResult(PluginResult.Status.OK, getJson("stop"));
      this.callbackContextPlaying.sendPluginResult(result);
    }

    if (speechService != null) {
      speechService.stop();
      speechServiceIsPlaying = false;
    }
  }

   /*  private void initDefaultModel() {
    StorageService.unpack(this.cordova.getContext(), "model-small-es", "model",
      (model) -> {
        this.model = model;
        try {
          initRecognize();
        } catch (JSONException e) {
          serdError(this.callbackContextEnabled, "initRecognize", e.getMessage());
          e.printStackTrace();
        }
      },
      (exception) -> LOG.e("Failed to unpack the model", exception.getMessage()));
  }*/

  // ******************************** EVENTOS VOSK **************************************
  @Override
  public void onPartialResult(String hypothesis) {
    try {
      JSONObject jsonObject = new JSONObject(hypothesis);
      String parcial = jsonObject.get("partial").toString();
      if (!parcial.equalsIgnoreCase("")) {
        LOG.i("onPartialResult", hypothesis);
        JSONObject obj = new JSONObject().put("parcial", parcial);
        PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
        result.setKeepCallback(true);
        this.callbackContextPlaying.sendPluginResult(result);
      }
    } catch (JSONException e) {
      serdError(this.callbackContextPlaying, "onPartialResult", e.getMessage());
      e.printStackTrace();
    }

  }

  @Override
  public void onResult(String hypothesis) {
    try {
      JSONObject jsonObject = new JSONObject(hypothesis);
      String texto = jsonObject.get("text").toString();
      if (!texto.equalsIgnoreCase("")) {
        LOG.i("onResult", hypothesis);
        JSONObject obj = new JSONObject().put("texto", texto);
        PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
        result.setKeepCallback(true);
        this.callbackContextPlaying.sendPluginResult(result);
      }
    } catch (JSONException e) {
      serdError(this.callbackContextPlaying, "onResult", e.getMessage());
      e.printStackTrace();
    }
  }

  @Override
  public void onError(Exception exception) {
    serdError(this.callbackContextPlaying, "onResult", exception.getMessage());
    LOG.e("onError", exception.getMessage());
  }

  @Override
  public void onFinalResult(String hypothesis) {
    try {
      PluginResult result = new PluginResult(PluginResult.Status.OK, getJson("stop"));
      this.callbackContextPlaying.sendPluginResult(result);
      audio.play(soundPath, playVolume);
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onTimeout() {
    LOG.i("vosk", "onTimeout");
  }


  // *********************************** DOWNLOADS ****************************************
  private void donwnload(String locale) throws JSONException {
    downloads.download(callbackContextDownload, locale, true);
  }

  // *********************************** PESMISOS *****************************************

  /**
   * check application's permissions
   */
  public boolean hasPermisssion() {
    for (String p : permissions) {
      if (!PermissionHelper.hasPermission(this, p)) {
        return false;
      }
    }
    return true;
  }

  /**
   * We override this so that we can access the permissions variable, which no longer exists in
   * the parent class, since we can't initialize it reliably in the constructor!
   *
   * @param requestCode The code to get request action
   */
  public void requestPermissions(int requestCode) {
    PermissionHelper.requestPermissions(this, requestCode, permissions);
  }

  /**
   * processes the result of permission request
   *
   * @param requestCode  The code to get request action
   * @param permissions  The collection of permissions
   * @param grantResults The result of grant
   */
  // TODO no funciuona en android12
  public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                         int[] grantResults) {
    PluginResult result;
    for (int r : grantResults) {
      if (r == PackageManager.PERMISSION_DENIED) {
        LOG.e("Failed to unpack the model", "Permission Denied!");
        result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION);
        this.callbackContextEnabled.sendPluginResult(result);
        return;
      }
    }

    if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
      enableRecognizer(this.locale);
    }
  }

  // ******************************** UTIL **************************************
  @NonNull
  private JSONObject getJson(String result) throws JSONException {
    JSONObject obj = new JSONObject();
    obj.put("action", "recognize");
    obj.put("result", result);
    return obj;
  }

  private void serdError(CallbackContext context, String tag, String message) {
    context.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, tag + message));
  }

}
