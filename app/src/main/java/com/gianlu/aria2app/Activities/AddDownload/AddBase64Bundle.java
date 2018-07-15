package com.gianlu.aria2app.Activities.AddDownload;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Serializable;
import java.util.HashMap;

public abstract class AddBase64Bundle extends AddDownloadBundle implements Serializable {
    public final String base64;

    public AddBase64Bundle(@NonNull String base64, @Nullable Integer position, @Nullable HashMap<String, String> options) {
        super(position, options);
        this.base64 = base64;
    }
}
