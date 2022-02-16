/**
 * Diego Santiago 14-02-2022
 */
package com.vayapedal.speechtotext;

import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;

public class SpeechToText extends CordovaPlugin implements RecognitionListener {

    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private Model model;
    private SpeechService speechService;

    private String[] permissions = {Manifest.permission.RECORD_AUDIO};
    private JSONArray requestArgs;
    private CallbackContext callbackContext;


    /**
     * Called after plugin construction and fields have been initialized.
     * Prefer to use pluginInitialize instead since there is no value in
     * having parameters on the initialize() function.
     *
     * @param cordova
     * @param webView
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
        if (action.equalsIgnoreCase("enable")) {
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        initRecognizer();
                        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, "fiesta!!!!!"));
                    } catch (Exception e) {
                        LOG.e("execute.enable", e.getMessage());
                        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, e.getMessage()));
                    }
                }
            });
        }
        return true;
    }


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

    //************************************ VOSK ***************************************


    @Override
    public void onPartialResult(String hypothesis) {
        LOG.i("onPartialResult", hypothesis);
    }

    @Override
    public void onResult(String hypothesis) {
        LOG.i("onResult", hypothesis);
    }

    @Override
    public void onFinalResult(String hypothesis) {
        LOG.i("onFinalResult", hypothesis);
    }

    @Override
    public void onError(Exception exception) {
        LOG.e("onError", exception.getMessage());
    }

    @Override
    public void onTimeout() {
        LOG.i("vosk", "onTimeout");
    }

/*    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        try {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        } catch (JSONException e) {
            LOG.e("onRequestPermissionsResult", e.getMessage());
        }

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Recognizer initialization is a time-consuming and it involves IO,
                // so we execute it in async task
                initModel();
            } else {
                this.cordova.getActivity().finish();
            }
        }
    }*/


    private void initRecognizer() {
        if (!hasPermisssion()) {
            requestPermissions(0);
        } else {
            initModel();
        }
    }


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
                                           int[] grantResults) throws JSONException {
        PluginResult result;
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                LOG.e("Failed to unpack the model", "Permission Denied!");
                result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION);
                this.callbackContext.sendPluginResult(result);
                return;
            }
        }

        switch (requestCode) {
            case 0:
                initModel();
                break;
        }
    }


    private void initModel() {
        StorageService.unpack(this.cordova.getContext(), "model-small-es", "model",
                (model) -> {
                    this.model = model;
                    recognizeMicrophone();
                },
                (exception) -> LOG.e("Failed to unpack the model", exception.getMessage()));
    }

    private void recognizeMicrophone() {
        if (speechService != null) {
            speechService.stop();
            speechService = null;
        } else {
            try {
                Recognizer rec = new Recognizer(model, 16000.0f);
                speechService = new SpeechService(rec, 16000.0f);
                speechService.startListening(this);
            } catch (IOException e) {
                LOG.e("recognizeMicrophone", e.getMessage());
            }
        }
    }

}
