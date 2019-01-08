package com.gianlu.aria2app.NetIO.Aria2;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

import androidx.annotation.NonNull;

public class OptionsMap extends HashMap<String, String> {

    public OptionsMap(@NonNull JSONObject obj) throws JSONException {
        super(obj.length());

        Iterator<String> iter = obj.keys();
        while (iter.hasNext()) {
            String key = iter.next();
            put(key, obj.getString(key));
        }
    }
}
