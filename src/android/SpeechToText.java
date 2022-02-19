
/*   Diego Santiago 16-02-2022    */


package com.vayapedal.speechtotext;

import android.Manifest;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import java.io.IOException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PermissionHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;

public class SpeechToText extends CordovaPlugin implements RecognitionListener {

  private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 0;
  private final String[] permissions = {Manifest.permission.RECORD_AUDIO};

  private CallbackContext callbackContext;

  private Model model;
  private SpeechService speechService;
  private boolean speechServiceIsEnable = false;
  private boolean speechServiceIsPlaying = false;

  /**
   * Called after plugin construction and fields have been initialized.
   * Prefer to use pluginInitialize instead since there is no value in
   * having parameters on the initialize() function.
   */
  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
  }

  /**
   * Called after plugin construction and fields have been initialized.
   */
  @Override
  protected void pluginInitialize() {
    super.pluginInitialize();
  }

  /**
   * Executes the request.
   * <p>
   * This method is called from the WebView thread. To do a non-trivial amount of work, use:
   * cordova.getThreadPool().execute(runnable);
   * <p>
   * To run on the UI thread, use:
   * cordova.getActivity().runOnUiThread(runnable);
   *
   * @param action          The action to execute.
   * @param args            The exec() arguments.
   * @param callbackContext The callback context used when calling back into JavaScript.
   * @return Whether the action was valid.
   */
  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    this.callbackContext = callbackContext;
    if (action.equalsIgnoreCase("enable")) {
      cordova.getThreadPool().execute(() -> {
        try {
          initRecognizer();
        } catch (Exception e) {
          LOG.e("execute.enable", e.getMessage());
          serdError("execute.enable", e);
        }
      });
    }
    if (action.equalsIgnoreCase("start")) {
      cordova.getThreadPool().execute(() -> {
        try {
          startRecognizer();
        } catch (Exception e) {
          LOG.e("execute.start", e.getMessage());
          serdError("execute.start", e);
        }
      });
    }
    if (action.equalsIgnoreCase("stop")) {
      cordova.getThreadPool().execute(() -> {
        try {
          stopRecognizer();
        } catch (Exception e) {
          LOG.e("execute.stop", e.getMessage());
          serdError("execute.stop", e);
        }
      });
    }
    if (action.equalsIgnoreCase("isEnable")) {
      cordova.getThreadPool().execute(() -> {
        try {
          callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,
            this.speechServiceIsEnable));
        } catch (Exception e) {
          LOG.e("execute.isEnable", e.getMessage());
          serdError("execute.isEnable", e);
        }
      });
    }
    if (action.equalsIgnoreCase("isPlaying")) {
      cordova.getThreadPool().execute(() -> {
        try {
          callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,
            this.speechServiceIsPlaying));
        } catch (Exception e) {
          LOG.e("execute.isPlaying", e.getMessage());
          serdError("execute.isPlaying", e);
        }
      });
    }
    return true;
  }

  // ****************************** CICLO DE VIDA APP *******************************

  /**
   * Called when the system is about to start resuming a previous activity.
   *
   * @param multitasking Flag indicating if multitasking is turned on for app
   */
  @Override
  public void onPause(boolean multitasking) {
    super.onPause(multitasking);
  }

  /**
   * Called when the activity will start interacting with the user.
   *
   * @param multitasking Flag indicating if multitasking is turned on for app
   */
  @Override
  public void onResume(boolean multitasking) {
    super.onResume(multitasking);
  }

  /**
   * Called when the activity is becoming visible to the user.
   */
  @Override
  public void onStart() {
    super.onStart();
  }

  /**
   * Called when the activity is no longer visible to the user.
   */
  @Override
  public void onStop() {
    super.onStop();
  }

  /**
   * The final call you receive before your activity is destroyed.
   */
  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  //******************************** Recognizer ***************************************

  private void initRecognizer() {
    if (!hasPermisssion()) {
      requestPermissions(PERMISSIONS_REQUEST_RECORD_AUDIO);
    } else {
      initModel();
    }
  }

  private void initModel() {
    StorageService.unpack(this.cordova.getContext(), "model-small-es", "model",
      (model) -> {
        this.model = model;
        try {
          initRecognize();
        } catch (JSONException e) {
          serdError("initModel", e);
          e.printStackTrace();
        }
      },
      (exception) -> LOG.e("Failed to unpack the model", exception.getMessage()));
  }

  private void initRecognize() throws JSONException {
    if (speechService != null) {
      speechService.stop();
      speechService = null;
      this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,
        getJson("recognize", "off")));
      speechServiceIsEnable = false;
      speechServiceIsPlaying = false;
    } else {
      try {
        Recognizer rec = new Recognizer(model, 16000.0f);
        speechService = new SpeechService(rec, 16000.0f);
        this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,
          getJson("recognize", "on")));
        speechServiceIsEnable = true;
      } catch (IOException | JSONException e) {
        LOG.e("initRecognize", e.getMessage());
        serdError("initRecognize", e);
      }
    }
  }

  private void startRecognizer() throws JSONException {
    if (speechService != null) {
      speechService.startListening(this);
      speechServiceIsPlaying = true;
    }
  }

  private void stopRecognizer() throws JSONException {
    if (speechService != null) {
      speechService.stop();
      speechServiceIsPlaying = false;
    }
  }

  @NonNull
  private JSONObject getJson(String action, String result) throws JSONException {
    JSONObject obj = new JSONObject();
    obj.put("action", action);
    obj.put("result", result);
    return obj;
  }

  private void serdError(String tag, @NonNull Exception e) {
    this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, tag + e.getMessage()));
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
        this.callbackContext.sendPluginResult(result);
      }
    } catch (JSONException e) {
      serdError("onPartialResult", e);
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
        this.callbackContext.sendPluginResult(result);
      }
    } catch (JSONException e) {
      serdError("onResult", e);
      e.printStackTrace();
    }
    LOG.i("onResult", hypothesis);
  }


  @Override
  public void onError(Exception exception) {
    serdError("onResult", exception);
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

  // ******************************** PERMISOS ***********************************

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
        this.callbackContext.sendPluginResult(result);
        return;
      }
    }
    if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
      initModel();
    }
  }
}
