package com.gianlu.aria2app.NetIO.Downloader;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.NetIO.Aria2.AriaFile;
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithUpdate;

import okhttp3.HttpUrl;

public class AriaFileWrapper {
    private final AriaFile file;
    private final String dir;

    public AriaFileWrapper(@NonNull AriaFile file, @NonNull DownloadWithUpdate download) {
        this.file = file;
        this.dir = download.update().dir;
    }

    @NonNull
    public HttpUrl getDownloadUrl(@NonNull HttpUrl base) {
        return file.getDownloadUrl(dir, base);
    }

    @Nullable
    public String getMimeType() {
        return file.getMimeType();
    }

    @NonNull
    public String getName() {
        return file.getName();
    }
}
