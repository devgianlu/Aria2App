package com.gianlu.aria2app.Activities.AddDownload;

import android.content.Context;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AddUriBundle extends AddDownloadBundle implements Serializable {
    public final ArrayList<String> uris;

    public AddUriBundle(@NonNull ArrayList<String> uris, @Nullable Integer position, @Nullable HashMap<String, String> options) {
        super(position, options);
        this.uris = uris;
    }

    @NonNull
    public static List<AddUriBundle> fromUri(@NonNull Context context, @NonNull Uri uri) throws CannotReadException {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new CannotReadException(new NullPointerException("in is null!"));
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            List<AddUriBundle> bundles = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] split = line.split("\\s");
                bundles.add(new AddUriBundle(new ArrayList<>(Arrays.asList(split)), null, null));
            }
            return bundles;
        } catch (IOException | SecurityException | OutOfMemoryError ex) {
            System.gc();
            throw new CannotReadException(ex);
        }
    }
}
