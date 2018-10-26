package com.gianlu.aria2app.NetIO.Downloader;

import android.support.annotation.NonNull;

import com.gianlu.commonutils.Logging;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Status;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class FetchDownloadWrapper {
    private Download download;
    private long eta = -1;
    private long speed = 0;

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

    @NotNull
    public synchronized String getName() {
        return new File(download.getFile()).getName();
    }

    public synchronized boolean is(@NotNull Download download) {
        return download.equals(this.download);
    }

    @NotNull
    public synchronized Status getStatus() {
        return download.getStatus();
    }

    public synchronized long getSpeed() {
        return speed;
    }

    public synchronized void setSpeed(long speed) {
        this.speed = speed;
    }

    public synchronized long getEta() {
        return eta;
    }

    public synchronized void setEta(long eta) {
        this.eta = eta;
    }

    public synchronized long getDownloaded() {
        return download.getDownloaded();
    }

    public synchronized int getProgress() {
        return download.getProgress();
    }
}
