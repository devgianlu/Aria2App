package com.gianlu.jtitan.Aria2Helper;

import java.util.Locale;
import java.util.Map;

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

    public static URI_STATUS uriStatusFromString(String status) {
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

    public String getRelativePath(String dir) {
        String relPath = path.replace(dir, "");
        if (relPath.startsWith("/"))
            return relPath;
        else
            return "/" + relPath;
    }

    // Uris
    public enum URI_STATUS {
        USED,
        WAITING
    }
}
