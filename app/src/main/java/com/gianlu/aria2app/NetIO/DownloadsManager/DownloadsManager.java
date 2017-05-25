package com.gianlu.aria2app.NetIO.DownloadsManager;

import android.content.Context;
import android.os.Environment;
import android.util.Base64;

import com.gianlu.aria2app.NetIO.JTA2.AFile;
import com.gianlu.aria2app.Prefs;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.ProfilesManager.UserProfile;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

// TODO: Downloads manager
public class DownloadsManager {
    private static DownloadsManager instance;
    private final FileDownloader downloader;
    private final File downloadPath;
    private final List<Integer> runningDownloads;

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

    private static URL createRemoteUrl(Context context, AFile file) throws MalformedURLException {
        UserProfile.DirectDownload dd = ProfilesManager.get(context).getCurrentAssert().directDownload;
        URL remoteAddr = dd.getURLAddress();
        return new URL(remoteAddr.getProtocol(), remoteAddr.getHost(), remoteAddr.getPort(), file.getName());
    }

    public void startDownload(Context context, AFile file) throws DownloadsManagerException {
        URL fileUrl;
        try {
            fileUrl = createRemoteUrl(context, file);
        } catch (MalformedURLException ex) {
            throw new DownloadsManagerException(ex);
        }

        BaseDownloadTask task = downloader.create(fileUrl.toString());
        task.setPath(downloadPath.getAbsolutePath(), true);
        setupAuth(context, task);
        int id = task.start();
        runningDownloads.add(id);
    }
}
