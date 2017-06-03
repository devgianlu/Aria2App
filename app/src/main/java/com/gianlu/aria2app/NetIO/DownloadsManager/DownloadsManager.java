package com.gianlu.aria2app.NetIO.DownloadsManager;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;

import com.gianlu.aria2app.NetIO.JTA2.AFile;
import com.gianlu.aria2app.Prefs;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.ProfilesManager.UserProfile;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.DownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.model.FileDownloadHeader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DownloadsManager extends FileDownloadListener {
    private static DownloadsManager instance;
    private final FileDownloader downloader;
    private final File downloadPath;
    private final List<DownloadTask> runningDownloads;
    private final File ddJournal;
    private IListener listener;

    private DownloadsManager(Context context) {
        downloader = FileDownloader.getImpl();
        downloadPath = new File(Prefs.getString(context, Prefs.Keys.DD_DOWNLOAD_PATH, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()));
        runningDownloads = new ArrayList<>();
        ddJournal = new File(context.getFilesDir(), "dd.journal");

        loadRunningDownloads();
    }

    public static DownloadsManager get(Context context) {
        if (instance == null) instance = new DownloadsManager(context);
        return instance;
    }

    private static void setupAuth(Context context, BaseDownloadTask task) {
        UserProfile.DirectDownload dd = ProfilesManager.get(context).getCurrentAssert().directDownload;
        if (dd.auth)
            task.addHeader("Authorization", "Basic " + Base64.encodeToString((dd.username + ":" + dd.password).getBytes(), Base64.NO_WRAP));
    }

    private static URI createRemoteUrl(Context context, AFile file, String dir) throws URISyntaxException, MalformedURLException {
        UserProfile.DirectDownload dd = ProfilesManager.get(context).getCurrentAssert().directDownload;
        URL remoteAddr = dd.getURLAddress();
        return new URI(remoteAddr.getProtocol(), null, remoteAddr.getHost(), remoteAddr.getPort(), file.getRelativePath(dir), null, null);
    }

    @NonNull
    private static String createFileName(AFile file) {
        return file.getName().replaceAll("(#|%|&|\\{|\\}|\\\\|<|>|\\*|\\?|/|\\$|!|'|:|@)", "");
    }

    private static JSONObject taskToJSON(DownloadTask task) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("url", task.getUrl());
        obj.put("path", task.getPath());
        obj.put("headers", headersToJSON(task.getHeader()));
        return obj;
    }

    private static DownloadTask JSONToTask(FileDownloader downloader, JSONObject obj) throws JSONException {
        DownloadTask task = (DownloadTask) downloader.create(obj.getString("url"));
        task.setPath(obj.getString("path"), false);

        for (Map.Entry<String, List<String>> header : JSONToHeaders(obj.getJSONObject("headers")).entrySet())
            for (String val : header.getValue())
                task.addHeader(header.getKey(), val);

        return task;
    }

    public static Map<String, List<String>> JSONToHeaders(JSONObject headers) throws JSONException {
        Map<String, List<String>> map = new HashMap<>();

        Iterator<String> iterator = headers.keys();
        while (iterator.hasNext()) {
            String name = iterator.next();
            JSONArray values = headers.getJSONArray(name);

            List<String> mapValues = map.get(name);
            if (mapValues == null) {
                mapValues = new ArrayList<>();
                map.put(name, mapValues);
            }

            for (int i = 0; i < values.length(); i++) {
                String val = values.getString(i);
                if (!mapValues.contains(val)) mapValues.add(val);
            }
        }

        return map;
    }

    public static JSONObject headersToJSON(@Nullable FileDownloadHeader headersObj) throws JSONException {
        if (headersObj == null) return new JSONObject();
        Map<String, List<String>> headers = headersObj.getHeaders();

        JSONObject obj = new JSONObject();
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            JSONArray values = new JSONArray();
            for (String val : header.getValue()) values.put(val);
            obj.put(header.getKey(), values);
        }

        return obj;
    }

    public void setListener(IListener listener) {
        this.listener = listener;
        if (listener != null) listener.onDownloadsCountChanged(runningDownloads.size());
    }

    public void startDownload(Context context, AFile file, String dir) throws DownloadsManagerException {
        URI fileUrl;
        try {
            fileUrl = createRemoteUrl(context, file, dir);
        } catch (MalformedURLException | URISyntaxException ex) {
            throw new DownloadsManagerException(ex);
        }

        DownloadTask task = (DownloadTask) downloader.create(fileUrl.toASCIIString());
        baseTaskSetup(task);
        task.setPath(new File(downloadPath, createFileName(file)).getAbsolutePath(), false);
        setupAuth(context, task);
        runningDownloads.add(task);
        task.start();

        if (listener != null) listener.onDownloadsCountChanged(runningDownloads.size());
    }

    private void baseTaskSetup(BaseDownloadTask task) {
        task.setCallbackProgressMinInterval(1000);
        task.setListener(this);
        task.setMinIntervalUpdateSpeed(1000);
    }

    private void loadRunningDownloads() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(ddJournal)))) {
            String json = reader.readLine();
            if (json != null && !json.isEmpty()) {
                JSONArray array = new JSONArray(json);

                runningDownloads.clear();
                for (int i = 0; i < array.length(); i++) {
                    try {
                        runningDownloads.add(JSONToTask(downloader, array.getJSONObject(i)));
                    } catch (JSONException ignored) {
                    }
                }
            }
        } catch (IOException | JSONException | NullPointerException ignored) {
        }
    }

    private void saveRunningDownloads() {
        JSONArray array = new JSONArray();

        for (DownloadTask task : runningDownloads) {
            try {
                array.put(taskToJSON(task));
            } catch (JSONException ignored) {
            }
        }

        try (OutputStream out = new FileOutputStream(ddJournal)) {
            out.write(array.toString().getBytes());
            out.flush();
        } catch (IOException ignored) {
        }
    }

    @Override
    protected void started(BaseDownloadTask task) {
        saveRunningDownloads();
    }

    public int getRunningDownloadsCount() {
        return runningDownloads.size();
    }

    public DDDownload getRunningDownloadAt(int i) {
        return new DDDownload(runningDownloads.get(i));
    }

    @Override
    protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {

    }

    @Override
    protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {

    }

    @Override
    protected void completed(BaseDownloadTask task) {
        saveRunningDownloads();
    }

    @Override
    protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
        saveRunningDownloads();
    }

    @Override
    protected void error(BaseDownloadTask task, Throwable e) {
        saveRunningDownloads();
    }

    @Override
    protected void warn(BaseDownloadTask task) {

    }

    public interface IListener {
        void onDownloadsCountChanged(int count);
    }
}
