package com.gianlu.aria2app.NetIO.Aria2;

import android.support.annotation.NonNull;

import java.io.Serializable;

public abstract class DownloadChild implements Serializable {
    protected final DownloadWithUpdate download;

    DownloadChild(@NonNull DownloadWithUpdate download) {
        this.download = download;
    }

    @NonNull
    public DownloadWithUpdate getDownload() {
        return download;
    }
}
