package com.gianlu.aria2app.Activities.AddDownload;

import android.content.Context;
import android.net.Uri;

import com.gianlu.aria2app.NetIO.Aria2.OptionsMap;
import com.gianlu.aria2app.WebView.InterceptedRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AddUriBundle extends AddDownloadBundle implements Serializable {
    public final ArrayList<String> uris;

    public AddUriBundle(@NonNull ArrayList<String> uris, @Nullable Integer position, @Nullable OptionsMap options) {
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

    @NonNull
    public static AddUriBundle fromIntercepted(@NonNull InterceptedRequest req) {
        ArrayList<String> uris = new ArrayList<>();
        uris.add(req.url());

        Map<String, String> headersMap = req.headers();
        List<String> headers = new ArrayList<>(headersMap.size());

        for (Map.Entry<String, String> entry : headersMap.entrySet())
            headers.add(entry.getKey() + ": " + entry.getValue());

        OptionsMap map = new OptionsMap();
        map.put("header", headers.toArray(new String[0]));

        return new AddUriBundle(uris, null, map);
    }
}
