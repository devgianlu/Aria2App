package com.gianlu.aria2app.Downloader;

import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;

import com.gianlu.aria2app.R;

import java.io.Serializable;

public class DownloadTask implements Serializable {
    public final DownloadStartConfig.Task task;
    public Status status;
    public DownloaderService.DownloaderException ex;
    public long downloaded;
    public long length;
    public float speed;

    public DownloadTask(DownloadStartConfig.Task task) {
        this.task = task;
        this.status = Status.PENDING;
        this.ex = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DownloadTask task1 = (DownloadTask) o;
        return task.equals(task1.task);
    }

    public enum Status {
        PENDING,
        STARTED,
        RUNNING,
        PAUSED,
        FAILED,
        COMPLETED;

        public String getFormal(Context context) {
            switch (this) {
                case PENDING:
                    return context.getString(R.string.pending);
                case STARTED:
                    return context.getString(R.string.started);
                case RUNNING:
                    return context.getString(R.string.running);
                case PAUSED:
                    return context.getString(R.string.paused);
                case FAILED:
                    return context.getString(R.string.failed);
                case COMPLETED:
                    return context.getString(R.string.completed);
                default:
                    return context.getString(R.string.unknown);
            }
        }

        @ColorInt
        public int getColor(Context context) {
            switch (this) {
                case PENDING:
                    return ContextCompat.getColor(context, R.color.downloadPending);
                case STARTED:
                    return ContextCompat.getColor(context, R.color.downloadStarted);
                case RUNNING:
                    return ContextCompat.getColor(context, R.color.downloadRunning);
                case PAUSED:
                    return ContextCompat.getColor(context, R.color.downloadPaused);
                case FAILED:
                    return ContextCompat.getColor(context, R.color.downloadFailed);
                case COMPLETED:
                    return ContextCompat.getColor(context, R.color.downloadCompleted);
                default:
                    return ContextCompat.getColor(context, android.R.color.black);
            }
        }
    }
}
