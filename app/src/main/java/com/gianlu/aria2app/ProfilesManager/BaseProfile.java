package com.gianlu.aria2app.ProfilesManager;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public abstract class BaseProfile implements Serializable {
    public final String id;
    public final String name;
    public long latency = -1;
    public Status status = Status.UNKNOWN;

    public BaseProfile(String name) {
        this.id = ProfilesManager.getId(name);
        this.name = name;
    }

    public BaseProfile(JSONObject obj) throws JSONException {
        this.name = obj.getString("name");
        this.id = ProfilesManager.getId(name);
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setLatency(long latency) {
        this.latency = latency;
    }

    public void delete(Context context) {
        context.deleteFile(id + ".profile");
    }

    public enum Status {
        OFFLINE,
        ERROR,
        UNKNOWN,
        ONLINE
    }
}
