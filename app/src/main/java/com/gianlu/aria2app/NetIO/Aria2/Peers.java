package com.gianlu.aria2app.NetIO.Aria2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class Peers extends ArrayList<Peer> {

    public Peers(JSONArray array) throws JSONException {
        for (int i = 0; i < array.length(); i++) add(new Peer(array.getJSONObject(i)));
    }

    private Peers() {
    }

    @NonNull
    public static Peers empty() {
        return new Peers();
    }

    @Nullable
    public Peer find(Peer match) {
        for (Peer peer : this)
            if (peer.equals(match))
                return peer;

        return null;
    }
}
