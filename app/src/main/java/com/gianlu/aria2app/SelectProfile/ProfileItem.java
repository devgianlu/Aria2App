package com.gianlu.aria2app.SelectProfile;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProfileItem {
    protected String profileName;
    protected boolean singleMode;
    protected STATUS status;
    protected boolean isDefault;
    private long latency = -1;

    public static boolean isSingleMode(Context context, File file) throws JSONException, IOException {
        FileInputStream in = context.openFileInput(file.getName());
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder builder = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }

        return new JSONObject(builder.toString()).isNull("conditions");
    }

    public static boolean isSingleMode(Context context, String name) throws JSONException, IOException {
        return isSingleMode(context, new File(name + ".profile"));
    }

    public STATUS getStatus() {
        return status;
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    public String getProfileName() {
        return profileName;
    }

    public boolean isSingleMode() {
        return singleMode;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public long getLatency() {
        return latency;
    }

    public void setLatency(long latency) {
        this.latency = latency;
    }

    public enum STATUS {
        ONLINE,
        OFFLINE,
        ERROR,
        UNKNOWN
    }
}
