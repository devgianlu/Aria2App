package com.gianlu.aria2app.NetIO.JTA2;

import android.content.Context;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

public class Download implements Serializable {
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
    private final String following;
    private final String belongsTo;
    private final String infoHash;

    public Download(JSONObject obj) {
        gid = obj.optString("gid");
        status = Status.parse(obj.optString("status"));
        length = Long.parseLong(obj.optString("totalLength", "0"));
        completedLength = Long.parseLong(obj.optString("completedLength"));
        uploadLength = Long.parseLong(obj.optString("uploadLength", "0"));
        bitfield = obj.optString("bitfield");
        downloadSpeed = Integer.parseInt(obj.optString("downloadSpeed", "0"));
        uploadSpeed = Integer.parseInt(obj.optString("uploadSpeed", "0"));
        pieceLength = Long.parseLong(obj.optString("pieceLength", "0"));
        numPieces = Integer.parseInt(obj.optString("numPieces", "0"));
        connections = Integer.parseInt(obj.optString("connections", "0"));
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
            infoHash = obj.optString("infoHash");
            numSeeders = Integer.parseInt(obj.optString("numSeeders", "0"));
            seeder = Boolean.parseBoolean(obj.optString("seeder", "false"));
            torrent = new BitTorrent(obj.optJSONObject("bittorrent"));
        } else {
            infoHash = null;
            numSeeders = 0;
            seeder = false;
            torrent = null;
        }

        if (obj.has("errorCode")) {
            errorCode = Integer.parseInt(obj.optString("errorCode", "0"));
            errorMessage = obj.optString("errorMessage");
        } else {
            errorCode = -1;
            errorMessage = null;
        }
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
}
