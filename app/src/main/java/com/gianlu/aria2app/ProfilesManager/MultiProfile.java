package com.gianlu.aria2app.ProfilesManager;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.Activities.EditProfile.AuthenticationFragment;
import com.gianlu.aria2app.Activities.EditProfile.ConnectionFragment;
import com.gianlu.aria2app.Activities.EditProfile.DirectDownloadFragment;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.Drawer.BaseDrawerProfile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MultiProfile implements BaseDrawerProfile, Serializable {
    public final List<UserProfile> profiles;
    public final String id;
    public final String name;
    public boolean notificationsEnabled;

    public MultiProfile(String name, boolean enableNotifs) {
        this.name = name;
        this.notificationsEnabled = enableNotifs;
        this.id = ProfilesManager.getId(name);
        profiles = new ArrayList<>();
    }

    MultiProfile(JSONObject obj) throws JSONException {
        this.name = obj.getString("name");
        this.notificationsEnabled = obj.optBoolean("notificationsEnabled", true);
        this.id = ProfilesManager.getId(name);

        profiles = new ArrayList<>();
        if (obj.has("serverAddr")) { // Needed for backward compatibility
            UserProfile unique = new UserProfile(obj);
            profiles.add(unique);
            return;
        }

        if (obj.isNull("profiles")) { // I hate backward compatibility
            JSONArray profilesArray = obj.getJSONArray("conditions");
            for (int i = 0; i < profilesArray.length(); i++) {
                JSONObject conditionObj = profilesArray.getJSONObject(i);
                ConnectivityCondition.Type type = ConnectivityCondition.Type.valueOf(conditionObj.getString("type").toUpperCase());

                boolean isDefault; // Needed for backward compatibility
                if (conditionObj.has("isDefault")) isDefault = conditionObj.getBoolean("isDefault");
                else isDefault = conditionObj.getJSONObject("profile").getBoolean("default");

                ConnectivityCondition condition;
                switch (type) {
                    case WIFI:
                        condition = ConnectivityCondition.newWiFiCondition(conditionObj.getString("ssid"), isDefault);
                        break;
                    case MOBILE:
                        condition = ConnectivityCondition.newMobileCondition(isDefault);
                        break;
                    case ETHERNET:
                        condition = ConnectivityCondition.newEthernetCondition(isDefault);
                        break;
                    case BLUETOOTH:
                        condition = ConnectivityCondition.newBluetoothCondition(isDefault);
                        break;
                    default:
                    case ALWAYS:
                        condition = ConnectivityCondition.newUniqueCondition();
                        break;
                }

                JSONObject profile = conditionObj.getJSONObject("profile");
                profile.put("name", name + " - " + condition.type.name().toLowerCase());
                profiles.add(new UserProfile(profile, condition));
            }
        } else {
            JSONArray profilesArray = obj.getJSONArray("profiles");
            for (int i = 0; i < profilesArray.length(); i++)
                profiles.add(new UserProfile(profilesArray.getJSONObject(i)));
        }
    }

    public MultiProfile(@NonNull String token, int port) {
        this.name = "Local device";
        this.id = ProfilesManager.getId(name);

        profiles = new ArrayList<>();
        profiles.add(new UserProfile(token, port));
    }

    private UserProfile getDefaultProfile() {
        for (UserProfile profile : profiles)
            if (profile.connectivityCondition.isDefault)
                return profile;

        return profiles.get(0);
    }

    @Nullable
    private UserProfile findFor(ConnectivityCondition.Type type) {
        for (UserProfile profile : profiles)
            if (profile.connectivityCondition.type == type)
                return profile;

        return null;
    }

    @Nullable
    private UserProfile findForWifi(String ssid) {
        for (UserProfile profile : profiles)
            if (profile.connectivityCondition.type == ConnectivityCondition.Type.WIFI && Objects.equals(profile.connectivityCondition.ssid, ssid))
                return profile;

        return null;
    }

    public UserProfile getProfile(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (connManager.getActiveNetworkInfo() == null) return getDefaultProfile();

        UserProfile profile = null;
        switch (connManager.getActiveNetworkInfo().getType()) {
            case ConnectivityManager.TYPE_WIMAX:
            case ConnectivityManager.TYPE_WIFI:
                profile = findForWifi(wifiManager.getConnectionInfo().getSSID().replace("\"", ""));
                break;
            case ConnectivityManager.TYPE_MOBILE_DUN:
            case ConnectivityManager.TYPE_MOBILE:
                profile = findFor(ConnectivityCondition.Type.MOBILE);
                break;
            case ConnectivityManager.TYPE_ETHERNET:
                profile = findFor(ConnectivityCondition.Type.ETHERNET);
                break;
            case ConnectivityManager.TYPE_BLUETOOTH:
                profile = findFor(ConnectivityCondition.Type.BLUETOOTH);
                break;
        }

        if (profile == null) return getDefaultProfile();
        else return profile;
    }

    @Override
    public String getProfileName(Context context) {
        return getProfile(context).getProfileName(context);
    }

    @Override
    public String getSecondaryText(Context context) {
        return getProfile(context).getSecondaryText(context);
    }

    @Override
    public String getInitials(Context context) {
        return getProfile(context).getInitials(context);
    }

    public JSONObject toJSON() throws JSONException {
        if (profiles.isEmpty()) throw new IllegalStateException("profiles cannot be empty!");

        JSONObject obj = new JSONObject();
        obj.put("name", name).put("notificationsEnabled", notificationsEnabled);

        JSONArray profilesArray = new JSONArray();
        for (UserProfile profile : profiles) profilesArray.put(profile.toJSON());

        obj.put("profiles", profilesArray);
        return obj;
    }

    @Override
    public String toString() {
        return name;
    }

    public void add(ConnectivityCondition cond, ConnectionFragment.Fields connFields, AuthenticationFragment.Fields authFields, DirectDownloadFragment.Fields ddFields) {
        profiles.add(new UserProfile(cond, connFields, authFields, ddFields));
    }

    public enum ConnectionMethod {
        HTTP,
        WEBSOCKET
    }

    public enum Status {
        OFFLINE,
        ERROR,
        UNKNOWN,
        ONLINE
    }

    public static class ConnectivityCondition implements Serializable {
        public final Type type;
        public final String ssid;
        public final boolean isDefault;

        public ConnectivityCondition(Type type, boolean isDefault, @Nullable String ssid) {
            this.type = type;
            this.isDefault = isDefault;
            this.ssid = ssid;
        }

        public ConnectivityCondition(JSONObject obj) throws JSONException {
            type = Type.valueOf(obj.getString("type"));
            ssid = obj.optString("ssid", null);
            isDefault = obj.getBoolean("isDefault");
        }

        public ConnectivityCondition(Type type, String ssid, boolean isDefault) {
            this.type = type;
            this.ssid = ssid;
            this.isDefault = isDefault;
        }

        public static ConnectivityCondition newWiFiCondition(String ssid, boolean isDefault) {
            return new ConnectivityCondition(Type.WIFI, isDefault, ssid);
        }

        public static ConnectivityCondition newMobileCondition(boolean isDefault) {
            return new ConnectivityCondition(Type.MOBILE, isDefault, null);
        }

        public static ConnectivityCondition newBluetoothCondition(boolean isDefault) {
            return new ConnectivityCondition(Type.BLUETOOTH, isDefault, null);
        }

        public static ConnectivityCondition newEthernetCondition(boolean isDefault) {
            return new ConnectivityCondition(Type.ETHERNET, isDefault, null);
        }

        public static ConnectivityCondition newUniqueCondition() {
            return new ConnectivityCondition(Type.ALWAYS, true, null);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConnectivityCondition that = (ConnectivityCondition) o;
            return type == that.type && (ssid != null ? ssid.equals(that.ssid) : that.ssid == null);
        }

        public JSONObject toJSON() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("type", type.name()).put("isDefault", isDefault);
            if (ssid != null) obj.put("ssid", ssid);
            return obj;
        }

        public String getFormal(Context context) {
            return type.getFormal(context) + (type == Type.WIFI ? ": " + ssid : "");
        }

        public enum Type {
            ALWAYS,
            WIFI,
            MOBILE,
            ETHERNET,
            BLUETOOTH;

            public String getFormal(Context context) {
                switch (this) {
                    case WIFI:
                        return context.getString(R.string.wifi);
                    case MOBILE:
                        return context.getString(R.string.mobile);
                    case ETHERNET:
                        return context.getString(R.string.ethernet);
                    case BLUETOOTH:
                        return context.getString(R.string.bluetooth);
                    default:
                    case ALWAYS:
                        return context.getString(R.string.always);
                }
            }
        }
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

    public static class TestStatus implements Serializable {
        public final Status status;
        public final long latency;

        public TestStatus(Status status, long latency) {
            this.latency = latency;
            this.status = status;
        }

        public TestStatus(Status status) {
            this.latency = -1;
            this.status = status;
        }
    }

    public class UserProfile implements BaseDrawerProfile, Serializable {
        public final String serverAddr;
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
        public final ConnectivityCondition connectivityCondition;
        public TestStatus status;

        public UserProfile(JSONObject obj) throws JSONException {
            this(obj, null);
        }

        public UserProfile(ConnectivityCondition cond, ConnectionFragment.Fields connFields, AuthenticationFragment.Fields authFields, DirectDownloadFragment.Fields ddFields) {
            connectivityCondition = cond;
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
            this.status = new TestStatus(Status.UNKNOWN);
        }

        private UserProfile(String token, int port) {
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
            connectivityCondition = ConnectivityCondition.newUniqueCondition();
            status = new TestStatus(Status.UNKNOWN);
        }

        public UserProfile(JSONObject obj, @Nullable ConnectivityCondition condition) throws JSONException {
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

            if (obj.isNull("connectivityCondition")) {
                if (condition == null)
                    connectivityCondition = ConnectivityCondition.newUniqueCondition();
                else
                    connectivityCondition = condition;
            } else {
                connectivityCondition = new ConnectivityCondition(obj.getJSONObject("connectivityCondition"));
            }

            status = new TestStatus(Status.UNKNOWN);
        }

        public void setStatus(TestStatus status) {
            this.status = status;
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
            profile.put("serverAddr", serverAddr)
                    .put("serverPort", serverPort)
                    .put("serverEndpoint", serverEndpoint)
                    .put("authMethod", authMethod.name())
                    .put("serverToken", serverToken)
                    .put("serverUsername", serverUsername)
                    .put("serverPassword", serverPassword)
                    .put("serverSSL", serverSSL)
                    .put("connectionMethod", connectionMethod.name())
                    .put("certificatePath", certificatePath)
                    .put("connectivityCondition", connectivityCondition.toJSON());

            if (isDirectDownloadEnabled()) profile.put("directDownload", directDownload.toJSON());
            return profile;
        }

        @Override
        @SuppressWarnings("SimplifiableIfStatement")
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UserProfile profile = (UserProfile) o;

            if (serverPort != profile.serverPort) return false;
            if (serverSSL != profile.serverSSL) return false;
            if (!serverAddr.equals(profile.serverAddr)) return false;
            if (!serverEndpoint.equals(profile.serverEndpoint)) return false;
            if (authMethod != profile.authMethod) return false;
            if (certificatePath != null ? !certificatePath.equals(profile.certificatePath) : profile.certificatePath != null)
                return false;
            if (serverUsername != null ? !serverUsername.equals(profile.serverUsername) : profile.serverUsername != null)
                return false;
            if (serverPassword != null ? !serverPassword.equals(profile.serverPassword) : profile.serverPassword != null)
                return false;
            if (serverToken != null ? !serverToken.equals(profile.serverToken) : profile.serverToken != null)
                return false;
            if (connectionMethod != profile.connectionMethod) return false;
            return connectivityCondition.equals(profile.connectivityCondition);
        }

        @Override
        public String getProfileName(Context context) {
            return name;
        }

        @Override
        public String getSecondaryText(Context context) {
            return getFullServerAddress();
        }

        @Override
        public String getInitials(Context context) {
            String name = getProfileName(context);
            if (name.length() < 2) return name;
            else return getProfileName(context).substring(0, 2);
        }
    }
}
