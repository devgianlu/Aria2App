package com.gianlu.aria2app.Activities.Search;

import org.json.JSONException;
import org.json.JSONObject;

public class SearchEngine {
    public final String id;
    public final String name;
    public final String url;
    public final int alexaRank;
    public final boolean proxyed;

    public SearchEngine(JSONObject obj) throws JSONException {
        id = obj.getString("id");
        name = obj.getString("name");
        url = obj.getString("url");
        alexaRank = obj.optInt("alexaRank", -1);
        proxyed = obj.getBoolean("proxyed");
    }
}
