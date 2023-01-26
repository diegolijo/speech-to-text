package com.vayapedal.speechtotext;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

public class Downloads {

  public static final Map<String, String> MODEL_URLS = new HashMap<String, String>() {
    {
      put("en", "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip");
      put("en-in", "https://alphacephei.com/vosk/models/vosk-model-small-en-in-0.4.zip");
      put("cn", "https://alphacephei.com/vosk/models/vosk-model-small-cn-0.3.zip");
      put("ru", "https://alphacephei.com/vosk/models/vosk-model-small-ru-0.15.zip");
      put("fr", "https://alphacephei.com/vosk/models/vosk-model-small-fr-0.22.zip");
      put("de", "https://alphacephei.com/vosk/models/vosk-model-small-de-0.15.zip");
      put("es", "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip");
      put("pt", "https://alphacephei.com/vosk/models/vosk-model-small-pt-0.3.zip");
      put("tr", "https://alphacephei.com/vosk/models/vosk-model-small-tr-0.3.zip");
      put("vn", "https://alphacephei.com/vosk/models/vosk-model-small-vn-0.3.zip");
      put("it", "https://alphacephei.com/vosk/models/vosk-model-small-it-0.4.zip");
      put("nl", "https://alphacephei.com/vosk/models/vosk-model-nl-spraakherkenning-0.6-lgraph.zip");
      put("ca", "https://alphacephei.com/vosk/models/vosk-model-small-ca-0.4.zip");
      put("fa", "https://alphacephei.com/vosk/models/vosk-model-small-fa-0.4.zip");
      put("ph", "https://alphacephei.com/vosk/models/vosk-model-tl-ph-generic-0.6.zip");
      put("uk", "https://alphacephei.com/vosk/models/vosk-model-small-uk-v3-nano.zip");
      put("kz", "https://alphacephei.com/vosk/models/vosk-model-small-kz-0.15.zip");
    }
  };

  private final Activity activity;
  private final FileManager fileManager;
  private CallbackContext callbackContextDownload;
  private BroadcastReceiver downloadingBroadcastReceiver = null;
  private Long currentDownloadId = null;
  private final CompositeDisposable disposables = new CompositeDisposable();

  public Downloads(Activity activity) {
    this.activity = activity;
    fileManager = new FileManager(activity);
  }

  public void download(
    CallbackContext callbackContextDownload,
    String locale,
    final boolean manual
  ) throws JSONException {
    Log.d("download -> ", "***************** Init ****************");
    this.callbackContextDownload = callbackContextDownload;

    final DownloadManager downloadManager = (DownloadManager) activity.getSystemService(
      Context.DOWNLOAD_SERVICE
    );
    if (currentDownloadId == null) {
      if (manual) { // manual download
        try {
          sendCallback(callbackContextDownload, "start", true);
          startDownloadingModel(
            downloadManager,
            locale,
            callbackContextDownload
          );
        } catch (Exception e) {
          e.printStackTrace();
          sendCallback(callbackContextDownload, "error descarga", false);
        }
      } else {
        // loading the model would require downloading it, but the user didn't
        // explicitly tell the voice recognizer to download files, so notify them
        // that a download is required
        // todo onRequiresDownload();
      }
    } else {
      Log.e(
        "download -> ",
        "Vosk model already being downloaded: currentModelDownloadId"
      );
      sendCallback(callbackContextDownload, "busy", false);
    }
  }

  private void startDownloadingModel(
    final DownloadManager downloadManager,
    final String locale,
    CallbackContext callbackContextDownload
  ) {
    // borramos el zip y la carpeta del modelo antes de iniciar la descarga
    final File modelZipFile = fileManager.getModelZipFile();
    //noinspection ResultOfMethodCallIgnored
    modelZipFile.delete();
    File f = fileManager.loadModelDirectory(locale);
    FileManager.deleteFolder(f);

    // build download manager request
    final String modelUrl = MODEL_URLS.get(locale);
    final DownloadManager.Request request = new DownloadManager.Request(
      Uri.parse(modelUrl)
    )
      .setTitle("Modelo Vosk")
      .setDescription("Idioma: " + locale)
      .setDestinationUri(Uri.fromFile(modelZipFile));
    // setup download completion listener

    final IntentFilter filter = new IntentFilter(
      DownloadManager.ACTION_DOWNLOAD_COMPLETE
    );
    downloadingBroadcastReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
          Log.d(
            "startDownloadingModel->",
            "Got intent for downloading broadcast receiver: " + intent
          );
          if (downloadingBroadcastReceiver == null) {
            return;
          }
          // DOWNLOAD_COMPLETE
          if (
            intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
          ) {
            final long id = intent.getLongExtra(
              DownloadManager.EXTRA_DOWNLOAD_ID,
              0
            );

            if (currentDownloadId == null || id != currentDownloadId) {
              Log.w(
                "startDownloadingModel->",
                "Download complete listener notified with unknown id: " + id
              );
              return; // do not unregister broadcast receiver
            }

            if (downloadingBroadcastReceiver != null) {
              Log.d(
                "startDownloadingModel->",
                "Unregistering downloading broadcast receiver"
              );
              activity.unregisterReceiver(downloadingBroadcastReceiver);
              downloadingBroadcastReceiver = null;
            }

            if (
              downloadManager.getMimeTypeForDownloadedFile(currentDownloadId) ==
              null
            ) {
              try {
                sendCallback(
                  callbackContextDownload,
                  "vosk_model_download_err",
                  false
                );
              } catch (JSONException e) {
                e.printStackTrace();
              }
              Log.e("startDownloadingModel->", "Failed to download vosk model");
              downloadManager.remove(currentDownloadId);
              updateCurrentDownloadId(activity, null);
              return;
            }

            Log.d(
              "startDownloadingModel->",
              "Vosk model download complete, extracting from zip"
            );

            disposables.add(
              Completable
                .fromAction(Downloads.this::extractModelZip)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                  () -> {
                    downloadManager.remove(currentDownloadId);
                    updateCurrentDownloadId(activity, null);

                    try {
                      sendCallback(
                        callbackContextDownload,
                        "vosk_model_save",
                        false
                      );
                    } catch (JSONException e) {
                      e.printStackTrace();
                    }
                  },
                  throwable -> {
                    try {
                      sendCallback(
                        callbackContextDownload,
                        "vosk_model_extraction_error",
                        false
                      );
                    } catch (JSONException e) {
                      e.printStackTrace();
                    }
                    downloadManager.remove(currentDownloadId);
                    updateCurrentDownloadId(activity, null);
                  }
                )
            );
          }
        }
      };

    activity.registerReceiver(downloadingBroadcastReceiver, filter);
    Log.d(
      "startDownloadingModel->",
      "Starting vosk model download: " + request
    );
    updateCurrentDownloadId(activity, downloadManager.enqueue(request));
  }

  private void extractModelZip() throws IOException {
    try {
      sendCallback(
        this.callbackContextDownload,
        "vosk_model_extraction_start",
        true
      );
    } catch (JSONException e) {
      e.printStackTrace();
    }
    try (
      final ZipInputStream zipInputStream = new ZipInputStream(
        new FileInputStream(fileManager.getModelZipFile())
      )
    ) {
      ZipEntry entry; // cycles through all entries
      while ((entry = zipInputStream.getNextEntry()) != null) {
        final File destinationFile = fileManager.getDestinationFile(
          entry.getName()
        );

        try {
          sendCallback(this.callbackContextDownload, entry.getName(), true);
        } catch (JSONException e) {
          e.printStackTrace();
        }

        if (entry.isDirectory()) {
          // create directory
          if (!destinationFile.mkdirs()) {
            throw new IOException("mkdirs failed: " + destinationFile);
          }
        } else {
          // copy file
          try (
            final BufferedOutputStream outputStream = new BufferedOutputStream(
              new FileOutputStream(destinationFile)
            )
          ) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = zipInputStream.read(buffer)) > 0) {
              outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
          }
        }

        zipInputStream.closeEntry();
      }
    }
  }

  // TODO
  private void updateCurrentDownloadId(final Context context, final Long id) {
    // this field is used anywhere except in static contexts, where the preference is used
    currentDownloadId = id;
    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
      context
    );
    final String downloadIdKey = "vosk_download_id";
    if (id == null) {
      // remove completely, used to notify of null values, check getDownloadIdFromPreferences
      prefs.edit().remove(downloadIdKey).apply();
    } else {
      prefs.edit().putLong(downloadIdKey, id).apply();
    }
  }

  //******************************  CORDOVA COMUNICACION **************************************

  private void sendCallback(
    CallbackContext callbackContextDownload,
    String result,
    boolean keepCallback
  ) throws JSONException {
    PluginResult res = new PluginResult(
      PluginResult.Status.OK,
      getJson(result)
    );
    res.setKeepCallback(keepCallback);
    callbackContextDownload.sendPluginResult(res);
  }

  @NonNull
  private JSONObject getJson(String result) throws JSONException {
    JSONObject obj = new JSONObject();
    obj.put("action", "download");
    obj.put("result", result);
    return obj;
  }
}
