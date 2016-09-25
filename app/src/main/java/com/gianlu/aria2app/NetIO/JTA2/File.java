package com.gianlu.aria2app.NetIO.JTA2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class File {
    // Global
    public Long completedLength;
    public Long length;
    public String path;
    public boolean selected;
    public Integer index;
    public Map<URI_STATUS, String> uris;

    public File(Integer index, String path, Long completedLength, Long length, boolean selected, Map<URI_STATUS, String> uris) {
        this.path = path;
        this.index = index;
        this.completedLength = completedLength;
        this.length = length;
        this.selected = selected;
        this.uris = uris;
    }

    private File() {
    }

    @Nullable
    private static Integer parseInt(String val) {
        try {
            return Integer.parseInt(val);
        } catch (Exception ex) {
            return 0;
        }
    }

    @Nullable
    private static Long parseLong(String val) {
        try {
            return Long.parseLong(val);
        } catch (Exception ex) {
            return 0L;
        }
    }

    @NonNull
    private static Boolean parseBoolean(String val) {
        try {
            return Boolean.parseBoolean(val);
        } catch (Exception ex) {
            return false;
        }
    }


    private static URI_STATUS uriStatusFromString(String status) {
        if (status == null) return URI_STATUS.WAITING;
        switch (status.toLowerCase()) {
            case "used":
                return URI_STATUS.USED;
            case "waiting":
                return URI_STATUS.WAITING;
            default:
                return URI_STATUS.WAITING;
        }
    }

    public static File fromJSON(JSONObject obj) {
        if (obj == null) return null;

        File file = new File();
        file.index = parseInt(obj.optString("index"));
        file.path = obj.optString("path");
        file.length = parseLong(obj.optString("length"));
        file.completedLength = parseLong(obj.optString("completedLength"));
        file.selected = parseBoolean(obj.optString("selected"));
        file.uris = new HashMap<>();

        if (!obj.isNull("uris")) {
            JSONArray array = obj.optJSONArray("uris");

            for (int i = 0; i < array.length(); i++)
                file.uris.put(uriStatusFromString(array.optJSONObject(i).optString("status")), array.optJSONObject(i).optString("uri"));
        }

        return file;
    }

    public String getName() {
        String[] splitted = path.split("/");
        return splitted[splitted.length - 1];
    }

    public Float getProgress() {
        return completedLength.floatValue() / length.floatValue() * 100;
    }

    public String getPercentage() {
        return String.format(Locale.getDefault(), "%.2f", getProgress()) + " %";
    }

    public boolean isCompleted() {
        return Objects.equals(completedLength, length);
    }

    public String getRelativePath(String dir) {
        String relPath = path.replace(dir, "");
        if (relPath.startsWith("/"))
            return relPath;
        else
            return "/" + relPath;
    }

    public enum URI_STATUS {
        USED,
        WAITING
    }
}
