package com.gianlu.aria2app.NetIO.Aria2;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class Servers extends ArrayList<Server> {

    public Servers(DownloadWithUpdate download, JSONArray servers) throws JSONException {
        for (int i = 0; i < servers.length(); i++)
            add(new Server(download, servers.getJSONObject(i)));
    }

    private Servers() {
    }

    public static Servers empty() {
        return new Servers();
    }
}
