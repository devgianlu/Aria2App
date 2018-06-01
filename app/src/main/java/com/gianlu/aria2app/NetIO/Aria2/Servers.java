package com.gianlu.aria2app.NetIO.Aria2;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class Servers extends ArrayList<Server> {

    public Servers(JSONArray array) throws JSONException {
        super(array.length());
        for (int i = 0; i < array.length(); i++) add(new Server(array.getJSONObject(i)));
    }

    private Servers() {
    }

    @NonNull
    public static Servers empty() {
        return new Servers();
    }
}
