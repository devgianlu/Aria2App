package com.gianlu.aria2app.NetIO.Aria2;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class DownloadStatic implements Serializable {
    public final String dir;
    public final String gid;
    public final int numPieces;
    public final long pieceLength;
    public final long length;
    public final BitTorrent torrent;

    DownloadStatic(JSONObject obj) throws JSONException {
        gid = obj.getString("gid");
        length = obj.optLong("totalLength", 0);
        pieceLength = obj.optLong("pieceLength", 0);
        numPieces = obj.optInt("numPieces", 0);
        dir = obj.optString("dir", null);
        if (obj.has("bittorrent")) {
            torrent = new BitTorrent(obj.getJSONObject("bittorrent"));
        } else {
            torrent = null;
        }
    }

    public final boolean isTorrent() {
        return torrent != null;
    }
}
