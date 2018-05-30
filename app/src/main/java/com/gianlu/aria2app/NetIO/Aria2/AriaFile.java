package com.gianlu.aria2app.NetIO.Aria2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.webkit.MimeTypeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

import okhttp3.HttpUrl;

public class AriaFile {
    public final long completedLength;
    public final long length;
    public final String path;
    public final Integer index;
    public final HashMap<String, Status> uris;
    public boolean selected;
    private String mime;
    private String name;

    public AriaFile(JSONObject obj) throws JSONException {
        index = obj.getInt("index");
        path = obj.getString("path");
        length = obj.getLong("length");
        completedLength = obj.getLong("completedLength");
        selected = obj.getBoolean("selected");

        uris = new HashMap<>();
        if (obj.has("uris")) {
            JSONArray array = obj.getJSONArray("uris");
            for (int i = 0; i < array.length(); i++) {
                JSONObject uri = array.getJSONObject(i);
                uris.put(uri.getString("uri"), Status.parse(uri.getString("status")));
            }
        }
    }

    public static Integer[] allIndexes(Collection<AriaFile> files) {
        Integer[] indexes = new Integer[files.size()];
        Iterator<AriaFile> iterator = files.iterator();
        for (int i = 0; i < files.size(); i++) indexes[i] = iterator.next().index;
        return indexes;
    }

    @NonNull
    public String getName() {
        if (name == null) {
            int last = path.lastIndexOf(AriaDirectory.SEPARATOR);
            name = path.substring(last + 1);
        }

        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AriaFile ariaFile = (AriaFile) o;
        return index.equals(ariaFile.index) && path.equals(ariaFile.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, index);
    }

    public float getProgress() {
        return ((float) completedLength / (float) length) * 100;
    }

    @NonNull
    public String getRelativePath(@NonNull String dir) {
        return path.substring(dir.length() + 1);
    }

    public boolean completed() {
        return completedLength == length;
    }

    @Nullable
    public String getMimeType() {
        if (mime == null) {
            int dot = path.lastIndexOf('.');
            if (dot >= 0)
                mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(path.substring(dot + 1));
        }

        return mime;
    }

    @NonNull
    public HttpUrl getDownloadUrl(String dir, @NonNull HttpUrl base) {
        HttpUrl.Builder builder = base.newBuilder();
        builder.addPathSegments(getRelativePath(dir));
        return builder.build();
    }

    public enum Status {
        USED,
        WAITING;

        public static Status parse(@Nullable String val) {
            if (val == null) return Status.WAITING;
            switch (val.toLowerCase()) {
                case "used":
                    return Status.USED;
                case "waiting":
                    return Status.WAITING;
                default:
                    return Status.WAITING;
            }
        }
    }
}
