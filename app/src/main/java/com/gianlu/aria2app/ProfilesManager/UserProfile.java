package com.gianlu.aria2app.ProfilesManager;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.Activities.EditProfile.AuthenticationFragment;
import com.gianlu.aria2app.Activities.EditProfile.ConnectionFragment;
import com.gianlu.aria2app.Activities.EditProfile.DirectDownloadFragment;
import com.gianlu.aria2app.Activities.EditProfile.GeneralFragment;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.commonutils.Drawer.BaseDrawerProfile;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

public class UserProfile extends BaseProfile implements BaseDrawerProfile, Serializable {
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

    public UserProfile(JSONObject obj) throws JSONException {
        super(obj);

        notificationsEnabled = obj.optBoolean("notificationsEnabled", true);
        if (obj.has("serverAuth")) authMethod = JTA2.AuthMethod.TOKEN;
        else authMethod = JTA2.AuthMethod.valueOf(obj.optString("authMethod", "NONE"));
        serverUsername = obj.optString("serverUsername", null);
        serverPassword = obj.optString("serverPassword", null);
        serverToken = obj.optString("serverToken", null);
        serverSSL = obj.optBoolean("serverSSL", false);

        serverAddr = obj.getString("serverAddr");
        serverPort = obj.getInt("serverPort");
        serverEndpoint = obj.getString("serverEndpoint");
        certificatePath = obj.optString("certificatePath", null);

        if (obj.has("directDownload"))
            directDownload = new DirectDownload(obj.getJSONObject("directDownload"));
        else directDownload = null;

        connectionMethod = ConnectionMethod.valueOf(obj.optString("connectionMethod", ConnectionMethod.WEBSOCKET.name()));
    }

    public UserProfile(GeneralFragment.Fields generalFields, ConnectionFragment.Fields connFields, AuthenticationFragment.Fields authFields, DirectDownloadFragment.Fields ddFields) {
        super(generalFields.profileName);
        notificationsEnabled = generalFields.enableNotifs;
        authMethod = authFields.authMethod;
        serverUsername = authFields.username;
        serverPassword = authFields.password;
        serverToken = authFields.token;
        connectionMethod = connFields.connectionMethod;
        serverSSL = connFields.encryption;
        certificatePath = connFields.certificatePath;
        serverAddr = connFields.address;
        serverPort = connFields.port;
        serverEndpoint = connFields.endpoint;
        directDownload = ddFields.dd;
    }

    private UserProfile(String name, String token, int port) {
        super(name);
        notificationsEnabled = false;
        serverAddr = "localhost";
        authMethod = JTA2.AuthMethod.TOKEN;
        serverUsername = null;
        serverPassword = null;
        serverToken = token;
        connectionMethod = ConnectionMethod.WEBSOCKET;
        serverPort = port;
        serverEndpoint = "/jsonrpc";
        serverSSL = false;
        certificatePath = null;
        directDownload = null;
    }

    public static UserProfile createExternal(@NonNull String token, int port) {
        return new UserProfile("Local device", token, port);
    }

    public String buildWebSocketUrl() {
        return (serverSSL ? "wss://" : "ws://") + serverAddr + ":" + serverPort + serverEndpoint;
    }

    public String buildHttpUrl() {
        return (serverSSL ? "https://" : "http://") + serverAddr + ":" + serverPort + serverEndpoint;
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

    @Override
    public String toString() {
        return getProfileName();
    }

    public enum ConnectionMethod {
        HTTP,
        WEBSOCKET
    }

    public static class DirectDownload implements Serializable {
        public final String address;
        public final boolean auth;
        public final String username;
        public final String password;

        public DirectDownload(JSONObject obj) throws JSONException {
            address = obj.getString("addr");
            auth = obj.getBoolean("auth");
            username = obj.optString("username", null);
            password = obj.optString("password", null);
        }

        public DirectDownload(String address, boolean auth, @Nullable String username, @Nullable String password) {
            this.address = address;
            this.auth = auth;
            this.username = username;
            this.password = password;
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
