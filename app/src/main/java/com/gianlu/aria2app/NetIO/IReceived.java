package com.gianlu.aria2app.NetIO;

import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public interface IReceived {
    void onResponse(JSONObject response) throws JSONException;

    void onException(boolean queuing, @Nullable Exception ex);
}

