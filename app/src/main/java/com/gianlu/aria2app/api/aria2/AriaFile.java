package com.gianlu.aria2app.api.aria2;

import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import okhttp3.HttpUrl;

public class AriaFile {
    public final long completedLength;
    public final long length;
    public final String path;
    public final Integer index;
    public final Uris uris;
    public boolean selected;
    private String mime;
    private String name;

    public AriaFile(JSONObject obj) throws JSONException {
        index = obj.getInt("index");
        path = obj.getString("path");
        length = obj.getLong("length");
        completedLength = obj.getLong("completedLength");
        selected = obj.getBoolean("selected");


        if (obj.has("uris")) {
            uris = new Uris(obj.getJSONArray("uris"));
        } else {
            uris = Uris.empty();
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
    public String getRelativePath(@NonNull OptionsMap global) {
        OptionsMap.OptionValue dir = global.get("dir");
        String dirStr = dir == null ? null : dir.string();
        if (dirStr == null) dirStr = "";
        return path.substring(dirStr.length() + 1);
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
    public HttpUrl getDownloadUrl(@NonNull OptionsMap global, @NonNull HttpUrl base) {
        HttpUrl.Builder builder = base.newBuilder();
        builder.addPathSegments(getRelativePath(global));
        return builder.build();
    }

    public enum Status {
        USED,
        WAITING;

        public static Status parse(@Nullable String val) {
            if (val != null && "used".equals(val.toLowerCase())) return Status.USED;
            else return Status.WAITING;
        }
    }

    public static class Uris {
        private final String[] uris;
        private final Status[] statuses;

        private Uris() {
            uris = new String[0];
            statuses = new Status[0];
        }

        private Uris(JSONArray array) throws JSONException {
            uris = new String[array.length()];
            statuses = new Status[array.length()];

            for (int i = 0; i < array.length(); i++) {
                JSONObject uri = array.getJSONObject(i);
                uris[i] = uri.getString("uri");
                statuses[i] = Status.parse(uri.getString("status"));
            }
        }

        @NonNull
        private static Uris empty() {
            return new Uris();
        }

        @NonNull
        public List<String> findByStatus(Status match) {
            List<String> list = new ArrayList<>(uris.length);
            for (int i = 0; i < statuses.length; i++)
                if (statuses[i] == match) list.add(uris[i]);
            return list;
        }
    }
}
