package com.gianlu.aria2app.Activities.Search;

import org.json.JSONException;
import org.json.JSONObject;

public class MissingSearchEngine {
    public final SearchEngine engine;
    public final int responseCode;

    public MissingSearchEngine(SearchUtils utils, JSONObject obj) throws JSONException {
        engine = utils.findEngine(obj.getString("id"));
        responseCode = obj.getInt("reason");
    }

    @Override
    public String toString() {
        if (engine == null) return "";
        if (engine.proxyed) return engine.name + " (proxyed, returned " + responseCode + ")";
        return engine.name + " (returned " + responseCode + ")";
    }
}
