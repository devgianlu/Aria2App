package com.gianlu.aria2app.NetIO.JTA2;

import com.gianlu.aria2app.Activities.MoreAboutDownload.ServersFragment.Item;

import org.json.JSONObject;

public class Server extends Item {
    public final String uri;
    public final String currentUri;
    public final int downloadSpeed;
    public int membershipIndex;

    public Server(JSONObject obj) {
        uri = obj.optString("uri");
        currentUri = obj.optString("currentUri");
        downloadSpeed = Integer.parseInt(obj.optString("downloadSpeed", "0"));
    }

    @Override
    public int getItemType() {
        return Item.SERVER;
    }
}
