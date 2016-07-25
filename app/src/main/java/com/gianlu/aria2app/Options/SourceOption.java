package com.gianlu.aria2app.Options;

import org.json.JSONException;
import org.json.JSONObject;

public class SourceOption {
    private String name;
    private String nameFormal;
    private DEFAULT_TYPE defaultType;
    private Object defaultVal;

    public SourceOption(String name, String nameFormal, DEFAULT_TYPE defaultType, Object defaultVal) {
        this.name = name;
        this.nameFormal = nameFormal;
        this.defaultType = defaultType;
        this.defaultVal = defaultVal;
    }

    public JSONObject toJSON() throws JSONException {
        return new JSONObject().put("name", name)
                .put("nameFormal", nameFormal)
                .put("defaultType", defaultType.name())
                .put("defaultVal", defaultVal);
    }

    public enum DEFAULT_TYPE {
        INTEGER,
        STRING,
        BOOLEAN,
        NONE
    }
}
