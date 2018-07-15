package com.gianlu.aria2app.Activities.AddDownload;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class AddTorrentBundle extends AddBase64Bundle implements Serializable {
    public final ArrayList<String> uris;

    public AddTorrentBundle(@NonNull String base64, @NonNull String filename, @Nullable ArrayList<String> uris, @Nullable Integer position, @Nullable HashMap<String, String> options) {
        super(base64, filename, position, options);
        this.uris = uris;
    }
}
