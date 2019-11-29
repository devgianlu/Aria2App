package com.gianlu.aria2app.api.search;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

public class SearchEngine {
    public final String id;
    public final String name;
    public final String url;
    public final int alexaRank;
    public final boolean proxyed;

    private SearchEngine(JSONObject obj) throws JSONException {
        id = obj.getString("id");
        name = obj.getString("name");
        url = obj.getString("url");
        alexaRank = obj.optInt("alexaRank", -1);
        proxyed = obj.getBoolean("proxyed");
    }

    @NonNull
    public static List<SearchEngine> list(JSONArray array) throws JSONException {
        List<SearchEngine> list = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) list.add(new SearchEngine(array.getJSONObject(i)));
        return list;
    }
}
