package com.gianlu.aria2app.NetIO.DownloadsManager;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.model.FileDownloadStatus;

public class DDDownload {
    public final String name;
    public final long length;
    public final long completedLength;
    public final Status status;
    public final int downloadSpeed;

    public DDDownload(BaseDownloadTask task) {
        name = task.getFilename();
        length = task.getLargeFileTotalBytes();
        completedLength = task.getLargeFileSoFarBytes();
        status = Status.parse(task.getStatus());
        downloadSpeed = task.getSpeed() * 1024;
    }

    public float getProgress() {
        return ((float) completedLength) / ((float) length) * 100;
    }

    public enum Status {
        COMPLETED,
        ERROR,
        PAUSED,
        RUNNING;

        public static Status parse(byte val) {
            if (FileDownloadStatus.isIng(val))
                return RUNNING;

            switch (val) {
                case FileDownloadStatus.completed:
                    return COMPLETED;
                default:
                case FileDownloadStatus.error:
                    return ERROR;
                case FileDownloadStatus.paused:
                    return PAUSED;
            }
        }
    }
}
