package com.gianlu.aria2app.NetIO.Downloader;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.gianlu.commonutils.logging.Logging;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Status;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import okhttp3.HttpUrl;

public class FetchDownloadWrapper {
    private Download download;
    private ProgressBundle progress;

    private FetchDownloadWrapper(@NotNull Download download) {
        this.download = download;
    }

    @NotNull
    public static List<FetchDownloadWrapper> wrap(@NotNull List<Download> downloads) {
        List<FetchDownloadWrapper> wrapped = new ArrayList<>();
        for (Download download : downloads) wrapped.add(new FetchDownloadWrapper(download));
        return wrapped;
    }

    @NotNull
    public static FetchDownloadWrapper wrap(@NotNull Download download) {
        return new FetchDownloadWrapper(download);
    }

    public synchronized void set(@NotNull Download download) {
        this.download = download;
    }

    @NotNull
    public synchronized Download get() {
        return download;
    }

    @NonNull
    public String getDecodedUrl() {
        try {
            return URLDecoder.decode(download.getUrl(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logging.log(ex);
            return download.getUrl();
        }
    }

    public synchronized ProgressBundle progress(long eta, long speed) {
        return progress = new ProgressBundle(eta, speed);
    }

    @NotNull
    public synchronized String getName() {
        return new File(download.getFile()).getName();
    }

    public synchronized boolean is(@NotNull Download download) {
        return this.download.getId() == download.getId();
    }

    @NotNull
    public synchronized Status getStatus() {
        return download.getStatus();
    }

    public synchronized long getDownloaded() {
        return download.getDownloaded();
    }

    public synchronized int getProgress() {
        return download.getProgress();
    }

    @NotNull
    public synchronized File getFile() {
        return new File(download.getFile());
    }

    @NotNull
    public synchronized Uri getUri() {
        return download.getFileUri();
    }

    public synchronized long getSpeed() {
        return progress == null ? 0 : progress.speed;
    }

    public long getEta() {
        return progress == null ? -1 : progress.eta;
    }

    @NonNull
    public HttpUrl getUrl() {
        return HttpUrl.get(download.getUrl());
    }

    public class ProgressBundle {
        public final long eta;
        public final long speed;

        private ProgressBundle(long eta, long speed) {
            this.eta = eta;
            this.speed = speed;
        }
    }
}
