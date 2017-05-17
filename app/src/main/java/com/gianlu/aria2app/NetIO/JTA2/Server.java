package com.gianlu.aria2app.NetIO.JTA2;


import android.support.annotation.Nullable;
import android.util.SparseArray;

import org.json.JSONObject;

import java.util.List;
import java.util.Objects;

public class Server {
    public final String uri;
    public final String currentUri;
    public final int downloadSpeed;

    public Server(JSONObject obj) {
        uri = obj.optString("uri");
        currentUri = obj.optString("currentUri");
        downloadSpeed = Integer.parseInt(obj.optString("downloadSpeed", "0"));
    }

    @Nullable
    public static Server find(SparseArray<List<Server>> servers, String uri) {
        for (int i = 0; i < servers.size(); i++) {
            for (Server server : servers.valueAt(i))
                if (Objects.equals(server.uri, uri) || Objects.equals(server.currentUri, uri))
                    return server;
        }

        return null;
    }
}
