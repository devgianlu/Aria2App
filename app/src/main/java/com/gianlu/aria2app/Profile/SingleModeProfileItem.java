package com.gianlu.aria2app.Profile;

import android.content.Context;
import android.util.Base64;
import android.widget.CheckBox;
import android.widget.EditText;

import com.gianlu.aria2app.NetIO.JTA2.JTA2;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;

public class SingleModeProfileItem extends ProfileItem implements Serializable {
    private static final String EXTERNAL_DEFAULT_NAME = "Local device";
    public static final String EXTERNAL_DEFAULT_FILE_NAME = Base64.encodeToString(EXTERNAL_DEFAULT_NAME.getBytes(), Base64.NO_WRAP) + ".profile";
    public String serverAddr;
    public int serverPort;
    public String serverEndpoint;
    public JTA2.AuthMethod authMethod;
    public boolean serverSSL;
    public String certificatePath;
    public String serverUsername;
    public String serverPassword;
    public String serverToken;
    public boolean directDownloadEnabled;
    public DirectDownload directDownload;
    public ConnectionMethod connectionMethod;
    private String profileName;

    private SingleModeProfileItem() {
        this.singleMode = true;
        this.status = ProfileItem.STATUS.UNKNOWN;
    }

    public static SingleModeProfileItem externalDefault(int port, String token) {
        SingleModeProfileItem profile = new SingleModeProfileItem();
        profile.profileName = EXTERNAL_DEFAULT_NAME;
        profile.globalProfileName = EXTERNAL_DEFAULT_NAME;
        profile.serverAddr = "localhost";
        profile.serverPort = port;
        profile.serverEndpoint = "/jsonrpc";
        profile.authMethod = JTA2.AuthMethod.TOKEN;
        profile.serverToken = token;
        profile.serverSSL = false;
        profile.notificationsEnabled = false;
        profile.directDownloadEnabled = false;
        profile.connectionMethod = ConnectionMethod.WEBSOCKET;

        return profile;
    }

    public static SingleModeProfileItem defaultProfile() {
        SingleModeProfileItem profile = new SingleModeProfileItem();
        profile.profileName = "Empty";
        profile.globalProfileName = "Empty";
        profile.serverAddr = "localhost";
        profile.serverPort = 6800;
        profile.serverEndpoint = "/jsonrpc";
        profile.authMethod = JTA2.AuthMethod.NONE;
        profile.serverSSL = false;
        profile.notificationsEnabled = false;
        profile.directDownloadEnabled = false;
        profile.connectionMethod = ConnectionMethod.WEBSOCKET;

        return profile;
    }

    static SingleModeProfileItem fromFields(String profileName, CheckBox enableNotifications, AddProfileActivity.SingleModeViewHolder sViewHolder) {
        SingleModeProfileItem profile = new SingleModeProfileItem();

        profile.profileName = profileName;
        profile.serverAddr = sViewHolder.addr.getText().toString().trim();
        profile.serverPort = Integer.parseInt(sViewHolder.port.getText().toString().trim());
        profile.serverEndpoint = sViewHolder.endpoint.getText().toString().trim();
        profile.serverSSL = sViewHolder.SSL.isChecked();
        profile.notificationsEnabled = enableNotifications.isChecked();
        profile.directDownloadEnabled = sViewHolder.directDownload.isChecked();
        profile.directDownload = DirectDownload.fromFields(sViewHolder);

        if (sViewHolder.authMethodNone.isChecked()) {
            profile.authMethod = JTA2.AuthMethod.NONE;
        } else if (sViewHolder.authMethodToken.isChecked()) {
            profile.authMethod = JTA2.AuthMethod.TOKEN;
            profile.serverToken = sViewHolder.authMethodTokenToken.getText().toString().trim();
        } else if (sViewHolder.authMethodHTTP.isChecked()) {
            profile.authMethod = JTA2.AuthMethod.HTTP;
            profile.serverUsername = sViewHolder.authMethodHTTPUsername.getText().toString().trim();
            profile.serverPassword = sViewHolder.authMethodHTTPPassword.getText().toString().trim();
        } else {
            profile.authMethod = JTA2.AuthMethod.NONE;
        }

        if (profile.serverSSL)
            profile.certificatePath = sViewHolder.SSLCertificate.getText().toString().trim();

        if (sViewHolder.connMethodWebSocket.isChecked())
            profile.connectionMethod = ConnectionMethod.WEBSOCKET;
        else
            profile.connectionMethod = ConnectionMethod.HTTP;

        return profile;
    }

    static SingleModeProfileItem fromFields(EditText profileName, CheckBox enableNotifications, AddProfileActivity.SingleModeViewHolder sViewHolder) {
        return fromFields(profileName.getText().toString().trim(), enableNotifications, sViewHolder);
    }

    public static SingleModeProfileItem fromJSON(String fileName, String json) throws JSONException, IOException {
        JSONObject jProfile = new JSONObject(json);
        SingleModeProfileItem item = new SingleModeProfileItem();
        item.fileName = fileName;

        item.profileName = jProfile.getString("name");
        item.notificationsEnabled = jProfile.optBoolean("notificationsEnabled", true);
        item.globalProfileName = item.profileName;
        if (!jProfile.isNull("serverAuth")) {
            item.authMethod = JTA2.AuthMethod.TOKEN;
        } else {
            String authMethod = jProfile.optString("authMethod");
            item.authMethod = JTA2.AuthMethod.valueOf(authMethod == null ? "NONE" : authMethod);
        }
        item.serverUsername = jProfile.optString("serverUsername");
        item.serverPassword = jProfile.optString("serverPassword");
        item.serverToken = jProfile.optString("serverToken");
        item.serverSSL = jProfile.optBoolean("serverSSL", false);
        item.isDefault = jProfile.optBoolean("default", false);
        if (!jProfile.optString("serverIP").isEmpty()) {
            URL serverIP = new URL(jProfile.getString("serverIP"));
            item.serverAddr = serverIP.getHost();
            item.serverPort = serverIP.getPort();
            item.serverEndpoint = serverIP.getPath();
        } else {
            item.serverAddr = jProfile.optString("serverAddr");
            item.serverPort = jProfile.optInt("serverPort");
            item.serverEndpoint = jProfile.optString("serverEndpoint");
        }

        item.certificatePath = jProfile.optString("certificatePath");

        if (!jProfile.isNull("directDownload")) {
            item.directDownloadEnabled = true;
            item.directDownload = DirectDownload.fromJSON(jProfile.getJSONObject("directDownload").toString());
        } else {
            item.directDownloadEnabled = false;
        }

        item.connectionMethod = ConnectionMethod.valueOf(jProfile.optString("connectionMethod", ConnectionMethod.WEBSOCKET.name()));

        return item;
    }

    public static SingleModeProfileItem fromName(Context context, String fileName) throws IOException, JSONException {
        FileInputStream in = context.openFileInput(fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder builder = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }

        return fromJSON(fileName, builder.toString());
    }

    void setGlobalProfileName(String globalProfileName) {
        this.globalProfileName = globalProfileName;
    }

    @Override
    public String getProfileName() {
        if (profileName == null) return globalProfileName;
        return profileName;
    }

    @Override
    public String getSecondaryText() {
        return getFullServerAddress();
    }

    @Override
    public String getInitials() {
        return getProfileName().substring(0, 2);
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

    public JSONObject toJSON() throws JSONException {
        JSONObject profile = new JSONObject();
        profile.put("name", profileName)
                .put("serverAddr", serverAddr)
                .put("serverPort", serverPort)
                .put("notificationsEnabled", notificationsEnabled)
                .put("serverEndpoint", serverEndpoint)
                .put("authMethod", authMethod.name())
                .put("serverToken", serverToken)
                .put("serverUsername", serverUsername)
                .put("serverPassword", serverPassword)
                .put("default", isDefault)
                .put("serverSSL", serverSSL)
                .put("connectionMethod", connectionMethod.name())
                .put("certificatePath", certificatePath);

        if (directDownloadEnabled)
            profile.put("directDownload", directDownload.toJSON());

        return profile;
    }

    public enum ConnectionMethod {
        HTTP,
        WEBSOCKET
    }
}
