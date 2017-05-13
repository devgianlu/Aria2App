package com.gianlu.aria2app.NetIO.JTA2;


import org.json.JSONObject;

public class Server {
    public final String uri;
    public final String currentUri;
    public final int downloadSpeed;
    public int membershipIndex;

    public Server(JSONObject obj) {
        uri = obj.optString("uri");
        currentUri = obj.optString("currentUri");
        downloadSpeed = Integer.parseInt(obj.optString("downloadSpeed", "0"));
    }
}
