package com.gianlu.aria2app.MoreAboutDownload.FilesFragment;

import android.app.Activity;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadLargeFileListener;

class FileDownloadListener extends FileDownloadLargeFileListener {
    private final Activity context;

    FileDownloadListener(Activity context) {
        this.context = context;
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
    protected void pending(BaseDownloadTask task, long soFarBytes, long totalBytes) {
        System.out.println("FileDownloadListener.pending");
    }

    @Override
    protected void progress(BaseDownloadTask task, long soFarBytes, long totalBytes) {
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
    protected void error(BaseDownloadTask task, Throwable e) {
        System.out.println("FileDownloadListener.error");
    }

    @Override
    protected void warn(BaseDownloadTask task) {
        System.out.println("FileDownloadListener.warn");
    }
}
