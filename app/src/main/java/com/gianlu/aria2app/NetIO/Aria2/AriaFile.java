package com.gianlu.aria2app.NetIO.Aria2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.webkit.MimeTypeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import okhttp3.HttpUrl;

public class AriaFile extends DownloadChild implements Serializable {
    public final long completedLength;
    public final long length;
    public final String path;
    public final Integer index;
    public final HashMap<Status, String> uris;
    public boolean selected;
    private String mime;

    public AriaFile(DownloadStatic download, JSONObject obj) throws JSONException {
        super(download);
        index = obj.getInt("index");
        path = obj.getString("path");
        length = obj.getLong("length");
        completedLength = obj.getLong("completedLength");
        selected = obj.getBoolean("selected");
        uris = new HashMap<>();

        if (obj.has("uris")) {
            JSONArray array = obj.getJSONArray("uris");
            for (int i = 0; i < array.length(); i++)
                uris.put(Status.parse(array.optJSONObject(i).optString("status")), array.optJSONObject(i).optString("uri"));
        }
    }

    private static String getRelativePath(String path, @NonNull String dir) {
        if (dir.contains("\\")) dir = dir.replaceAll("\\\\", "\\\\");
        String relPath = path.replaceFirst(dir, "");
        if (relPath.charAt(0) == '/') return relPath.substring(1);
        else return relPath;
    }

    @NonNull
    public static AriaFile find(List<AriaFile> objs, @NonNull AriaFile match) {
        for (AriaFile obj : objs)
            if (match.equals(obj))
                return obj;

        return match;
    }

    public String getName() {
        String[] splitted = path.split("/");
        return splitted[splitted.length - 1];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AriaFile ariaFile = (AriaFile) o;
        return index.equals(ariaFile.index) && path.equals(ariaFile.path);
    }

    public float getProgress() {
        return ((float) completedLength) / ((float) length) * 100;
    }

    public String getRelativePath(@NonNull String dir) {
        return getRelativePath(path, dir);
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
    public HttpUrl getDownloadUrl(@NonNull HttpUrl base) {
        HttpUrl.Builder builder = base.newBuilder();
        builder.addPathSegments(getRelativePath(download.dir));
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
