package com.gianlu.aria2app.api.aria2;

import android.util.SparseArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SparseServers extends SparseArray<Servers> {
    public SparseServers(JSONArray array) throws JSONException {
        super(array.length());

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
    public Server find(Server match) {
        for (int i = 0; i < size(); i++)
            for (Server server : valueAt(i))
                if (Objects.equals(server, match))
                    return server;

        return null;
    }
}
