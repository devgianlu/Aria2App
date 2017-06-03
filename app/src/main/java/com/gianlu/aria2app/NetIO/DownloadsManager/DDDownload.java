package com.gianlu.aria2app.NetIO.DownloadsManager;

import android.content.Context;

import com.gianlu.aria2app.R;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.model.FileDownloadStatus;

public class DDDownload {
    public final String name;
    public final long length;
    public final long completedLength;
    public final Status status;
    public final int downloadSpeed;
    public final Throwable errorCause;

    public DDDownload(BaseDownloadTask task) {
        name = task.getFilename();
        length = task.getLargeFileTotalBytes();
        completedLength = task.getLargeFileSoFarBytes();
        status = Status.parse(task.getStatus());
        downloadSpeed = task.getSpeed() * 1024;
        errorCause = task.getErrorCause();
    }

    public float getProgress() {
        return ((float) completedLength) / ((float) length) * 100;
    }

    public long getMissingTime() {
        if (downloadSpeed == 0) return 0;
        return (length - completedLength) / downloadSpeed;
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

        public String toFormal(Context context) {
            switch (this) {
                case COMPLETED:
                    return context.getString(R.string.completed);
                case ERROR:
                    return context.getString(R.string.downloadStatus_error);
                case PAUSED:
                    return context.getString(R.string.downloadStatus_paused);
                case RUNNING:
                    return context.getString(R.string.downloadStatus_active);
                default:
                    return context.getString(R.string.unknown);
            }
        }
    }
}
