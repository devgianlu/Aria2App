package com.gianlu.aria2app.downloader;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tonyodev.fetch2.Download;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import okhttp3.HttpUrl;

public final class DdDownload {
    private static final Map<Object, DdDownload> wrappedMap = new HashMap<>(10);
    private final Download fetchDownload;
    private final AbsStreamDownloadHelper.DownloadRunnable streamDownload;
    private ProgressBundle progress;

    private DdDownload(@Nullable Download fetchDownload, @Nullable AbsStreamDownloadHelper.DownloadRunnable streamDownload) {
        this.fetchDownload = fetchDownload;
        this.streamDownload = streamDownload;
    }

    @NonNull
    public static DdDownload wrap(@NonNull Download download) {
        DdDownload wrap = wrappedMap.get(download);
        if (wrap == null) {
            wrap = new DdDownload(download, null);
            wrappedMap.put(download, wrap);
        }

        return wrap;
    }

    @NonNull
    public static DdDownload wrap(@NonNull AbsStreamDownloadHelper.DownloadRunnable download) {
        DdDownload wrap = wrappedMap.get(download);
        if (wrap == null) {
            wrap = new DdDownload(null, download);
            wrappedMap.put(download, wrap);
        }

        return wrap;
    }

    @Nullable
    public Download unwrapFetch() {
        return fetchDownload;
    }

    @Nullable
    public AbsStreamDownloadHelper.DownloadRunnable unwrapStream() {
        return streamDownload;
    }

    /**
     * @param eta   Remaining time in milliseconds
     * @param speed Download speed in bytes per second
     */
    void progress(long eta, long speed) {
        progress = new ProgressBundle(eta, speed);
    }

    @Nullable
    public ProgressBundle progress() {
        return progress;
    }

    @NotNull
    public synchronized String getName() {
        String name;
        if (fetchDownload != null) name = new File(fetchDownload.getFile()).getName();
        else if (streamDownload != null) name = streamDownload.file.getName();
        else throw new IllegalStateException();

        if (name == null) name = "<unknown>";

        try {
            return URLDecoder.decode(name, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            return name;
        }
    }

    public synchronized boolean is(@NotNull DdDownload other) {
        if (fetchDownload != null && other.fetchDownload != null)
            return fetchDownload.getId() == other.fetchDownload.getId();
        else if (streamDownload != null && other.streamDownload != null)
            return streamDownload.id == other.streamDownload.id;
        else
            throw new IllegalStateException();
    }

    @NotNull
    public synchronized Status getStatus() {
        if (fetchDownload != null) return Status.from(fetchDownload.getStatus());
        else if (streamDownload != null) return streamDownload.status;
        else throw new IllegalStateException();
    }

    public synchronized long getDownloaded() {
        if (fetchDownload != null) return fetchDownload.getDownloaded();
        else if (streamDownload != null) return streamDownload.downloaded;
        else throw new IllegalStateException();
    }

    public synchronized int getProgress() {
        if (fetchDownload != null) return fetchDownload.getProgress();
        else if (streamDownload != null) return streamDownload.getProgress();
        else throw new IllegalStateException();
    }

    @NotNull
    public synchronized Uri getUri() {
        if (fetchDownload != null) return fetchDownload.getFileUri();
        else if (streamDownload != null) return streamDownload.file.getUri();
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
        else if (streamDownload != null) return streamDownload.getUrl();
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
