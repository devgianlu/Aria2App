package com.gianlu.aria2app.api.aria2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class OptionsMap extends HashMap<String, OptionsMap.OptionValue> {

    public OptionsMap(@NonNull JSONObject obj) throws JSONException {
        super(obj.length());

        Iterator<String> iter = obj.keys();
        while (iter.hasNext()) {
            String key = iter.next();
            put(key, obj.getString(key));
        }
    }

    public OptionsMap() {
    }

    public String getString(String key, String fallback) {
        OptionsMap.OptionValue val = get(key);
        return val == null ? fallback : val.string();
    }

    @Nullable
    public OptionValue put(String key, String value) {
        if (Objects.equals(key, "header") || Objects.equals(key, "index-out"))
            return put(key, value.split("\\n"));
        else
            return put(key, new OptionValue(value));
    }

    @Nullable
    public OptionValue put(String key, String... values) {
        if (Objects.equals(key, "header") || Objects.equals(key, "index-out"))
            return put(key, new OptionValue(values));
        else
            throw new IllegalArgumentException("Cannot have multiple options for " + key);
    }

    @NonNull
    public JSONObject toJson() throws JSONException {
        if (isEmpty()) return new JSONObject();

        JSONObject json = new JSONObject();
        for (Map.Entry<String, OptionValue> entry : entrySet()) {
            if (entry.getValue().size() > 1)
                json.put(entry.getKey(), entry.getValue().toJsonArray());
            else
                json.put(entry.getKey(), entry.getValue().string());
        }

        return json;
    }

    public static class OptionValue implements Serializable {
        private final String[] values;

        OptionValue(String... values) {
            this.values = values;
        }

        @NonNull
        public String[] values() {
            return values;
        }

        @NonNull
        public String string() {
            if (values.length == 0) return "";
            else if (values.length == 1) return values[0];
            else throw new IllegalStateException("Too many elements!");
        }

        @NonNull
        public String strings(@NonNull String separator) {
            if (values.length == 0) return "";
            else if (values.length == 1) return values[0];
            return CommonUtils.join(values, separator);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OptionValue that = (OptionValue) o;
            return Arrays.equals(values, that.values);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(values);
        }

        public int size() {
            return values.length;
        }

        @NonNull
        JSONArray toJsonArray() {
            JSONArray array = new JSONArray();
            for (String value : values) array.put(value);
            return array;
        }

        public boolean isEmpty() {
            return values.length == 0 || values[0].isEmpty();
        }
    }
}
