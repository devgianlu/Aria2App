package com.gianlu.aria2app.api.search;

import com.gianlu.commonutils.CommonUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class Torrent {
    public final String engineId;
    public final String title;
    public final String magnet;
    public final String torrentFileUrl;
    public final long size;
    public final int seeders;
    public final int leeches;

    public Torrent(JSONObject obj) throws JSONException {
        engineId = obj.getString("engine");
        title = obj.getString("title");
        magnet = obj.getString("magnet");
        torrentFileUrl = CommonUtils.optString(obj, "torrent");
        size = obj.getLong("size");
        seeders = obj.getInt("seeders");
        leeches = obj.getInt("leeches");
    }
}
