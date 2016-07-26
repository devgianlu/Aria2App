package com.gianlu.aria2app.Options;

import org.json.JSONException;
import org.json.JSONObject;

public class SourceOption {
    private String name;
    private String nameFormal;
    private String definition;
    private String defaultVal;

    public SourceOption(String name, String nameFormal, String definition, String defaultVal) {
        this.name = name;
        this.nameFormal = nameFormal;
        this.defaultVal = defaultVal;
        this.definition = definition;
    }

    public String getName() {
        return name;
    }

    public JSONObject toJSON() throws JSONException {
        return new JSONObject().put("nameCMD", nameFormal)
                .put("definition", definition)
                .put("defaultVal", defaultVal);
    }

    public enum OPTION_TYPE {
        INTEGER,
        BOOLEAN,
        STRING,
        MULTIPLE
    }
}
