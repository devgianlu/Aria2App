package com.gianlu.aria2app.ProfilesManager;

import android.content.Context;

import com.gianlu.commonutils.Drawer.BaseDrawerProfile;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public abstract class BaseProfile implements Serializable, BaseDrawerProfile {
    public final String id;
    public final String name;
    public boolean notificationsEnabled;
    public TestStatus status;

    public BaseProfile(String name) {
        this.id = ProfilesManager.getId(name);
        this.name = name;
        this.status = new TestStatus(Status.UNKNOWN);
    }

    public BaseProfile(JSONObject obj) throws JSONException {
        this.name = obj.getString("name");
        this.notificationsEnabled = obj.optBoolean("notificationsEnabled", true);
        this.id = ProfilesManager.getId(name);
        this.status = new TestStatus(Status.UNKNOWN);
    }

    public abstract UserProfile getProfile(Context context);

    public void setStatus(TestStatus status) {
        this.status = status;
    }

    public enum Status {
        OFFLINE,
        ERROR,
        UNKNOWN,
        ONLINE
    }

    public static class TestStatus implements Serializable {
        public final long latency;
        public final Status status;

        public TestStatus(Status status, long latency) {
            this.latency = latency;
            this.status = status;
        }

        public TestStatus(Status status) {
            this.latency = -1;
            this.status = status;
        }
    }
}
