package com.gianlu.aria2app.NetIO.JTA2;

import org.json.JSONException;
import org.json.JSONObject;

public class AriaException extends Exception {
    public final int code;
    public final String reason;

    public AriaException(String detailMessage, int code) {
        super(detailMessage);
        this.reason = detailMessage;
        this.code = code;
    }

    public AriaException(JSONObject error) throws JSONException {
        this(error.getString("message"), error.getInt("code"));
    }

    @Override
    public String toString() {
        return "AriaException #" + code + ": " + getMessage();
    }
}
