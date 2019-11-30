package com.gianlu.aria2app.activities.adddownload;

import com.gianlu.aria2app.api.aria2.OptionsMap;

import java.io.Serializable;

import androidx.annotation.Nullable;

public abstract class AddDownloadBundle implements Serializable {
    public final Integer position;
    public final OptionsMap options;

    public AddDownloadBundle(@Nullable Integer position, @Nullable OptionsMap options) {
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
