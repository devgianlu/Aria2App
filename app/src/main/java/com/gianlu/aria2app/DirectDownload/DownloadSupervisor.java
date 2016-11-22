package com.gianlu.aria2app.DirectDownload;

import android.util.Base64;

import com.gianlu.aria2app.NetIO.JTA2.AFile;
import com.gianlu.aria2app.Profile.DirectDownload;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloader;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DownloadSupervisor {
    private static DownloadSupervisor supervisor;
    private final List<Integer> ids;

    private DownloadSupervisor() {
        ids = new ArrayList<>();
    }

    public static DownloadSupervisor getInstance() {
        if (supervisor == null)
            supervisor = new DownloadSupervisor();

        return supervisor;
    }

    public void start(DirectDownload dd, File localPath, URL remoteURL, AFile file) {
        BaseDownloadTask downloadTask = FileDownloader.getImpl().create(remoteURL.toString());
        downloadTask.setPath(localPath.getAbsolutePath())
                .setCallbackProgressMinInterval(1000)
                .setListener(new FileDownloadListener());

        if (dd.auth)
            downloadTask.addHeader("Authorization", "Basic " + Base64.encodeToString((dd.username + ":" + dd.password).getBytes(), Base64.NO_WRAP));

        downloadTask.start();
        ids.add(downloadTask.getId());
    }
}
