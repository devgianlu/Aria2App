package com.gianlu.aria2app.Main.Profile;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

public class DirectDownload implements Serializable {
    public String address;
    public boolean auth;
    public String username;
    public String password;

    private DirectDownload() {
    }

    public static DirectDownload fromFields(AddProfileActivity.SingleModeViewHolder sViewHolder) {
        DirectDownload dd = new DirectDownload();
        dd.address = sViewHolder.directDownloadAddr.getText().toString().trim();
        dd.auth = sViewHolder.directDownloadAuth.isChecked();
        dd.username = sViewHolder.directDownloadUsername.getText().toString().trim();
        dd.password = sViewHolder.directDownloadPassword.getText().toString().trim();

        return dd;
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

    public URL getURLAddress() throws MalformedURLException {
        return new URL(address);
    }

    JSONObject toJSON() throws JSONException {
        JSONObject jDirectDownload = new JSONObject();
        jDirectDownload.put("addr", address)
                .put("auth", auth)
                .put("username", username)
                .put("password", password);
        return jDirectDownload;
    }
}