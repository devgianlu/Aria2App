package com.gianlu.aria2app.NetIO.Aria2;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class AriaException extends Exception {
    public final int code;
    public final String reason;

    private AriaException(@NonNull String detailMessage, int code) {
        super(detailMessage);
        this.reason = detailMessage;
        this.code = code;
    }

    public AriaException(@NonNull JSONObject error) throws JSONException {
        this(error.getString("message"), error.getInt("code"));
    }

    public boolean isNoPeers() {
        return reason.startsWith("No peer data is available");
    }

    public boolean isNoServers() {
        return reason.startsWith("No active download");
    }

    public boolean isNotFound() {
        return reason.endsWith("is not found");
    }

    @Override
    public String toString() {
        return "AriaException #" + code + ": " + getMessage();
    }
}
