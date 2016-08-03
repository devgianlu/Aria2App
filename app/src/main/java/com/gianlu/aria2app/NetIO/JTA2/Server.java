package com.gianlu.aria2app.NetIO.JTA2;

import android.support.annotation.NonNull;

import com.gianlu.aria2app.MoreAboutDownload.ServersFragment.Item;

import org.json.JSONObject;

public class Server extends Item {
    public String uri;
    public String currentUri;
    public int downloadSpeed;

    private Server() {
    }

    @NonNull
    private static Integer parseInt(String val) {
        try {
            return Integer.parseInt(val);
        } catch (Exception ex) {
            return 0;
        }
    }


    public static Server fromJSON(JSONObject jResult) {
        if (jResult == null) return null;

        Server server = new Server();
        server.uri = jResult.optString("uri");
        server.currentUri = jResult.optString("currentUri");
        server.downloadSpeed = parseInt(jResult.optString("downloadSpeed"));

        return server;
    }

    @Override
    public int getItemType() {
        return Item.SERVER;
    }
}
