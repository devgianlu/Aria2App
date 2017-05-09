package com.gianlu.aria2app.ProfilesManager;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class BaseProfile {
    public final String id;
    public final String name;
    public long latency = -1;
    public Status status = Status.UNKNOWN;

    public BaseProfile(String name) {
        this.id = Base64.encodeToString(name.getBytes(), Base64.NO_WRAP);
        this.name = name;
    }

    public BaseProfile(JSONObject obj) throws JSONException {
        this.name = obj.getString("name");
        this.id = Base64.encodeToString(name.getBytes(), Base64.NO_WRAP);
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setLatency(long latency) {
        this.latency = latency;
    }

    public enum Status {
        OFFLINE,
        ERROR,
        UNKNOWN,
        ONLINE
    }
}
