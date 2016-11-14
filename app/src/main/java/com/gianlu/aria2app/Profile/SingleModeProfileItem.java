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
    public static final String EXTERNAL_DEFAULT_BASE64_NAME = Base64.encodeToString(EXTERNAL_DEFAULT_NAME.getBytes(), Base64.NO_WRAP);
    public String serverAddr;
    public int serverPort;
    public String serverEndpoint;
    public JTA2.AUTH_METHOD authMethod;
    public boolean serverSSL;
    public String certificatePath;
    public String serverUsername;
    public String serverPassword;
    public String serverToken;
    public boolean directDownloadEnabled;
    public DirectDownload directDownload;
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
        profile.authMethod = JTA2.AUTH_METHOD.TOKEN;
        profile.serverToken = token;
        profile.serverSSL = false;
        profile.notificationsEnabled = false;
        profile.directDownloadEnabled = false;

        return profile;
    }

    public static SingleModeProfileItem defaultProfile() {
        SingleModeProfileItem profile = new SingleModeProfileItem();
        profile.profileName = "Empty";
        profile.globalProfileName = "Empty";
        profile.serverAddr = "localhost";
        profile.serverPort = 6800;
        profile.serverEndpoint = "/jsonrpc";
        profile.authMethod = JTA2.AUTH_METHOD.NONE;
        profile.serverSSL = false;
        profile.notificationsEnabled = false;
        profile.directDownloadEnabled = false;

        return profile;
    }

    public static SingleModeProfileItem fromFields(String profileName, CheckBox enableNotifications, AddProfileActivity.SingleModeViewHolder sViewHolder) {
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
            profile.authMethod = JTA2.AUTH_METHOD.NONE;
        } else if (sViewHolder.authMethodToken.isChecked()) {
            profile.authMethod = JTA2.AUTH_METHOD.TOKEN;
            profile.serverToken = sViewHolder.authMethodTokenToken.getText().toString().trim();
        } else if (sViewHolder.authMethodHTTP.isChecked()) {
            profile.authMethod = JTA2.AUTH_METHOD.HTTP;
            profile.serverUsername = sViewHolder.authMethodHTTPUsername.getText().toString().trim();
            profile.serverPassword = sViewHolder.authMethodHTTPPassword.getText().toString().trim();
        } else {
            profile.authMethod = JTA2.AUTH_METHOD.NONE;
        }

        if (profile.serverSSL) {
            profile.certificatePath = sViewHolder.SSLCertificate.getText().toString().trim();
        }

        return profile;
    }

    public static SingleModeProfileItem fromFields(EditText profileName, CheckBox enableNotifications, AddProfileActivity.SingleModeViewHolder sViewHolder) {
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
            item.authMethod = JTA2.AUTH_METHOD.TOKEN;
        } else {
            String authMethod = jProfile.optString("authMethod");
            item.authMethod = JTA2.AUTH_METHOD.valueOf(authMethod == null ? "NONE" : authMethod);
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

        return item;
    }

    public static SingleModeProfileItem fromString(Context context, String base64name) throws IOException, JSONException {
        if (!base64name.endsWith(".profile"))
            base64name += ".profile";

        FileInputStream in = context.openFileInput(base64name);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder builder = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }

        return fromJSON(base64name, builder.toString());
    }

    public void setGlobalProfileName(String globalProfileName) {
        this.globalProfileName = globalProfileName;
    }

    public String getProfileName() {
        if (profileName == null) return globalProfileName;
        return profileName;
    }

    public String getFullServerAddress() {
        return (serverSSL ? "wss://" : "ws://") + serverAddr + ":" + serverPort + serverEndpoint;
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
                .put("certificatePath", certificatePath);

        if (directDownloadEnabled) {
            profile.put("directDownload", directDownload.toJSON());
        }

        return profile;
    }
}
