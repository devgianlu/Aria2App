package com.gianlu.aria2app.NetIO.JTA2;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class AriaFile implements Serializable {
    public final long completedLength;
    public final long length;
    public final String path;
    public final int index;
    public final HashMap<Status, String> uris;
    public boolean selected;

    @Keep
    public AriaFile(JSONObject obj) throws JSONException {
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

    @Nullable
    public static AriaFile find(List<AriaFile> files, AriaFile item) {
        for (AriaFile file : files)
            if (file.index == item.index && Objects.equals(file.path, item.path))
                return file;

        return null;
    }

    public static String getRelativePath(String path, @NonNull String dir) {
        if (dir.contains("\\")) dir = dir.replaceAll("\\\\", "\\\\");
        String relPath = path.replaceFirst(dir, "");
        if (relPath.charAt(0) == '/') return relPath.substring(1);
        else return relPath;
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
        return index == ariaFile.index && path.equals(ariaFile.path);
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
