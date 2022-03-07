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
import androidx.preference.PreferenceManager;    //todo      implementation 'androidx.preference:preference:1.2.0'


import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import io.reactivex.rxjava3.disposables.CompositeDisposable; // todo     implementation "io.reactivex.rxjava3:rxjava:3.0.8"    implementation "io.reactivex.rxjava3:rxandroid:3.0.0"
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Downloads {

  private Activity activity;
  private BroadcastReceiver downloadingBroadcastReceiver = null;
  private Long currentDownloadId = null;
  private final CompositeDisposable disposables = new CompositeDisposable();
  public static final String MODEL_PATH = "/vosk-model";
  public static final String MODEL_ZIP_FILENAME = "model.zip";
  public static final Map<String, String> MODEL_URLS = new HashMap<String, String>() {{
    put("en", "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip");
    put("en-in", "https://alphacephei.com/vosk/models/vosk-model-small-en-in-0.4.zip");
    put("cn", "https://alphacephei.com/vosk/models/vosk-model-small-cn-0.3.zip");
    put("ru", "https://alphacephei.com/vosk/models/vosk-model-small-ru-0.15.zip");
    put("fr", "https://alphacephei.com/vosk/models/vosk-model-small-fr-0.22.zip");
    put("de", "https://alphacephei.com/vosk/models/vosk-model-small-de-0.15.zip");
    put("es", "https://alphacephei.com/vosk/models/vosk-model-small-es-0.3.zip");
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
  }};

  public Downloads(Activity activity) {
    this.activity = activity;
  }


  public void download(CallbackContext callbackContextDownload, String locale, final boolean manual) throws JSONException {
    Log.d("download -> ", "***************** Init ****************");
    final DownloadManager downloadManager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
    if (currentDownloadId == null) {
      Log.d("download -> ", "Aun no se inicio ninguna descarga");
      if (manual) { // manual download
        // the model needs to be downloaded and no download has already started;
        // the user manually triggered the input device, so he surely wants the
        // model to be downloaded, so we can proceed
        try {
          sendCallback(callbackContextDownload, "start" , true);
         /* todo final LocaleResolutionResult result = resolveSupportedLocale(
            LocaleListCompat.create(Sections.getCurrentLocale()),
            MODEL_URLS.keySet());*/
          startDownloadingModel(downloadManager, locale);
        } catch (Exception e) {
          e.printStackTrace();
          // todo   onRequiresDownload();
        }
      } else {
        // loading the model would require downloading it, but the user didn't
        // explicitly tell the voice recognizer to download files, so notify them
        // that a download is required
        // todo onRequiresDownload();
      }
    } else {
      Log.e("download -> ", "Vosk model already being downloaded: currentModelDownloadId");
      sendCallback(callbackContextDownload, "busy" , true);
    }
  }


  private void startDownloadingModel(final DownloadManager downloadManager,
                                     final String language) {
    final File modelZipFile = getModelZipFile();
    //noinspection ResultOfMethodCallIgnored
    modelZipFile.delete(); // if existing, delete the model zip file (should never happen)

    // build download manager request
    final String modelUrl = MODEL_URLS.get(language);
    final DownloadManager.Request request = new DownloadManager.Request(Uri.parse(modelUrl))
      .setTitle("Modelo Vosk").setDescription("Idioma: %1$s")
      .setDestinationUri(Uri.fromFile(modelZipFile));

    // setup download completion listener
    final IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
    downloadingBroadcastReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(final Context context, final Intent intent) {
        Log.d("startDownloadingModel->", "Got intent for downloading broadcast receiver: " + intent);
        if (downloadingBroadcastReceiver == null) {
          return; // just to be sure there are no issues with threads
        }

        if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
          final long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);

          if (currentDownloadId == null || id != currentDownloadId) {
            Log.w("startDownloadingModel->", "Download complete listener notified with unknown id: " + id);
            return; // do not unregister broadcast receiver
          }

          if (downloadingBroadcastReceiver != null) {
            Log.d("startDownloadingModel->", "Unregistering downloading broadcast receiver");
            activity.unregisterReceiver(downloadingBroadcastReceiver);
            downloadingBroadcastReceiver = null;
          }

          if (downloadManager.getMimeTypeForDownloadedFile(currentDownloadId)
            == null) {
            Log.e("startDownloadingModel->", "Failed to download vosk model");
            //todo asyncMakeToast(R.string.vosk_model_download_error);
            downloadManager.remove(currentDownloadId);
            updateCurrentDownloadId(activity, null);
            //todo showstate  onInactive();
            return;
          }

          Log.d("startDownloadingModel->", "Vosk model download complete, extracting from zip");
          disposables.add(Completable
            .fromAction(Downloads.this::extractModelZip)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(() -> {
                //todo   asyncMakeToast(R.string.vosk_model_ready);
                downloadManager.remove(currentDownloadId);
                updateCurrentDownloadId(activity, null);

                // surely the user pressed a button a while ago that
                // triggered the download process, so manual=true
                // todo  load(true);
              },
              throwable -> {
                //todo    asyncMakeToast(R.string.vosk_model_extraction_error);
                //todo throwable.printStackTrace();
                downloadManager.remove(currentDownloadId);
                updateCurrentDownloadId(activity, null);
                //todo showstate  onInactive();
              }));
        }
      }
    };
    activity.registerReceiver(downloadingBroadcastReceiver, filter);

    // launch download
    Log.d("startDownloadingModel->", "Starting vosk model download: " + request);
    updateCurrentDownloadId(activity, downloadManager.enqueue(request));
  }
  ////////////////////
  // File utilities //
  ////////////////////

  private File getDestinationFile(final String entryName) throws IOException {
    // model files are under a subdirectory, so get the path after the first /
    final String filePath = entryName.substring(entryName.indexOf('/') + 1);
    final File destinationDirectory = getModelDirectory();

    // protect from Zip Slip vulnerability (!)
    final File destinationFile = new File(destinationDirectory, filePath);
    if (!destinationDirectory.getCanonicalPath().equals(destinationFile.getCanonicalPath()) &&
      !destinationFile.getCanonicalPath().startsWith(
        destinationDirectory.getCanonicalPath() + File.separator)) {
      throw new IOException("Entry is outside of the target dir: " + entryName);
    }

    return destinationFile;
  }

  private File getModelDirectory() {
    return new File(activity.getFilesDir(), MODEL_PATH);
  }

  private File getModelZipFile() {
    return new File(activity.getExternalFilesDir(null), MODEL_ZIP_FILENAME);
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private static void deleteFolder(final File file) {
    final File[] subFiles = file.listFiles();
    if (subFiles != null) {
      for (final File subFile : subFiles) {
        if (subFile.isDirectory()) {
          deleteFolder(subFile);
        } else {
          subFile.delete();
        }
      }
    }
    file.delete();
  }


  private void extractModelZip() throws IOException {
    //todo asyncMakeToast(R.string.vosk_model_extracting);

    try (final ZipInputStream zipInputStream =
           new ZipInputStream(new FileInputStream(getModelZipFile()))) {
      ZipEntry entry; // cycles through all entries
      while ((entry = zipInputStream.getNextEntry()) != null) {
        final File destinationFile = getDestinationFile(entry.getName());

        if (entry.isDirectory()) {
          // create directory
          if (!destinationFile.mkdirs()) {
            throw new IOException("mkdirs failed: " + destinationFile);
          }

        } else {
          // copy file
          try (final BufferedOutputStream outputStream = new BufferedOutputStream(
            new FileOutputStream(destinationFile))) {
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


  private void updateCurrentDownloadId(final Context context, final Long id) {
    // this field is used anywhere except in static contexts, where the preference is used
    currentDownloadId = id;

    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    final String downloadIdKey = "vosk_download_id";
    if (id == null) {
      // remove completely, used to notify of null values, check getDownloadIdFromPreferences
      prefs.edit().remove(downloadIdKey).apply();
    } else {
      prefs.edit().putLong(downloadIdKey, id).apply();
    }
  }

  //******************************  CORDOVA COMUNICACION **************************************



  private void sendCallback(CallbackContext callbackContextDownload,
                            String result, boolean keepCallback) throws JSONException {
    PluginResult res = new PluginResult(PluginResult.Status.OK,
      getJson(result));
    if (keepCallback) {
      res.setKeepCallback(true);
    }
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
