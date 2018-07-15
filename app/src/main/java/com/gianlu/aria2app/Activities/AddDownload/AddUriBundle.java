package com.gianlu.aria2app.Activities.AddDownload;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class AddUriBundle extends AddDownloadBundle implements Serializable {
    public final ArrayList<String> uris;

    public AddUriBundle(@NonNull ArrayList<String> uris, @Nullable Integer position, @Nullable HashMap<String, String> options) {
        super(position, options);
        this.uris = uris;
    }
}
