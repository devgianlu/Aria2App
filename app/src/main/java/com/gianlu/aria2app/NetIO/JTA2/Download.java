package com.gianlu.aria2app.NetIO.JTA2;

import android.content.Context;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.R;
import com.gianlu.commonutils.Sorting.Filterable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class Download implements Serializable, Filterable<Download.Status> {
    public final String bitfield;
    public final long completedLength;
    public final long length;
    public final long uploadLength;
    public final String dir;
    public final int connections;
    public final String gid;
    public final int numPieces;
    public final long pieceLength;
    public final Status status;
    public final int downloadSpeed;
    public final int uploadSpeed;
    public final ArrayList<AFile> files;
    public final int errorCode;
    public final String errorMessage;
    public final String followedBy;
    public final long verifiedLength;
    public final boolean verifyIntegrityPending;
    // BitTorrent only
    public final boolean seeder;
    public final int numSeeders;
    public final BitTorrent torrent;
    public final String following;
    public final String belongsTo;
    public final String infoHash;

    public Download(JSONObject obj) throws JSONException {
        gid = obj.getString("gid");
        status = Status.parse(obj.getString("status"));
        length = Long.parseLong(obj.getString("totalLength"));
        completedLength = Long.parseLong(obj.getString("completedLength"));
        uploadLength = Long.parseLong(obj.getString("uploadLength"));
        bitfield = obj.optString("bitfield", null);
        downloadSpeed = Integer.parseInt(obj.getString("downloadSpeed"));
        uploadSpeed = Integer.parseInt(obj.getString("uploadSpeed"));
        pieceLength = Long.parseLong(obj.getString("pieceLength"));
        numPieces = Integer.parseInt(obj.getString("numPieces"));
        connections = Integer.parseInt(obj.getString("connections"));
        followedBy = obj.optString("followedBy", null);
        following = obj.optString("following", null);
        belongsTo = obj.optString("belongsTo", null);
        dir = obj.optString("dir", null);
        verifiedLength = Long.parseLong(obj.optString("verifiedLength", "0"));
        verifyIntegrityPending = Boolean.parseBoolean(obj.optString("verifyIntegrityPending", "false"));
        files = new ArrayList<>();

        if (obj.has("files")) {
            JSONArray array = obj.optJSONArray("files");
            for (int i = 0; i < array.length(); i++) files.add(new AFile(array.optJSONObject(i)));
        }

        if (obj.has("bittorrent")) {
            infoHash = obj.optString("infoHash", null);
            numSeeders = Integer.parseInt(obj.getString("numSeeders"));
            seeder = Boolean.parseBoolean(obj.optString("seeder", "false"));
            torrent = new BitTorrent(obj.getJSONObject("bittorrent"));
        } else {
            infoHash = null;
            numSeeders = 0;
            seeder = false;
            torrent = null;
        }

        if (obj.has("errorCode")) {
            errorCode = Integer.parseInt(obj.getString("errorCode"));
            errorMessage = obj.getString("errorMessage");
        } else {
            errorCode = -1;
            errorMessage = null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Download download = (Download) o;
        return gid.equals(download.gid);
    }

    public float shareRatio() {
        if (completedLength == 0) return 0f;
        return ((float) uploadLength) / ((float) completedLength);
    }

    public boolean isMetadata() {
        return getName().startsWith("[METADATA]");
    }

    public boolean isTorrent() {
        return torrent != null;
    }

    public String getName() {
        try {
            if (isTorrent()) {
                if (torrent != null && torrent.name != null) {
                    return torrent.name;
                } else {
                    String[] splitted = files.get(0).path.split("/");
                    return splitted[splitted.length - 1];
                }
            } else {
                String[] splitted = files.get(0).path.split("/");
                if (splitted.length == 1) {
                    if (files.get(0).uris.get(AFile.Status.USED) != null) {
                        return files.get(0).uris.get(AFile.Status.USED);
                    } else if (files.get(0).uris.get(AFile.Status.WAITING) != null) {
                        return files.get(0).uris.get(AFile.Status.WAITING);
                    } else {
                        return "Unknown";
                    }
                } else {
                    return splitted[splitted.length - 1];
                }
            }
        } catch (Exception ex) {
            return "Unknown";
        }
    }

    public float getProgress() {
        return ((float) completedLength) / ((float) length) * 100;
    }

    public long getMissingTime() {
        if (downloadSpeed == 0) return 0;
        return (length - completedLength) / downloadSpeed;
    }

    public boolean isLinked() {
        return following != null;
    }

    public boolean supportsDeselectingFiles() {
        return isTorrent() && files.size() > 1;
    }

    @Override
    public Status getFilterable() {
        return status;
    }

    public enum Status {
        ACTIVE,
        PAUSED,
        WAITING,
        ERROR,
        REMOVED,
        COMPLETE,
        UNKNOWN;

        public static Status parse(@Nullable String val) {
            if (val == null) return Status.UNKNOWN;
            switch (val.toLowerCase()) {
                case "active":
                    return Status.ACTIVE;
                case "paused":
                    return Status.PAUSED;
                case "waiting":
                    return Status.WAITING;
                case "complete":
                    return Status.COMPLETE;
                case "error":
                    return Status.ERROR;
                case "removed":
                    return Status.REMOVED;
                default:
                    return Status.UNKNOWN;
            }
        }

        public static List<String> stringValues() {
            List<String> values = new ArrayList<>();
            for (Status value : values()) values.add(value.name());
            return values;
        }

        public String getFormal(Context context, boolean firstCapital) {
            String val;
            switch (this) {
                case ACTIVE:
                    val = context.getString(R.string.downloadStatus_active);
                    break;
                case PAUSED:
                    val = context.getString(R.string.downloadStatus_paused);
                    break;
                case REMOVED:
                    val = context.getString(R.string.downloadStatus_removed);
                    break;
                case WAITING:
                    val = context.getString(R.string.downloadStatus_waiting);
                    break;
                case ERROR:
                    val = context.getString(R.string.downloadStatus_error);
                    break;
                case COMPLETE:
                    val = context.getString(R.string.downloadStatus_complete);
                    break;
                case UNKNOWN:
                default:
                    val = context.getString(R.string.downloadStatus_unknown);
                    break;
            }

            if (firstCapital) return val;
            else return val.substring(0, 1).toLowerCase() + val.substring(1);
        }
    }

    public static class StatusComparator implements Comparator<Download> {
        @Override
        public int compare(Download o1, Download o2) {
            if (o1.status == o2.status) return 0;
            else if (o1.status.ordinal() < o2.status.ordinal()) return -1;
            else return 1;
        }
    }

    public static class DownloadSpeedComparator implements Comparator<Download> {
        @Override
        public int compare(Download o1, Download o2) {
            if (Objects.equals(o1.downloadSpeed, o2.downloadSpeed)) return 0;
            else if (o1.downloadSpeed > o2.downloadSpeed) return -1;
            else return 1;
        }
    }

    public static class UploadSpeedComparator implements Comparator<Download> {
        @Override
        public int compare(Download o1, Download o2) {
            if (Objects.equals(o1.uploadSpeed, o2.uploadSpeed)) return 0;
            else if (o1.uploadSpeed > o2.uploadSpeed) return -1;
            else return 1;
        }
    }

    public static class LengthComparator implements Comparator<Download> {
        @Override
        public int compare(Download o1, Download o2) {
            if (Objects.equals(o1.length, o2.length)) return 0;
            else if (o1.length > o2.length) return -1;
            else return 1;
        }
    }

    public static class CompletedLengthComparator implements Comparator<Download> {
        @Override
        public int compare(Download o1, Download o2) {
            if (Objects.equals(o1.completedLength, o2.completedLength)) return 0;
            else if (o1.completedLength > o2.completedLength) return -1;
            else return 1;
        }
    }

    public static class ProgressComparator implements Comparator<Download> {
        @Override
        public int compare(Download o1, Download o2) {
            int p1 = (int) o1.getProgress();
            int p2 = (int) o2.getProgress();
            if (p1 == p2) return 0;
            else if (p1 < p2) return 1;
            else return -1;
        }
    }
}
