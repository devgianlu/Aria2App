package com.gianlu.aria2app.SelectProfile;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.NetIO.JTA2.JTA2;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class SingleModeProfileItem extends ProfileItem implements Parcelable {
    public static final Creator<SingleModeProfileItem> CREATOR = new Creator<SingleModeProfileItem>() {
        @Override
        public SingleModeProfileItem createFromParcel(Parcel in) {
            return new SingleModeProfileItem(in);
        }

        @Override
        public SingleModeProfileItem[] newArray(int size) {
            return new SingleModeProfileItem[size];
        }
    };
    private String profileName;
    private String serverAddr;
    private int serverPort;
    private String serverEndpoint;
    private JTA2.AUTH_METHOD authMethod;
    private boolean serverSSL;
    private String serverUsername;
    private String serverPassword;
    private String serverToken;
    private boolean directDownloadEnabled;
    private DirectDownload directDownload;

    private SingleModeProfileItem() {
        this.singleMode = true;
        this.status = STATUS.UNKNOWN;
    }

    public SingleModeProfileItem(String profileName, String serverAddr, int serverPort, String serverEndpoint, JTA2.AUTH_METHOD authMethod, boolean serverSSL, boolean directDownloadEnabled, @Nullable DirectDownload directDownload) {
        this.authMethod = authMethod;
        this.serverUsername = null;
        this.serverPassword = null;
        this.serverToken = null;
        this.profileName = profileName;
        this.singleMode = true;
        this.status = STATUS.UNKNOWN;
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
        this.serverEndpoint = serverEndpoint;
        this.serverSSL = serverSSL;
        this.directDownloadEnabled = directDownloadEnabled;
        this.directDownload = directDownload;
    }

    public SingleModeProfileItem(String profileName, String serverAddr, int serverPort, String serverEndpoint, JTA2.AUTH_METHOD authMethod, boolean serverSSL, String serverToken, boolean directDownloadEnabled, @Nullable DirectDownload directDownload) {
        this.authMethod = authMethod;
        this.profileName = profileName;
        this.serverUsername = null;
        this.serverPassword = null;
        this.singleMode = true;
        this.status = STATUS.UNKNOWN;
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
        this.serverEndpoint = serverEndpoint;
        this.serverSSL = serverSSL;
        this.serverToken = serverToken;
        this.directDownloadEnabled = directDownloadEnabled;
        this.directDownload = directDownload;
    }

    public SingleModeProfileItem(String profileName, String serverAddr, int serverPort, String serverEndpoint, JTA2.AUTH_METHOD authMethod, boolean serverSSL, String serverUsername, String serverPassword, boolean directDownloadEnabled, @Nullable DirectDownload directDownload) {
        this.authMethod = authMethod;
        this.serverUsername = serverUsername;
        this.serverPassword = serverPassword;
        this.serverToken = null;
        this.profileName = profileName;
        this.singleMode = true;
        this.status = STATUS.UNKNOWN;
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
        this.serverEndpoint = serverEndpoint;
        this.serverSSL = serverSSL;
        this.directDownloadEnabled = directDownloadEnabled;
        this.directDownload = directDownload;
    }

    protected SingleModeProfileItem(Parcel in) {
        super(in);
        profileName = in.readString();
        serverAddr = in.readString();
        serverPort = in.readInt();
        serverEndpoint = in.readString();
        authMethod = JTA2.AUTH_METHOD.valueOf(in.readString());
        serverSSL = in.readByte() != 0;
        serverUsername = in.readString();
        serverPassword = in.readString();
        serverToken = in.readString();
        directDownloadEnabled = in.readByte() != 0;
        directDownload = in.readParcelable(DirectDownload.class.getClassLoader());
    }

    public static SingleModeProfileItem fromJSON(String json) throws JSONException, IOException {
        JSONObject jProfile = new JSONObject(json);
        SingleModeProfileItem item = new SingleModeProfileItem();
        item.profileName = jProfile.getString("name");
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

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(profileName);
        dest.writeString(serverAddr);
        dest.writeInt(serverPort);
        dest.writeString(serverEndpoint);
        dest.writeString(authMethod.name());
        dest.writeByte((byte) (serverSSL ? 1 : 0));
        dest.writeString(serverUsername);
        dest.writeString(serverPassword);
        dest.writeString(serverToken);
        dest.writeByte((byte) (directDownloadEnabled ? 1 : 0));
        dest.writeParcelable(directDownload, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public SingleModeProfileItem setGlobalProfileName(String globalProfileName) {
        this.globalProfileName = globalProfileName;
        return this;
    }

    public String getProfileName() {
        if (profileName == null) return globalProfileName;
        return profileName;
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

    public boolean isServerSSL() {
        return serverSSL;
    }

    public String getServerToken() {
        return serverToken;
    }

    public String getFullServerAddr() {
        return "http://" + serverAddr + ":" + serverPort + serverEndpoint;
    }

    public JTA2.AUTH_METHOD getAuthMethod() {
        return authMethod;
    }

    public String getServerUsername() {
        return serverUsername;
    }

    public String getServerPassword() {
        return serverPassword;
    }

    public boolean isDirectDownloadEnabled() {
        return directDownloadEnabled;
    }

    public DirectDownload getDirectDownload() {
        return directDownload;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject profile = new JSONObject();
        profile.put("name", profileName)
                .put("serverAddr", serverAddr)
                .put("serverPort", serverPort)
                .put("serverEndpoint", serverEndpoint)
                .put("authMethod", authMethod.name())
                .put("serverToken", serverToken)
                .put("serverUsername", serverUsername)
                .put("serverPassword", serverPassword)
                .put("default", isDefault)
                .put("serverSSL", serverSSL);

        if (directDownloadEnabled) {
            profile.put("directDownload", directDownload.toJSON());
        }

        return profile;
    }
}
