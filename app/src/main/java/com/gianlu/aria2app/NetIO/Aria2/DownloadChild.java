package com.gianlu.aria2app.NetIO.Aria2;

import android.support.annotation.NonNull;

import java.io.Serializable;

public abstract class DownloadChild implements Serializable {
    protected final DownloadStatic download;

    DownloadChild(@NonNull DownloadStatic download) {
        this.download = download;
    }

    @NonNull
    public DownloadStatic getDownload() { // TODO: Not really useful
        return download;
    }
}
