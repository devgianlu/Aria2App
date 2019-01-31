package com.gianlu.aria2app.Activities.AddDownload;

import android.content.Context;
import android.net.Uri;

import com.gianlu.aria2app.NetIO.Aria2.OptionsMap;

import java.io.Serializable;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AddTorrentBundle extends AddBase64Bundle implements Serializable {
    public final ArrayList<String> uris;

    public AddTorrentBundle(@NonNull String base64, @NonNull String filename, @NonNull Uri fileUri, @Nullable ArrayList<String> uris, @Nullable Integer position, @Nullable OptionsMap options) {
        super(base64, filename, fileUri, position, options);
        this.uris = uris;
    }

    private AddTorrentBundle(@NonNull Context context, @NonNull Uri fileUri, @Nullable Integer position, @Nullable OptionsMap options) throws CannotReadException {
        super(context, fileUri, position, options);
        this.uris = null;
    }

    @NonNull
    public static AddTorrentBundle fromUri(@NonNull Context context, @NonNull Uri uri) throws CannotReadException {
        return new AddTorrentBundle(context, uri, null, null);
    }
}
