package com.gianlu.aria2app.Activities.AddDownload;

import java.io.Serializable;
import java.util.HashMap;

import androidx.annotation.Nullable;

public abstract class AddDownloadBundle implements Serializable {
    public final Integer position;
    public final HashMap<String, String> options;

    public AddDownloadBundle(@Nullable Integer position, @Nullable HashMap<String, String> options) {
        this.position = position;
        this.options = options;
    }

    public static class CannotReadException extends Exception {
        CannotReadException(String message) {
            super(message);
        }

        CannotReadException(Throwable cause) {
            super(cause);
        }
    }
}
