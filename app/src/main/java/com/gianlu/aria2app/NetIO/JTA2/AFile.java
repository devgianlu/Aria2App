package com.gianlu.aria2app.NetIO.JTA2;

import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class AFile implements Serializable {
    public final long completedLength;
    public final long length;
    public final String path;
    public final boolean selected;
    public final int index;
    public final HashMap<Status, String> uris;

    public AFile(JSONObject obj) {
        index = Integer.parseInt(obj.optString("index", "0"));
        path = obj.optString("path");
        length = Long.parseLong(obj.optString("length", "0"));
        completedLength = Long.parseLong(obj.optString("completedLength", "0"));
        selected = Boolean.parseBoolean(obj.optString("selected", "false"));
        uris = new HashMap<>();

        if (obj.has("uris")) {
            JSONArray array = obj.optJSONArray("uris");

            for (int i = 0; i < array.length(); i++)
                uris.put(Status.parse(array.optJSONObject(i).optString("status")), array.optJSONObject(i).optString("uri"));
        }
    }

    @Nullable
    public static AFile find(List<AFile> files, AFile item) {
        for (AFile file : files)
            if (file.index == item.index && Objects.equals(file.path, item.path))
                return file;

        return null;
    }

    public String getName() {
        String[] splitted = path.split("/");
        return splitted[splitted.length - 1];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AFile aFile = (AFile) o;
        return index == aFile.index && path.equals(aFile.path);
    }

    public float getProgress() {
        return ((float) completedLength) / ((float) length) * 100;
    }

    public String getRelativePath(String dir) {
        String relPath = path.replace(dir, "");
        if (relPath.startsWith("/")) return relPath;
        else return "/" + relPath;
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
