package com.gianlu.aria2app.NetIO.Aria2;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class Peers extends ArrayList<Peer> {

    public Peers(JSONArray array) throws JSONException {
        for (int i = 0; i < array.length(); i++) add(new Peer(array.getJSONObject(i)));
    }

    private Peers() {
    }

    public static Peers empty() {
        return new Peers();
    }
}
