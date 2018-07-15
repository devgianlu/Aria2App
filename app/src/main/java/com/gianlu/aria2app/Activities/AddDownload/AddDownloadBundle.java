package com.gianlu.aria2app.Activities.AddDownload;

import android.support.annotation.Nullable;

import java.io.Serializable;
import java.util.HashMap;

public abstract class AddDownloadBundle implements Serializable {
    public final Integer position;
    public final HashMap<String, String> options;

    public AddDownloadBundle(@Nullable Integer position, @Nullable HashMap<String, String> options) {
        this.position = position;
        this.options = options;
    }
}
