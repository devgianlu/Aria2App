package com.gianlu.jtitan.Aria2Helper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Download {
    // General
    public String bitfield;
    public boolean isBitTorrent;
    public Long completedLength;
    public Long length;
    public Long uploadedLength;
    public String dir;
    public Integer connections;
    public String GID;
    public Integer numPieces;
    public Long pieceLength;
    public STATUS status;
    public Integer downloadSpeed;
    public Integer uploadSpeed;
    public List<File> files;
    public Integer errorCode;
    public String errorMessage;
    public String followedBy;
    // BitTorrent only
    public boolean seeder;
    public Integer numSeeders;
    public String infoHash;
    public BitTorrent bitTorrent;

    // Empty
    public Download() {
    }

    // HTTP(S)/FTP
    public Download(boolean isBitTorrent, String bitfield, Long completedLength, Long length, Long uploadedLength, String dir, Integer connections, String GID, Integer numPieces, Long pieceLength, STATUS status, Integer downloadSpeed, Integer uploadSpeed, List<File> files, Integer errorCode, String errorMessage, String followedBy) {
        this.isBitTorrent = isBitTorrent;
        this.completedLength = completedLength;
        this.bitfield = bitfield;
        this.length = length;
        this.uploadedLength = uploadedLength;
        this.dir = dir;
        this.connections = connections;
        this.GID = GID;
        this.numPieces = numPieces;
        this.pieceLength = pieceLength;
        this.files = files;
        this.status = status;
        this.downloadSpeed = downloadSpeed;
        this.uploadSpeed = uploadSpeed;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.followedBy = followedBy;
        this.bitTorrent = null;
        this.numSeeders = null;
        this.infoHash = null;
        this.seeder = false;
    }

    // BitTorrent
    public Download(boolean isBitTorrent, String bitfield, Long completedLength, Long length, Long uploadedLength, String dir, Integer connections, String GID, Integer numPieces, Long pieceLength, STATUS status, Integer downloadSpeed, Integer uploadSpeed, List<File> files, Integer errorCode, String errorMessage, String followedBy, BitTorrent bitTorrent, boolean seeder, Integer numSeeders, String infoHash) {
        this.isBitTorrent = isBitTorrent;
        this.completedLength = completedLength;
        this.bitfield = bitfield;
        this.length = length;
        this.uploadedLength = uploadedLength;
        this.dir = dir;
        this.connections = connections;
        this.GID = GID;
        this.numPieces = numPieces;
        this.pieceLength = pieceLength;
        this.numSeeders = numSeeders;
        this.infoHash = infoHash;
        this.files = files;
        this.status = status;
        this.downloadSpeed = downloadSpeed;
        this.uploadSpeed = uploadSpeed;
        this.followedBy = followedBy;
        this.bitTorrent = bitTorrent;
        this.seeder = seeder;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public static Download fromString(JSONObject jResult) throws JSONException {
        if (!jResult.isNull("bittorrent")) {
            // BitTorrent
            List<File> files = new ArrayList<>();
            JSONArray jFiles = jResult.getJSONArray("files");
            for (int c = 0; c < jFiles.length(); c++) {
                JSONObject jFile = jFiles.getJSONObject(c);

                Map<File.URI_STATUS, String> uris = new HashMap<>();
                JSONArray jUris = jFile.getJSONArray("uris");
                for (int cc = 0; cc < jUris.length(); cc++) {
                    JSONObject jUri = jUris.getJSONObject(cc);
                    uris.put(File.uriStatusFromString(jUri.optString("status")), jUri.optString("uri"));
                }

                String index = jFile.optString("index");
                String completedLength = jFile.optString("completedLength");
                String length = jFile.optString("length");
                String selected = jFile.optString("selected");

                File file = new File((index == null ? null : Integer.parseInt(index)),
                        jFile.optString("path"),
                        (completedLength == null ? null : Long.parseLong(completedLength)),
                        (length == null ? null : Long.parseLong(length)),
                        (selected != null && Boolean.parseBoolean(selected)),
                        uris);

                files.add(file);
            }

            List<String> announceList = new ArrayList<>();
            JSONArray jAnnounceList = jResult.getJSONObject("bittorrent").getJSONArray("announceList");
            for (int c = 0; c < jAnnounceList.length(); c++) {
                announceList.add(jAnnounceList.getJSONArray(c).optString(0));
            }

            String creationDate = jResult.getJSONObject("bittorrent").optString("creationDate");
            String completedLength = jResult.optString("completedLength");
            String totalLength = jResult.optString("totalLength");
            String uploadLength = jResult.optString("uploadLength");
            String pieceLength = jResult.optString("pieceLength");
            String connections = jResult.optString("connections");
            String errorCode = jResult.optString("errorCode");
            String numPieces = jResult.optString("numPieces");
            String downloadSpeed = jResult.optString("downloadSpeed");
            String uploadSpeed = jResult.optString("uploadSpeed");
            String seeder = jResult.optString("seeder");
            String numSeeders = jResult.optString("numSeeders");

            JSONObject info = jResult.getJSONObject("bittorrent").optJSONObject("info");
            String name;
            if (info == null) {
                name = null;
            } else {
                name = info.optString("name");
            }

            BitTorrent bitTorrent = new BitTorrent(announceList,
                    BitTorrent.modeFromString(jResult.getJSONObject("bittorrent").optString("mode")),
                    jResult.getJSONObject("bittorrent").optString("comment"),
                    (creationDate == null || creationDate.isEmpty() ? null : Integer.parseInt(creationDate)),
                    (name == null ? null : name));

            return new Download(true,
                    jResult.optString("bitfield"),
                    (completedLength == null || completedLength.isEmpty() ? null : Long.parseLong(completedLength)),
                    (totalLength == null || totalLength.isEmpty() ? null : Long.parseLong(totalLength)),
                    (uploadLength == null || uploadLength.isEmpty() ? null : Long.parseLong(uploadLength)),
                    jResult.optString("dir"),
                    (connections == null || connections.isEmpty() ? null : Integer.parseInt(connections)),
                    jResult.optString("gid"),
                    (numPieces == null || numPieces.isEmpty() ? null : Integer.parseInt(numPieces)),
                    (pieceLength == null ? null : Long.parseLong(pieceLength)),
                    Download.statusFromString(jResult.optString("status")),
                    (downloadSpeed == null || downloadSpeed.isEmpty() ? null : Integer.parseInt(downloadSpeed)),
                    (uploadSpeed == null || uploadSpeed.isEmpty() ? null : Integer.parseInt(uploadSpeed)),
                    files,
                    (errorCode == null || errorCode.isEmpty() ? null : Integer.parseInt(errorCode)),
                    jResult.optString("errorMessage"),
                    jResult.optString("followedBy"),
                    bitTorrent,
                    (seeder != null && Boolean.parseBoolean(seeder)),
                    (numSeeders == null || numSeeders.isEmpty() ? null : Integer.parseInt(numSeeders)),
                    jResult.optString("infoHash"));
        } else {
            // HTTP
            List<File> files = new ArrayList<>();
            JSONArray jFiles = jResult.optJSONArray("files");
            for (int c = 0; c < jFiles.length(); c++) {
                JSONObject jFile = jFiles.getJSONObject(c);

                Map<File.URI_STATUS, String> uris = new HashMap<>();
                JSONArray jUris = jFile.getJSONArray("uris");
                for (int cc = 0; cc < jUris.length(); cc++) {
                    JSONObject jUri = jUris.getJSONObject(cc);
                    uris.put(File.uriStatusFromString(jUri.optString("status")), jUri.optString("uri"));
                }

                File file = new File(Integer.parseInt(jFile.optString("index")),
                        jFile.optString("path"),
                        Long.parseLong(jFile.optString("completedLength")),
                        Long.parseLong(jFile.optString("length")),
                        Boolean.parseBoolean(jFile.optString("selected")),
                        uris);

                files.add(file);
            }

            String completedLength = jResult.optString("completedLength");
            String totalLength = jResult.optString("totalLength");
            String uploadLength = jResult.optString("uploadLength");
            String pieceLength = jResult.optString("pieceLength");
            String connections = jResult.optString("connections");
            String errorCode = jResult.optString("errorCode");
            String numPieces = jResult.optString("numPieces");
            String downloadSpeed = jResult.optString("downloadSpeed");
            String uploadSpeed = jResult.optString("uploadSpeed");

            return new Download(false,
                    jResult.optString("bitfield"),
                    (completedLength == null ? null : Long.parseLong(completedLength)),
                    (totalLength == null ? null : Long.parseLong(totalLength)),
                    (uploadLength == null ? null : Long.parseLong(uploadLength)),
                    jResult.optString("dir"),
                    (connections == null ? null : Integer.parseInt(connections)),
                    jResult.optString("gid"),
                    (numPieces == null ? null : Integer.parseInt(numPieces)),
                    (pieceLength == null ? null : Long.parseLong(pieceLength)),
                    Download.statusFromString(jResult.optString("status")),
                    (downloadSpeed == null ? null : Integer.parseInt(downloadSpeed)),
                    (uploadSpeed == null ? null : Integer.parseInt(uploadSpeed)),
                    files,
                    (errorCode == null || errorCode.isEmpty() ? null : Integer.parseInt(errorCode)),
                    jResult.optString("errorMessage"),
                    jResult.optString("followedBy"));
        }
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

    public String getPercentage() {
        return String.format(Locale.getDefault(), "%.2f", (completedLength.floatValue() / length.floatValue() * 100)) + " %";
    }

    // Status
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
