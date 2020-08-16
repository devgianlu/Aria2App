package com.gianlu.aria2app.downloader;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tonyodev.fetch2.Download;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import okhttp3.HttpUrl;

public final class DdDownload {
    private final Download fetchDownload;
    private final FtpHelper.DownloadRunnable ftpDownload;
    private ProgressBundle progress;

    private DdDownload(@Nullable Download fetchDownload, @Nullable FtpHelper.DownloadRunnable ftpDownload) {
        this.fetchDownload = fetchDownload;
        this.ftpDownload = ftpDownload;
    }

    @NonNull
    public static DdDownload wrap(@NonNull Download download) {
        return new DdDownload(download, null);
    }

    @NonNull
    public static DdDownload wrap(@NonNull FtpHelper.DownloadRunnable download) {
        return new DdDownload(null, download);
    }

    @Nullable
    public Download unwrapFetch() {
        return fetchDownload;
    }

    @Nullable
    public FtpHelper.DownloadRunnable unwrapFtp() {
        return ftpDownload;
    }

    public synchronized ProgressBundle progress(long eta, long speed) {
        return progress = new ProgressBundle(eta, speed);
    }

    @NotNull
    public synchronized String getName() {
        String name = getFile().getName();

        try {
            return URLDecoder.decode(name, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            return name;
        }
    }

    public synchronized boolean is(@NotNull DdDownload other) {
        if (fetchDownload != null && other.fetchDownload != null)
            return fetchDownload.getId() == other.fetchDownload.getId();
        else if (ftpDownload != null && other.ftpDownload != null)
            return ftpDownload.id == other.ftpDownload.id;
        else
            throw new IllegalStateException();
    }

    @NotNull
    public synchronized Status getStatus() {
        if (fetchDownload != null) return Status.from(fetchDownload.getStatus());
        else if (ftpDownload != null) return ftpDownload.status;
        else throw new IllegalStateException();
    }

    public synchronized long getDownloaded() {
        if (fetchDownload != null) return fetchDownload.getDownloaded();
        else if (ftpDownload != null) return ftpDownload.downloaded;
        else throw new IllegalStateException();
    }

    public synchronized int getProgress() {
        if (fetchDownload != null) return fetchDownload.getProgress();
        else if (ftpDownload != null) return ftpDownload.getProgress();
        else throw new IllegalStateException();
    }

    @NotNull
    public synchronized File getFile() {
        if (fetchDownload != null) return new File(fetchDownload.getFile());
            // FIXME else if (ftpDownload != null) return ftpDownload.file;
        else throw new IllegalStateException();
    }

    @NotNull
    public synchronized Uri getUri() {
        if (fetchDownload != null) return fetchDownload.getFileUri();
        else if (ftpDownload != null) return ftpDownload.file.getUri();
        else throw new IllegalStateException();
    }

    public synchronized long getSpeed() {
        return progress == null ? 0 : progress.speed;
    }

    public synchronized long getEta() {
        return progress == null ? -1 : progress.eta;
    }

    @NonNull
    public synchronized String getUrlStr() {
        if (fetchDownload != null) return fetchDownload.getUrl();
        else if (ftpDownload != null) return ftpDownload.getUrl();
        else throw new IllegalStateException();
    }

    @NonNull
    public synchronized String getDecodedUrl() {
        try {
            return URLDecoder.decode(getUrlStr(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            return getUrlStr();
        }
    }

    @NonNull
    public synchronized HttpUrl getUrl() {
        return HttpUrl.get(getUrlStr());
    }

    public enum Status {
        QUEUED, DOWNLOADING,
        PAUSED, COMPLETED,
        CANCELLED, FAILED,
        UNKNOWN;

        @NonNull
        static Status from(@NotNull com.tonyodev.fetch2.Status status) {
            switch (status) {
                case ADDED:
                case DELETED:
                case REMOVED:
                case NONE:
                    return UNKNOWN;
                case QUEUED:
                    return QUEUED;
                case DOWNLOADING:
                    return DOWNLOADING;
                case PAUSED:
                    return PAUSED;
                case COMPLETED:
                    return COMPLETED;
                case CANCELLED:
                    return CANCELLED;
                case FAILED:
                    return FAILED;
                default:
                    throw new IllegalArgumentException("Unknown status: " + status);
            }
        }
    }

    public static class ProgressBundle {
        public final long eta;
        public final long speed;

        private ProgressBundle(long eta, long speed) {
            this.eta = eta;
            this.speed = speed;
        }
    }
}
