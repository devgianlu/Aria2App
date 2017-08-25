package com.gianlu.aria2app.NetIO.DownloadsManager;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;

import com.gianlu.aria2app.NetIO.JTA2.AFile;
import com.gianlu.aria2app.PKeys;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.commonutils.Prefs;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.DownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.model.FileDownloadHeader;
import com.liulishuo.filedownloader.model.FileDownloadStatus;

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
    private static IGlobalMonitor monitor;
    private final FileDownloader downloader;
    private final File downloadPath;
    private final List<DownloadTask> runningTasks;
    private final File ddJournal;
    private IListener listener;

    private DownloadsManager(Context context) {
        downloader = FileDownloader.getImpl();
        downloadPath = new File(Prefs.getString(context, PKeys.DD_DOWNLOAD_PATH, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()));
        runningTasks = new ArrayList<>();
        ddJournal = new File(context.getFilesDir(), "dd.journal");

        loadRunningDownloads();
    }

    public static void setGlobalMonitor(IGlobalMonitor monitor) {
        DownloadsManager.monitor = monitor;
    }

    public static DownloadsManager get(Context context) {
        if (instance == null) instance = new DownloadsManager(context);
        return instance;
    }

    private static void setupAuth(Context context, BaseDownloadTask task) {
        MultiProfile.DirectDownload dd = ProfilesManager.get(context).getCurrent(context).getProfile(context).directDownload;
        if (dd.auth)
            task.addHeader("Authorization", "Basic " + Base64.encodeToString((dd.username + ":" + dd.password).getBytes(), Base64.NO_WRAP));
    }

    private static URI createRemoteUrl(Context context, AFile file, String dir) throws URISyntaxException, MalformedURLException {
        MultiProfile.DirectDownload dd = ProfilesManager.get(context).getCurrent(context).getProfile(context).directDownload;
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

    private DownloadTask JSONToTask(FileDownloader downloader, JSONObject obj) throws JSONException {
        DownloadTask task = (DownloadTask) downloader.create(obj.getString("url"));
        task.setPath(obj.getString("path"), false);

        for (Map.Entry<String, List<String>> header : JSONToHeaders(obj.getJSONObject("headers")).entrySet())
            for (String val : header.getValue())
                task.addHeader(header.getKey(), val);

        baseTaskSetup(task);
        return task;
    }

    public void setListener(IListener listener) {
        this.listener = listener;
        if (listener != null) listener.onDownloadsCountChanged(runningTasks.size());
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
        task.start();
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

                runningTasks.clear();
                for (int i = 0; i < array.length(); i++) {
                    try {
                        DownloadTask task = JSONToTask(downloader, array.getJSONObject(i));
                        task.start();
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (IOException | JSONException | NullPointerException ignored) {
        }
    }

    private void saveRunningDownloads() {
        JSONArray array = new JSONArray();

        for (DownloadTask task : runningTasks) {
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

    private boolean hasTask(BaseDownloadTask task) {
        for (BaseDownloadTask running : runningTasks)
            if (running.getId() == task.getId())
                return true;

        return false;
    }

    @Override
    protected void started(BaseDownloadTask task) {
        if (hasTask(task)) runningTasks.set(indexOf(task), (DownloadTask) task);
        else runningTasks.add((DownloadTask) task);
        saveRunningDownloads();

        if (listener != null) listener.onDownloadsCountChanged(runningTasks.size());
    }

    public int getRunningDownloadsCount() {
        return runningTasks.size();
    }

    public DDDownload getRunningDownloadAt(int i) {
        return new DDDownload(runningTasks.get(i));
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
    protected void error(BaseDownloadTask task, Throwable ex) {
        saveRunningDownloads();
        if (monitor != null) monitor.onException(ex);
    }

    @Override
    protected void warn(BaseDownloadTask task) {

    }

    public void pause(DDDownload download) {
        downloader.pause(download.id);
    }

    public void resume(DDDownload download) {
        int pos = indexOf(download);
        if (pos != -1) {
            DownloadTask task = runningTasks.get(pos);
            if (task.getStatus() == FileDownloadStatus.paused)
                if (task.reuse()) task.start();
        }
    }

    private int indexOf(BaseDownloadTask task) {
        for (int i = 0; i < runningTasks.size(); i++)
            if (runningTasks.get(i).getId() == task.getId())
                return i;

        return -1;
    }

    private int indexOf(DDDownload download) {
        for (int i = 0; i < runningTasks.size(); i++)
            if (runningTasks.get(i).getId() == download.id)
                return i;

        return -1;
    }

    public void remove(DDDownload download) {
        pause(download);

        int pos = indexOf(download);
        if (pos != -1) runningTasks.remove(pos);
    }

    private DownloadTask copyTask(DownloadTask item) {
        DownloadTask copy = (DownloadTask) downloader.create(item.getUrl()).setPath(item.getPath(), false);

        for (Map.Entry<String, List<String>> header : item.getHeader().getHeaders().entrySet())
            for (String val : header.getValue())
                copy.addHeader(header.getKey(), val);

        baseTaskSetup(copy);
        return copy;
    }

    public void restart(DDDownload download) {
        int pos = indexOf(download);
        DownloadTask copy = copyTask(runningTasks.get(pos));
        copy.start();
    }

    public interface IGlobalMonitor {
        void onException(Throwable ex);
    }

    public interface IListener {
        void onDownloadsCountChanged(int count);
    }
}
