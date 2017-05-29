package com.gianlu.aria2app.NetIO.DownloadsManager;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Base64;

import com.gianlu.aria2app.NetIO.JTA2.AFile;
import com.gianlu.aria2app.Prefs;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.ProfilesManager.UserProfile;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DownloadsManager {
    private static DownloadsManager instance;
    private final FileDownloader downloader;
    private final File downloadPath;
    private final List<BaseDownloadTask> runningDownloads;
    private IListener listener;

    private DownloadsManager(Context context) {
        downloader = FileDownloader.getImpl();
        downloadPath = new File(Prefs.getString(context, Prefs.Keys.DD_DOWNLOAD_PATH, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()));
        runningDownloads = new ArrayList<>();
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

    public void setListener(IListener listener) {
        this.listener = listener;

        if (listener != null) listener.onDownloadsCountChanged(runningDownloads.size());
    }

    // TODO: Must handle all the exceptions
    public void startDownload(Context context, AFile file, String dir) throws DownloadsManagerException {
        URI fileUrl;
        try {
            fileUrl = createRemoteUrl(context, file, dir);
        } catch (MalformedURLException | URISyntaxException ex) {
            throw new DownloadsManagerException(ex);
        }

        BaseDownloadTask task = downloader.create(fileUrl.toASCIIString());
        task.setCallbackProgressMinInterval(1000);
        task.setMinIntervalUpdateSpeed(1000);
        task.setPath(new File(downloadPath, createFileName(file)).getAbsolutePath(), false);
        setupAuth(context, task);
        task.start();
        runningDownloads.add(task);

        if (listener != null) listener.onDownloadsCountChanged(runningDownloads.size());
    }

    public int getRunningDownloadsCount() {
        return runningDownloads.size();
    }

    public DDDownload getRunningDownloadAt(int i) {
        return new DDDownload(runningDownloads.get(i));
    }

    public interface IListener {
        void onDownloadsCountChanged(int count);
    }
}
