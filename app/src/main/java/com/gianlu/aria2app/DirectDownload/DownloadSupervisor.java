package com.gianlu.aria2app.DirectDownload;

import android.util.Base64;

import com.gianlu.aria2app.NetIO.JTA2.AFile;
import com.gianlu.aria2app.Profile.DirectDownload;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadLargeFileListener;
import com.liulishuo.filedownloader.FileDownloader;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

// TODO: Yay
public class DownloadSupervisor extends FileDownloadLargeFileListener {
    private static DownloadSupervisor supervisor;
    private final List<DDDownload> downloads;

    private DownloadSupervisor() {
        downloads = new ArrayList<>();
    }

    public static DownloadSupervisor getInstance() {
        if (supervisor == null)
            supervisor = new DownloadSupervisor();

        return supervisor;
    }

    @Override
    protected void connected(BaseDownloadTask task, String etag, boolean isContinue, long soFarBytes, long totalBytes) {

    }

    @Override
    protected void retry(BaseDownloadTask task, Throwable ex, int retryingTimes, long soFarBytes) {

    }

    @Override
    protected void started(BaseDownloadTask task) {

    }

    @Override
    protected void completed(BaseDownloadTask task) {

    }

    @Override
    protected void error(BaseDownloadTask task, Throwable e) {

    }

    @Override
    protected void warn(BaseDownloadTask task) {

    }

    @Override
    protected void pending(BaseDownloadTask task, long soFarBytes, long totalBytes) {

    }

    @Override
    protected void progress(BaseDownloadTask task, long soFarBytes, long totalBytes) {

    }

    @Override
    protected void paused(BaseDownloadTask task, long soFarBytes, long totalBytes) {

    }

    public void start(DirectDownload dd, File localPath, URL remoteURL, AFile file) {
        BaseDownloadTask downloadTask = FileDownloader.getImpl().create(remoteURL.toString());
        downloadTask.setPath(localPath.getAbsolutePath())
                .setCallbackProgressMinInterval(1000)
                .setListener(this);

        if (dd.auth)
            downloadTask.addHeader("Authorization", "Basic " + Base64.encodeToString((dd.username + ":" + dd.password).getBytes(), Base64.NO_WRAP));

        downloadTask.start();
        downloads.add(DDDownload.fromTask(downloadTask));
    }

    public List<DDDownload> getDownloads() {
        return downloads;
    }

    public void remove(int id) {
        for (DDDownload obj : new ArrayList<>(downloads))
            if (obj.id == id)
                downloads.remove(obj);
    }


}
