package com.gianlu.aria2app.NetIO.Aria2;

import android.support.annotation.NonNull;

import java.io.Serializable;

public abstract class DownloadChild implements Serializable {
    protected final Download download;

    DownloadChild(@NonNull Download download) {
        this.download = download;
    }

    @NonNull
    public Download getDownload() {
        return download;
    }
}
