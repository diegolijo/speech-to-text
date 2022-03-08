package com.vayapedal.speechtotext;

import android.app.Activity;
import org.json.JSONArray;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FileManager {

  public static final Map<String, String> MODEL_PATHS = new HashMap<String, String>() {{
    put("en", "vosk-model-small-en-us-0.15");
    put("en-in", "vosk-model-small-en-in-0.4");
    put("cn", "vosk-model-small-cn-0.3");
    put("ru", "vosk-model-small-ru-0.15");
    put("fr", "vosk-model-small-fr-0.22");
    put("de", "vosk-model-small-de-0.15");
    put("es", "vosk-model-small-es-0.3");
    put("pt", "vosk-model-small-pt-0.3");
    put("tr", "vosk-model-small-tr-0.3");
    put("vn", "vosk-model-small-vn-0.3");
    put("it", "vosk-model-small-it-0.4");
    put("nl", "vosk-model-nl-spraakherkenning-0.6-lgraph");
    put("ca", "vosk-model-small-ca-0.4");
    put("fa", "vosk-model-small-fa-0.4");
    put("ph", "vosk-model-tl-ph-generic-0.6");
    put("uk", "vosk-model-small-uk-v3-nano");
    put("kz", "vosk-model-small-kz-0.15");
  }};

  private static final String MODELS_PATH = "/vosk-models";
  private static final String MODEL_ZIP_FILENAME = "model.zip";
  private final Activity activity;

  public FileManager(Activity activity) {
    this.activity = activity;
  }

  public File loadModelDirectory(String locale) {
    return this.getModelDirectory(MODEL_PATHS.get(locale));
  }

  public File getDestinationFile(final String entryName) throws IOException {
    // model files are under a subdirectory, so get the path after the first /
    final String filePath = entryName.substring(entryName.indexOf('/') + 1);
    final String modelPath = entryName.substring(0, entryName.indexOf('/'));
    final File destinationDirectory = getModelDirectory(modelPath);
    // protect from Zip Slip vulnerability (!)
    final File destinationFile = new File(destinationDirectory, filePath);
    if (!destinationDirectory.getCanonicalPath().equals(destinationFile.getCanonicalPath()) &&
      !destinationFile.getCanonicalPath().startsWith(
        destinationDirectory.getCanonicalPath() + File.separator)) {
      throw new IOException("Entry is outside of the target dir: " + entryName);
    }
    return destinationFile;
  }

  public File getModelZipFile() {
    return new File(activity.getExternalCacheDir(), MODEL_ZIP_FILENAME);
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public static void deleteFolder(final File file) {
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

  public JSONArray getSavedModels() {
    JSONArray obj = new JSONArray();
    for (File file : Objects.requireNonNull(new File(activity.getExternalFilesDir(null), MODELS_PATH).listFiles())) {
      if (file.isDirectory()) {
        obj.put(file.getName());
      }
    }
    return obj;
  }

  public File getModelDirectory(String modelPath) {
    return new File(activity.getExternalFilesDir(null), MODELS_PATH + File.separator + modelPath);
  }

}
