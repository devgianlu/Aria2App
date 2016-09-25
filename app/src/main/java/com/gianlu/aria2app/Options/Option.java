package com.gianlu.aria2app.Options;

import android.app.Activity;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class Option implements Serializable {
    String short_option;
    String long_option;
    String def;
    List<String> values;
    TYPE type;

    private Option() {
    }

    @Nullable
    private static Option fromJSON(JSONObject obj) {
        if (obj == null) return null;

        Option opt = new Option();
        opt.short_option = obj.optString("shortOption");
        opt.long_option = obj.optString("longOption");
        opt.def = obj.optString("def");
        JSONArray jValues = obj.optJSONArray("values");
        if (jValues != null) {
            opt.values = new ArrayList<>();
            for (int i = 0; i < jValues.length(); i++) {
                opt.values.add(jValues.optString(i));
            }
        }
        opt.type = obj.isNull("type") ? TYPE.STRING : TYPE.valueOf(obj.optString("type"));
        return opt;
    }

    static Map<String, Option> fromJSONtoMap(JSONObject obj) {
        Map<String, Option> map = new HashMap<>();

        Iterator<String> iter = obj.keys();
        while (iter.hasNext()) {
            String key = iter.next();

            map.put(key, fromJSON(obj.optJSONObject(key)));
        }

        return map;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    static Map<String, Option> loadOptionsMap(final Activity context) {
        try {
            return (Map<String, Option>) new ObjectInputStream(context.openFileInput("options.ser")).readObject();
        } catch (Exception ex) {
            return null;
        }
    }

    enum TYPE {
        BOOLEAN,
        PATH_DIR,
        PATH_FILE,
        MULTICHOICHE,
        STRING
    }
}
