package com.gianlu.aria2app.ProfilesManager;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.Activities.EditProfile.AuthenticationFragment;
import com.gianlu.aria2app.Activities.EditProfile.ConnectionFragment;
import com.gianlu.aria2app.Activities.EditProfile.DirectDownloadFragment;
import com.gianlu.aria2app.Activities.EditProfile.GeneralFragment;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.commonutils.Drawer.BaseDrawerProfile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MultiProfile extends BaseProfile implements BaseDrawerProfile, Serializable {
    private final Map<ConnectivityCondition, UserProfile> profiles;

    MultiProfile(JSONObject obj) throws JSONException {
        super(obj);

        profiles = new HashMap<>();
        if (obj.isNull("conditions")) { // Needed for backward compatibility
            UserProfile unique = new UserProfile(obj);
            profiles.put(ConnectivityCondition.newUniqueCondition(), unique);
            return;
        }

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

            profiles.put(condition, new UserProfile(conditionObj.getJSONObject("profile")));
        }
    }

    public MultiProfile(String name, UserProfile unique) {
        super(name);

        profiles = new HashMap<>();
        profiles.put(ConnectivityCondition.newUniqueCondition(), unique);
    }

    public static MultiProfile createForExternal(@NonNull String token, int port) {
        return new MultiProfile("Local device", new UserProfile("Local device", token, port));
    }

    private UserProfile getDefaultProfile() {
        for (Map.Entry<ConnectivityCondition, UserProfile> entry : profiles.entrySet())
            if (entry.getKey().isDefault)
                return entry.getValue();

        return profiles.values().iterator().next();
    }

    @Nullable
    private UserProfile findFor(ConnectivityCondition.Type type) {
        for (Map.Entry<ConnectivityCondition, UserProfile> entry : profiles.entrySet())
            if (entry.getKey().type == type)
                return entry.getValue();

        return null;
    }

    @Nullable
    private UserProfile findForWifi(String ssid) {
        for (Map.Entry<ConnectivityCondition, UserProfile> entry : profiles.entrySet())
            if (entry.getKey().type == ConnectivityCondition.Type.WIFI && Objects.equals(entry.getKey().ssid, ssid))
                return entry.getValue();

        return null;
    }

    @Override
    public UserProfile getProfile(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (connManager.getActiveNetworkInfo() == null) return getDefaultProfile();

        UserProfile profile = null;
        switch (connManager.getActiveNetworkInfo().getType()) {
            case ConnectivityManager.TYPE_WIMAX:
            case ConnectivityManager.TYPE_WIFI:
                profile = findForWifi(wifiManager.getConnectionInfo().getSSID());
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

    public JSONObject toJSON() {
        return null; // TODO
    }

    public enum ConnectionMethod {
        HTTP,
        WEBSOCKET
    }

    public static class UserProfile extends BaseProfile implements BaseDrawerProfile, Serializable {
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

        public UserProfile(JSONObject obj) throws JSONException {
            super(obj);

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

        @Override
        public UserProfile getProfile(Context context) {
            return this;
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
        public String getProfileName(Context context) {
            return name;
        }

        @Override
        public String getSecondaryText(Context context) {
            return getFullServerAddress();
        }

        @Override
        public String getInitials(Context context) {
            return getProfileName(context).substring(0, 2);
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
}
