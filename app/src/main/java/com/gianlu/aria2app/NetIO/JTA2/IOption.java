package com.gianlu.aria2app.NetIO.JTA2;

import org.json.JSONException;

import java.util.Map;

public interface IOption {
    void onOptions(Map<String, String> options) throws JSONException;

    void onException(Exception exception);
}
