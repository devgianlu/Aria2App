package com.gianlu.aria2app.NetIO.Aria2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

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

    @Nullable
    public Server find(Server current) {
        for (int i = 0; i < size(); i++)
            for (Server server : valueAt(i))
                if (Objects.equals(server, current))
                    return server;

        return null;
    }
}
