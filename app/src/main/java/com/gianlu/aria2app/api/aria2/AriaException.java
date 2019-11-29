package com.gianlu.aria2app.api.aria2;

import org.json.JSONException;
import org.json.JSONObject;

import androidx.annotation.NonNull;

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

    public boolean isCannotChangeOptions() {
        return reason.startsWith("Cannot change option");
    }

    @NonNull
    @Override
    public String toString() {
        return "AriaException #" + code + ": " + getMessage();
    }
}
