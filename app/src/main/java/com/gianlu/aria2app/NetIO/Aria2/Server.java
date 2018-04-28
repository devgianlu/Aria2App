package com.gianlu.aria2app.NetIO.Aria2;


import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class Server {
    public final Uri uri;
    public final String currentUri;
    public final int downloadSpeed;

    Server(JSONObject obj) throws JSONException {
        uri = Uri.parse(obj.getString("uri"));
        currentUri = obj.getString("currentUri");
        downloadSpeed = obj.getInt("downloadSpeed");
    }

    @Nullable
    public static Server find(SparseArray<List<Server>> servers, Server current) {
        for (int i = 0; i < servers.size(); i++)
            for (Server server : servers.valueAt(i))
                if (Objects.equals(server, current))
                    return server;

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Server server = (Server) o;
        return uri.equals(server.uri) || currentUri.equals(server.uri.toString());
    }

    public static class DownloadSpeedComparator implements Comparator<Server> {
        @Override
        public int compare(Server o1, Server o2) {
            if (Objects.equals(o1.downloadSpeed, o2.downloadSpeed)) return 0;
            else if (o1.downloadSpeed > o2.downloadSpeed) return -1;
            else return 1;
        }
    }
}
