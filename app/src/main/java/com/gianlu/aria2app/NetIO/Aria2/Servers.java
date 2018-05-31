package com.gianlu.aria2app.NetIO.Aria2;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class Servers extends ArrayList<Server> {

    public Servers(JSONArray servers) throws JSONException {
        for (int i = 0; i < servers.length(); i++)
            add(new Server(servers.getJSONObject(i)));
    }

    private Servers() {
    }

    @NonNull
    public static Servers empty() {
        return new Servers();
    }
}
