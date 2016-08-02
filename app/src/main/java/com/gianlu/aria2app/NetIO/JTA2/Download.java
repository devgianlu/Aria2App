package com.gianlu.aria2app.NetIO.JTA2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Download {
    // General
    public String bitfield;
    public boolean isBitTorrent;
    public Long completedLength;
    public Long length;
    public Long uploadedLength;
    public String dir;
    public Integer connections;
    public String gid;
    public Integer numPieces;
    public Long pieceLength;
    public STATUS status;
    public Integer downloadSpeed;
    public Integer uploadSpeed;
    public List<File> files;
    public Integer errorCode;
    public String errorMessage;
    public String followedBy;
    public String following;
    public String belongsTo;
    public Long verifiedLength;
    public boolean verifyIntegrityPending;

    // BitTorrent only
    public boolean seeder;
    public Integer numSeeders;
    public String infoHash;
    public BitTorrent bitTorrent;

    private Download() {
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

    public static Download fromJSON(JSONObject jResult) {
        Download download = new Download();
        download.gid = jResult.optString("gid");
        download.status = statusFromString(jResult.optString("status"));
        download.length = parseLong(jResult.optString("totalLength"));
        download.completedLength = parseLong(jResult.optString("completedLength"));
        download.uploadedLength = parseLong(jResult.optString("uploadLength"));
        download.bitfield = jResult.optString("bitfield");
        download.downloadSpeed = parseInt(jResult.optString("downloadSpeed"));
        download.uploadSpeed = parseInt(jResult.optString("downloadSpeed"));
        download.pieceLength = parseLong(jResult.optString("pieceLength"));
        download.numPieces = parseInt(jResult.optString("numPieces"));
        download.connections = parseInt(jResult.optString("connections"));
        download.followedBy = jResult.optString("followedBy");
        download.following = jResult.optString("following");
        download.belongsTo = jResult.optString("belongsTo");
        download.dir = jResult.optString("dir");
        download.verifiedLength = parseLong(jResult.optString("verifiedLength"));
        download.verifyIntegrityPending = parseBoolean(jResult.optString("verifyIntegrityPending"));
        download.files = new ArrayList<>();

        if (!jResult.isNull("files")) {
            JSONArray array = jResult.optJSONArray("files");

            for (int i = 0; i < array.length(); i++)
                download.files.add(File.fromJSON(array.optJSONObject(i)));
        }

        if (jResult.isNull("bittorrent")) {
            download.infoHash = jResult.optString("infoHash");
            download.numSeeders = parseInt(jResult.optString("numSeeders"));
            download.seeder = parseBoolean(jResult.optString("seeder"));
            download.bitTorrent = BitTorrent.fromJSON(jResult.optJSONObject("bittorrent"));
        }

        return download;
    }

    public static STATUS statusFromString(String status) {
        if (status == null) return STATUS.UNKNOWN;
        switch (status.toLowerCase()) {
            case "active":
                return STATUS.ACTIVE;
            case "paused":
                return STATUS.PAUSED;
            case "waiting":
                return STATUS.WAITING;
            case "complete":
                return STATUS.COMPLETE;
            case "error":
                return STATUS.ERROR;
            case "removed":
                return STATUS.REMOVED;
            default:
                return STATUS.UNKNOWN;
        }
    }

    public String getName() {
        String downloadName;
        if (this.isBitTorrent) {
            if (this.bitTorrent != null && this.bitTorrent.name != null) {
                downloadName = this.bitTorrent.name;
            } else {
                String[] splitted = this.files.get(0).path.split("/");
                downloadName = splitted[splitted.length - 1];
            }
        } else {
            String[] splitted = this.files.get(0).path.split("/");
            downloadName = splitted[splitted.length - 1];
            /*
            String[] splitted = (this.files.get(0).uris.get(File.URI_STATUS.USED) == null ? this.files.get(0).uris.get(File.URI_STATUS.WAITING).split("/") : this.files.get(0).uris.get(File.URI_STATUS.USED).split("/"));
            downloadName = splitted[splitted.length - 1];
            */
        }

        return downloadName;
    }

    public Float getProgress() {
        return completedLength.floatValue() / length.floatValue() * 100;
    }

    public Long getMissingTime() {
        if (downloadSpeed == 0) return null;
        return (length - completedLength) / downloadSpeed;
    }

    public enum STATUS {
        ACTIVE,
        PAUSED,
        REMOVED,
        WAITING,
        ERROR,
        COMPLETE,
        UNKNOWN
    }
}
