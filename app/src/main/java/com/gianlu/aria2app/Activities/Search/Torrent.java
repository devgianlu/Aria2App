package com.gianlu.aria2app.Activities.Search;

import org.json.JSONException;
import org.json.JSONObject;

public class Torrent {
    public final String title;
    public final String magnet;
    public final String torrentFileUrl;
    public final long size;
    public final int seeders;
    public final int leeches;

    public Torrent(JSONObject obj) throws JSONException {
        title = obj.getString("title");
        magnet = obj.getString("magnet");
        torrentFileUrl = obj.optString("torrent", null);
        size = obj.getLong("size");
        seeders = obj.getInt("seeders");
        leeches = obj.getInt("leeches");
    }
}
