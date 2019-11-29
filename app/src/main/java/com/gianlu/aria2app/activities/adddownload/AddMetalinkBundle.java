package com.gianlu.aria2app.activities.adddownload;

import android.content.Context;
import android.net.Uri;

import com.gianlu.aria2app.api.aria2.OptionsMap;

import java.io.Serializable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AddMetalinkBundle extends AddBase64Bundle implements Serializable {
    public AddMetalinkBundle(@NonNull String base64, @NonNull String filename, @NonNull Uri fileUri, @Nullable Integer position, @Nullable OptionsMap options) {
        super(base64, filename, fileUri, position, options);
    }

    private AddMetalinkBundle(@NonNull Context context, @NonNull Uri fileUri, @Nullable Integer position, @Nullable OptionsMap options) throws CannotReadException {
        super(context, fileUri, position, options);
    }

    @NonNull
    public static AddMetalinkBundle fromUri(@NonNull Context context, @NonNull Uri uri) throws CannotReadException {
        return new AddMetalinkBundle(context, uri, null, null);
    }
}
