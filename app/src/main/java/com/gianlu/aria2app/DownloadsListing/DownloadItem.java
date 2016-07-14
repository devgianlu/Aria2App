package com.gianlu.aria2app.DownloadsListing;

import com.gianlu.jtitan.Aria2Helper.Download;

import java.util.Locale;

public class DownloadItem {
    public Download download;

    public DownloadItem(Download download) {
        this.download = download;
    }

    public String getDownloadName() {
        return download.getName();
    }

    public String getDownloadGID() {
        return download.GID;
    }

    public Download.STATUS getDownloadStatus() {
        return download.status;
    }

    public Float getDownloadProgress() {
        return download.completedLength.floatValue() / download.length.floatValue() * 100;
    }

    public String getDownloadPercentage() {
        return String.format(Locale.getDefault(), "%.2f", getDownloadProgress()) + " %";
    }

    public Float getDownloadSpeed() {
        return download.downloadSpeed.floatValue();
    }

    public Long getDownloadTime() {
        if (download.downloadSpeed == 0) return 0L;
        return (download.length - download.completedLength) / download.downloadSpeed;
    }
}
