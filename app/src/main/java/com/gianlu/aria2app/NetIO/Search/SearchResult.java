package com.gianlu.aria2app.NetIO.Search;

import org.json.JSONException;
import org.json.JSONObject;

public class SearchResult {
    public final String url;
    public final String title;
    public final Integer seeders;
    public final Integer leeches;
    public final String engineId;

    public SearchResult(JSONObject obj) throws JSONException {
        url = obj.getString("url");
        title = obj.getString("title");
        engineId = obj.getString("engine");

        int seeders = obj.optInt("seeders", -1);
        this.seeders = seeders == -1 ? null : seeders;

        int leeches = obj.optInt("leeches", -1);
        this.leeches = leeches == -1 ? null : leeches;
    }
}
