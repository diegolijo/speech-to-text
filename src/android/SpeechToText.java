
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
import org.vosk.android.StorageService;

import java.io.File;
import java.io.IOException;

public class SpeechToText extends CordovaPlugin implements RecognitionListener {

  /* Used to handle permission request */
  private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 0;
  private Model model;
  private SpeechService speechService;
  private boolean speechServiceIsEnable = false;
  private boolean speechServiceIsPlaying = false;
  private static final float SAMPLE_RATE = 44100.0f;

  private final String[] permissions = {Manifest.permission.RECORD_AUDIO};

  private CallbackContext callbackContextPlaying;
  private CallbackContext callbackContextEnabled;
  private CallbackContext callbackContextDownload;

  private Downloads downloads;
  private FileManager fileManager;

  static {
    System.loadLibrary("native_c");
  }

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
          initRecognizer(locale); 
        } catch (Exception e) {
          LOG.e("execute.enable", e.getMessage());
          serdError(this.callbackContextEnabled, "execute.enable", e);
        }
      });
    } else if (action.equalsIgnoreCase("start")) {
      cordova.getThreadPool().execute(() -> {
        try {
          this.callbackContextPlaying = callbackContext;
          startRecognizer();
        } catch (Exception e) {
          LOG.e("execute.start", e.getMessage());
          serdError(this.callbackContextPlaying, "execute.start", e);
        }
      });
    } else if (action.equalsIgnoreCase("stop")) {
      cordova.getThreadPool().execute(() -> {
        try {
          this.callbackContextPlaying = callbackContext;
          stopRecognizer();
        } catch (Exception e) {
          LOG.e("execute.stop", e.getMessage());
          serdError(this.callbackContextPlaying, "execute.stop", e);
        }
      });
    } else if (action.equalsIgnoreCase("isEnable")) {
      cordova.getThreadPool().execute(() -> {
        try {
          callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,
            this.speechServiceIsEnable));
        } catch (Exception e) {
          LOG.e("execute.isEnable", e.getMessage());
          serdError(callbackContext, "execute.isEnable", e);
        }
      });
    } else if (action.equalsIgnoreCase("isPlaying")) {
      cordova.getThreadPool().execute(() -> {
        try {
          callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,
            this.speechServiceIsPlaying));
        } catch (Exception e) {
          LOG.e("execute.isPlaying", e.getMessage());
          serdError(callbackContext, "execute.isPlaying", e);
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
          serdError(callbackContext, "execute.download", e);
        }
      });
    } else if (action.equalsIgnoreCase("getDownloadedLanguages")) {
      cordova.getThreadPool().execute(() -> {
        try {
          callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,
            fileManager.getSavedModels()));
        } catch (Exception e) {
          LOG.e("execute.getDownloadedLanguages", e.getMessage());
          serdError(callbackContext, "execute.getDownloadedLanguages", e);
        }
      });
    } else if (action.equalsIgnoreCase("getAvailableLanguages")) {
      cordova.getThreadPool().execute(() -> {
        try {
          callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,
            new JSONObject(FileManager.MODEL_PATHS)));
        } catch (Exception e) {
          LOG.e("execute.getAvailableLanguages", e.getMessage());
          serdError(callbackContext, "execute.getAvailableLanguages", e);
        }
      });
    }else if (action.equalsIgnoreCase("nativeCall")) {
      cordova.getThreadPool().execute(() -> {
        try {
            String param = args.get(0).toString();

        } catch (Exception e) {
          LOG.e("execute.nativeCall", e.getMessage());
          serdError(callbackContext, "execute.nativeCall", e);
        }
      });
    }
    return true;
  }


  // ****************************** CLICLO DE VIDA APP *********************************

  @Override
  public void onPause(boolean multitasking) {
    super.onPause(multitasking);
  }


  @Override
  public void onResume(boolean multitasking) {
    super.onResume(multitasking);
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

  //******************************** Recognizer ***************************************

  private void initRecognizer(String locale) {
    if (!hasPermisssion()) {
      requestPermissions(PERMISSIONS_REQUEST_RECORD_AUDIO);
    } else {
      File f = fileManager.getModelDirectory(FileManager.MODEL_PATHS.get(locale));
      if (f != null && f.isDirectory()) {
        loadModel(locale);
      } else {
        initModel();
      }
    }
  }

  private void initModel() {
    StorageService.unpack(this.cordova.getContext(), "model-small-es", "model",
      (model) -> {
        this.model = model;
        try {
          initRecognize();
        } catch (JSONException e) {
          serdError(this.callbackContextEnabled, "initRecognize", e);
          e.printStackTrace();
        }
      },
      (exception) -> LOG.e("Failed to unpack the model", exception.getMessage()));
  }

  private void loadModel(String locale) {
    this.model = new Model(fileManager.loadModelDirectory(locale).getAbsolutePath());
    try {
      initRecognize();
    } catch (JSONException e) {
      serdError(this.callbackContextEnabled, "initRecognize", e);
      e.printStackTrace();
    }
  }

  private void initRecognize() throws JSONException {
    if (speechService != null) {
      speechService.stop();
      speechService = null;
      this.callbackContextEnabled.sendPluginResult(new PluginResult(PluginResult.Status.OK,
        getJson("off")));
      speechServiceIsEnable = false;
      speechServiceIsPlaying = false;
    } else {
      try {
        speechService = new SpeechService(new Recognizer(model, SAMPLE_RATE), SAMPLE_RATE);
        this.callbackContextEnabled.sendPluginResult(new PluginResult(PluginResult.Status.OK,
          getJson("on")));
        speechServiceIsEnable = true;
      } catch (IOException | JSONException e) {
        LOG.e("initRecognize", e.getMessage());
        serdError(this.callbackContextEnabled, "initRecognize", e);
      }
    }
  }

  private void startRecognizer() throws JSONException {
    if (speechService != null) {
      speechService.startListening(this);
      speechServiceIsPlaying = true;
    }
    PluginResult result = new PluginResult(PluginResult.Status.OK,
      getJson("play"));
    result.setKeepCallback(true);
    this.callbackContextPlaying.sendPluginResult(result);
  }

  private void stopRecognizer() throws JSONException {
    if (speechService != null) {
      speechService.stop();
      speechServiceIsPlaying = false;
    }
    this.callbackContextPlaying.sendPluginResult(new PluginResult(PluginResult.Status.OK,
      getJson("stop")));
  }


  @NonNull
  private JSONObject getJson(String result) throws JSONException {
    JSONObject obj = new JSONObject();
    obj.put("action", "recognize");
    obj.put("result", result);
    return obj;
  }

  private void serdError(CallbackContext context, String tag, @NonNull Exception e) {
    context.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, tag + e.getMessage()));
  }


  // ******************************** EVENTOS VOSK **************************************
  @Override
  public void onPartialResult(String hypothesis) {
    try {
      JSONObject jsonObject = new JSONObject(hypothesis);
      String parcial = jsonObject.get("partial").toString();
      if (!parcial.equalsIgnoreCase("")) {
        JSONObject obj = new JSONObject().put("parcial", parcial);
        PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
        result.setKeepCallback(true);
        this.callbackContextPlaying.sendPluginResult(result);
      }
    } catch (JSONException e) {
      serdError(this.callbackContextPlaying, "onPartialResult", e);
      e.printStackTrace();
    }
    LOG.i("onPartialResult", hypothesis);
  }

  @Override
  public void onResult(String hypothesis) {
    try {
      JSONObject jsonObject = new JSONObject(hypothesis);
      String texto = jsonObject.get("text").toString();
      if (!texto.equalsIgnoreCase("")) {
        JSONObject obj = new JSONObject().put("texto", texto);
        PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
        result.setKeepCallback(true);
        this.callbackContextPlaying.sendPluginResult(result);
      }
    } catch (JSONException e) {
      serdError(this.callbackContextPlaying, "onResult", e);
      e.printStackTrace();
    }
    LOG.i("onResult", hypothesis);
  }

  @Override
  public void onError(Exception exception) {
    serdError(this.callbackContextPlaying, "onResult", exception);
    LOG.e("onError", exception.getMessage());
  }

  @Override
  public void onFinalResult(String hypothesis) {
    LOG.i("onFinalResult", hypothesis);
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
      initModel();
    }
  }


  private native boolean initAudioStream();

  private native boolean playSeno(Boolean enable);

}
