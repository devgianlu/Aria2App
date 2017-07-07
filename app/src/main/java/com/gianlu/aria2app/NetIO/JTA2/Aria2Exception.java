package com.gianlu.aria2app.NetIO.JTA2;

import org.json.JSONException;
import org.json.JSONObject;

public class Aria2Exception extends Exception {
    public final int code;
    public final String reason;

    public Aria2Exception(String detailMessage, int code) {
        super(detailMessage);
        this.reason = detailMessage;
        this.code = code;
    }

    public Aria2Exception(JSONObject error) throws JSONException {
        this(error.getString("message"), error.getInt("code"));
    }

    @Override
    public String toString() {
        return "Aria2Exception #" + code + ": " + getMessage();
    }
}
