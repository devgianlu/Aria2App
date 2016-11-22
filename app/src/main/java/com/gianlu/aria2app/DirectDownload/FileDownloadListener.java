package com.gianlu.aria2app.DirectDownload;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadLargeFileListener;

public class FileDownloadListener extends FileDownloadLargeFileListener {
    @SuppressWarnings("deprecation")
    public FileDownloadListener() {

    }

    @Override
    protected void started(BaseDownloadTask task) {
        System.out.println("FileDownloadListener.started");
    }

    @Override
    protected void connected(BaseDownloadTask task, String etag, boolean isContinue, long soFarBytes, long totalBytes) {
        System.out.println("FileDownloadListener.connected");
    }

    @Override
    protected void retry(BaseDownloadTask task, Throwable ex, int retryingTimes, long soFarBytes) {
        System.out.println("FileDownloadListener.retry");
    }

    @Override
    protected void pending(BaseDownloadTask task, long soFarBytes, long totalBytes) {
        System.out.println("FileDownloadListener.pending");
    }

    @Override
    protected void progress(BaseDownloadTask task, final long soFarBytes, long totalBytes) {
        System.out.println("FileDownloadListener.progress");
    }

    @Override
    protected void paused(BaseDownloadTask task, long soFarBytes, long totalBytes) {
        System.out.println("FileDownloadListener.paused");
    }

    @Override
    protected void completed(BaseDownloadTask task) {
        System.out.println("FileDownloadListener.completed");
    }

    @Override
    protected void error(BaseDownloadTask task, Throwable ex) {
        ex.printStackTrace();
        System.out.println("FileDownloadListener.error");
    }

    @Override
    protected void warn(BaseDownloadTask task) {
        System.out.println("FileDownloadListener.warn");
    }
}
