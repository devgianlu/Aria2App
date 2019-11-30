package com.gianlu.aria2app.api.aria2;

import org.json.JSONException;
import org.json.JSONObject;

public class SessionInfo {
    public final String sessionId;

    public SessionInfo(JSONObject obj) throws JSONException {
        sessionId = obj.getString("sessionId");
    }
}
