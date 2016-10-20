package com.gianlu.aria2app.Main.Profile;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

// TODO: This should be setup for each single profile (in multi)
public class DirectDownload implements Parcelable {
    public static final Creator<DirectDownload> CREATOR = new Creator<DirectDownload>() {
        @Override
        public DirectDownload createFromParcel(Parcel in) {
            return new DirectDownload(in);
        }

        @Override
        public DirectDownload[] newArray(int size) {
            return new DirectDownload[size];
        }
    };
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

    private DirectDownload(Parcel in) {
        address = in.readString();
        auth = in.readByte() != 0;
        username = in.readString();
        password = in.readString();
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

    public URL getURLAddress() throws MalformedURLException {
        return new URL(address);
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

    JSONObject toJSON() throws JSONException {
        JSONObject jDirectDownload = new JSONObject();
        jDirectDownload.put("addr", address)
                .put("auth", auth)
                .put("username", username)
                .put("password", password);
        return jDirectDownload;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(address);
        parcel.writeByte((byte) (auth ? 1 : 0));
        parcel.writeString(username);
        parcel.writeString(password);
    }
}