package com.gianlu.aria2app.Downloader;

public class DownloadTask {
    public final DownloadStartConfig.Task task;
    public Status status;
    public DownloaderService.DownloaderException ex;

    public DownloadTask(DownloadStartConfig.Task task) {
        this.task = task;
        this.status = Status.PENDING;
        this.ex = null;
    }

    public enum Status {
        PENDING,
        STARTED,
        FAILED,
        COMPLETED
    }
}
