package com.gianlu.aria2app.NetIO;

import org.json.JSONException;
import org.json.JSONObject;

public interface OnJson {
    void onResponse(JSONObject response) throws JSONException;

    void onException(Exception ex);
}

