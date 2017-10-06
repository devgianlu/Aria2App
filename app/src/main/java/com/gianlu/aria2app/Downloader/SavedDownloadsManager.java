package com.gianlu.aria2app.Downloader;

import android.content.Context;

import com.gianlu.commonutils.Logging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;

public class SavedDownloadsManager {
    private static SavedDownloadsManager instance;
    private final File storeFile;
    private JSONArray savedDownloads;

    private SavedDownloadsManager(Context context) {
        storeFile = new File(context.getFilesDir(), "savedDownloads.json");

        load(context);
    }

    public static SavedDownloadsManager get(Context context) {
        if (instance == null) instance = new SavedDownloadsManager(context);
        return instance;
    }

    private void load(Context context) {
        try {
            if (!storeFile.exists()) //noinspection ResultOfMethodCallIgnored
                storeFile.createNewFile();

            try (FileInputStream in = new FileInputStream(storeFile)) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line = reader.readLine();
                if (line == null || line.isEmpty()) line = "[]";
                savedDownloads = new JSONArray(line);
            }
        } catch (IOException | JSONException ex) {
            Logging.logMe(context, ex);
            savedDownloads = new JSONArray();
        }
    }

    private void save(Context context) {
        try (FileOutputStream out = new FileOutputStream(storeFile, false)) {
            out.write(savedDownloads.toString().getBytes());
            out.flush();
        } catch (IOException ex) {
            Logging.logMe(context, ex);
        }
    }

    public void saveState(Context context, int id, URI uri, File tempFile) {
        if (savedDownloads == null) load(context);

        try {
            for (int i = 0; i < savedDownloads.length(); i++)
                if (savedDownloads.getJSONObject(i).getInt("id") == id)
                    return;

            JSONObject obj = new JSONObject();
            obj.put("id", id)
                    .put("uri", uri.toASCIIString())
                    .put("tempFile", tempFile.getAbsolutePath());

            savedDownloads.put(obj);
            save(context);
        } catch (JSONException ex) {
            Logging.logMe(context, ex);
        }
    }
}
