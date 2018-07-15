package com.gianlu.aria2app.Activities.AddDownload;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Serializable;
import java.util.HashMap;

public class AddMetalinkBundle extends AddBase64Bundle implements Serializable {
    public AddMetalinkBundle(@NonNull String base64, @NonNull String filename, @Nullable Integer position, @Nullable HashMap<String, String> options) {
        super(base64, filename, position, options);
    }
}
