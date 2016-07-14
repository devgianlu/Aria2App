package com.gianlu.aria2app.SelectProfile;

import android.content.Context;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class SingleModeProfileItem extends ProfileItem {
    private String serverAddr;
    private int serverPort;
    private String serverEndpoint;
    private boolean serverAuth;
    private boolean serverSSL;
    private String serverToken;
    private boolean directDownloadEnabled;
    private DirectDownload directDownload;

    private SingleModeProfileItem() {
        this.singleMode = true;
        this.status = STATUS.UNKNOWN;
    }

    public SingleModeProfileItem(String profileName, String serverAddr, int serverPort, String serverEndpoint, boolean serverAuth, boolean serverSSL, String serverToken, boolean directDownloadEnabled, @Nullable DirectDownload directDownload) {
        this.profileName = profileName;
        this.singleMode = true;
        this.status = STATUS.UNKNOWN;
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
        this.serverEndpoint = serverEndpoint;
        this.serverAuth = serverAuth;
        this.serverSSL = serverSSL;
        this.serverToken = serverToken;
        this.directDownloadEnabled = directDownloadEnabled;
        this.directDownload = directDownload;
    }

    public static SingleModeProfileItem fromJSON(String json) throws JSONException, IOException {
        JSONObject jProfile = new JSONObject(json);
        SingleModeProfileItem item = new SingleModeProfileItem();
        item.profileName = jProfile.getString("name");
        item.serverAuth = jProfile.getBoolean("serverAuth");
        item.serverToken = jProfile.getString("serverToken");
        item.serverSSL = jProfile.optBoolean("serverSSL", false);
        item.isDefault = jProfile.optBoolean("default", false);
        if (!jProfile.optString("serverIP").isEmpty()) {
            URL serverIP = new URL(jProfile.getString("serverIP"));
            item.serverAddr = serverIP.getHost();
            item.serverPort = serverIP.getPort();
            item.serverEndpoint = serverIP.getPath();
        } else {
            item.serverAddr = jProfile.getString("serverAddr");
            item.serverPort = jProfile.getInt("serverPort");
            item.serverEndpoint = jProfile.getString("serverEndpoint");
        }

        if (!jProfile.isNull("directDownload")) {
            item.directDownloadEnabled = true;
            item.directDownload = DirectDownload.fromJSON(jProfile.getJSONObject("directDownload").toString());
        } else {
            item.directDownloadEnabled = false;
        }

        return item;
    }

    public static SingleModeProfileItem fromString(Context context, String name) throws IOException, JSONException {
        return fromFile(context, new File(name + ".profile"));
    }

    public static SingleModeProfileItem fromFile(Context context, File file) throws IOException, JSONException {
        FileInputStream in = context.openFileInput(file.getName());
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder builder = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }

        return fromJSON(builder.toString());
    }

    public String getServerAddr() {
        return serverAddr;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getServerEndpoint() {
        return serverEndpoint;
    }

    public boolean isServerAuth() {
        return serverAuth;
    }

    public boolean isServerSSL() {
        return serverSSL;
    }

    public String getServerToken() {
        return serverToken;
    }

    public String getFullServerAddr() {
        return "http://" + serverAddr + ":" + serverPort + serverEndpoint;
    }

    public boolean isDirectDownloadEnabled() {
        return directDownloadEnabled;
    }

    public DirectDownload getDirectDownload() {
        return directDownload;
    }
}
