package com.gianlu.aria2app.NetIO.Aria2;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Peers extends ArrayList<Peer> {

    public Peers(JSONArray array) throws JSONException {
        super(array.length());
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
