package com.gianlu.aria2app.ProfilesManager;

import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.commonutils.Drawer.BaseDrawerProfile;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

public class UserProfile extends BaseProfile implements BaseDrawerProfile {
    public final String serverAddr;
    public final boolean notificationsEnabled;
    public final int serverPort;
    public final String serverEndpoint;
    public final JTA2.AuthMethod authMethod;
    public final boolean serverSSL;
    public final String certificatePath;
    public final String serverUsername;
    public final String serverPassword;
    public final String serverToken;
    public final DirectDownload directDownload;
    public final ConnectionMethod connectionMethod;

    public UserProfile(JSONObject obj) throws JSONException, MalformedURLException {
        super(obj);

        notificationsEnabled = obj.optBoolean("notificationsEnabled", true);
        if (!obj.isNull("serverAuth")) authMethod = JTA2.AuthMethod.TOKEN;
        else authMethod = JTA2.AuthMethod.valueOf(obj.optString("authMethod", "NONE"));
        serverUsername = obj.optString("serverUsername");
        serverPassword = obj.optString("serverPassword");
        serverToken = obj.optString("serverToken");
        serverSSL = obj.optBoolean("serverSSL", false);
        // isDefault = obj.optBoolean("default", false);

        serverAddr = obj.optString("serverAddr");
        serverPort = obj.optInt("serverPort");
        serverEndpoint = obj.optString("serverEndpoint");

        certificatePath = obj.optString("certificatePath");

        if (obj.has("directDownload"))
            directDownload = new DirectDownload(obj.getJSONObject("directDownload"));
        else directDownload = null;

        connectionMethod = ConnectionMethod.valueOf(obj.optString("connectionMethod", ConnectionMethod.WEBSOCKET.name()));
    }

    public String getFullServerAddress() {
        switch (connectionMethod) {
            case HTTP:
                return (serverSSL ? "https://" : "http://") + serverAddr + ":" + serverPort + serverEndpoint;
            default:
            case WEBSOCKET:
                return (serverSSL ? "wss://" : "ws://") + serverAddr + ":" + serverPort + serverEndpoint;
        }
    }

    public boolean isDirectDownloadEnabled() {
        return directDownload != null;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject profile = new JSONObject();
        profile.put("name", name)
                .put("serverAddr", serverAddr)
                .put("serverPort", serverPort)
                .put("notificationsEnabled", notificationsEnabled)
                .put("serverEndpoint", serverEndpoint)
                .put("authMethod", authMethod.name())
                .put("serverToken", serverToken)
                .put("serverUsername", serverUsername)
                .put("serverPassword", serverPassword)
                /* .put("default", isDefault) */
                .put("serverSSL", serverSSL)
                .put("connectionMethod", connectionMethod.name())
                .put("certificatePath", certificatePath);

        if (isDirectDownloadEnabled()) profile.put("directDownload", directDownload.toJSON());
        return profile;
    }

    @Override
    public String getProfileName() {
        return name;
    }

    @Override
    public String getSecondaryText() {
        return getFullServerAddress();
    }

    @Override
    public String getInitials() {
        return getProfileName().substring(0, 2);
    }

    public enum ConnectionMethod {
        HTTP,
        WEBSOCKET
    }

    public class DirectDownload implements Serializable {
        public final String address;
        public final boolean auth;
        public final String username;
        public final String password;

        public DirectDownload(JSONObject obj) throws JSONException {
            address = obj.getString("addr");
            auth = obj.getBoolean("auth");
            username = obj.getString("username");
            password = obj.getString("password");
        }

        public JSONObject toJSON() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("addr", address).put("auth", auth).put("username", username).put("password", password);
            return obj;
        }

        public URL getURLAddress() throws MalformedURLException {
            return new URL(address);
        }
    }
}
