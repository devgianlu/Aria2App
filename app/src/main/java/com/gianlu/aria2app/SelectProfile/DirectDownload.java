package com.gianlu.aria2app.SelectProfile;

import org.json.JSONException;
import org.json.JSONObject;

public class DirectDownload {
    private String address;
    private boolean auth;
    private String username;
    private String password;

    private DirectDownload() {
    }

    public DirectDownload(String address, boolean auth, String username, String password) {
        this.address = address;
        this.auth = auth;
        this.username = username;
        this.password = password;
    }

    public static DirectDownload fromJSON(String json) throws JSONException {
        DirectDownload item = new DirectDownload();

        JSONObject jDirectDownload = new JSONObject(json);
        item.address = jDirectDownload.getString("addr");
        item.auth = jDirectDownload.getBoolean("auth");
        item.username = jDirectDownload.getString("username");
        item.password = jDirectDownload.getString("password");

        return item;
    }

    public String getAddress() {
        return address;
    }

    public boolean isAuth() {
        return auth;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}