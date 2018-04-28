package com.gianlu.aria2app.NetIO.Aria2;

import android.support.annotation.NonNull;
import android.util.SparseArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SparseServers extends SparseArray<Servers> {
    public SparseServers(JSONArray array) throws JSONException {
        for (int i = 0; i < array.length(); i++) {
            JSONObject server = array.getJSONObject(i);
            put(server.getInt("index"), new Servers(server.getJSONArray("servers")));
        }
    }

    private SparseServers() {
    }

    @NonNull
    public static SparseServers empty() {
        return new SparseServers();
    }
}
