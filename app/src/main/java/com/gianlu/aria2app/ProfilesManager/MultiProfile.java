package com.gianlu.aria2app.ProfilesManager;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;

import com.gianlu.aria2app.Activities.EditProfile.AuthenticationFragment;
import com.gianlu.aria2app.Activities.EditProfile.ConnectionFragment;
import com.gianlu.aria2app.Activities.EditProfile.DirectDownloadFragment;
import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.CertUtils;
import com.gianlu.aria2app.NetIO.NetUtils;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Drawer.BaseDrawerProfile;
import com.gianlu.commonutils.Logging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import okhttp3.HttpUrl;

public class MultiProfile implements BaseDrawerProfile, Serializable {
    public final List<UserProfile> profiles;
    public final String id;
    public final String name;
    public boolean notificationsEnabled;
    public TestStatus status;

    public MultiProfile(String name, boolean enableNotifs) {
        this.name = name;
        this.notificationsEnabled = enableNotifs;
        this.id = ProfilesManager.getId(name);
        this.profiles = new ArrayList<>();
        this.status = new TestStatus(Status.UNKNOWN, null);
    }

    MultiProfile(JSONObject obj) throws JSONException {
        this.name = obj.getString("name");
        this.notificationsEnabled = obj.optBoolean("notificationsEnabled", true);
        this.id = ProfilesManager.getId(name);
        this.status = new TestStatus(Status.UNKNOWN, null);

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
                        condition = ConnectivityCondition.newWiFiCondition(new String[]{conditionObj.getString("ssid")}, isDefault);
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
        this.status = new TestStatus(Status.UNKNOWN, null);

        profiles = new ArrayList<>();
        profiles.add(new UserProfile(token, port));
    }

    @NonNull
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
        for (UserProfile profile : profiles) {
            if (profile.connectivityCondition.type == ConnectivityCondition.Type.WIFI) {
                for (String profileSsid : profile.connectivityCondition.ssids)
                    if (Objects.equals(profileSsid, ssid)) return profile;
            }
        }

        return null;
    }

    @NonNull
    public UserProfile getProfile(ProfilesManager manager) {
        return getProfile(manager.getConnectivityManager(), manager.getWifiManager());
    }

    @NonNull
    public UserProfile getProfile(Context context) {
        return getProfile(ProfilesManager.get(context));
    }

    @NonNull
    public UserProfile getProfile(int networkType, WifiManager wifiManager) {
        UserProfile profile = null;
        switch (networkType) {
            case ConnectivityManager.TYPE_WIMAX:
            case ConnectivityManager.TYPE_WIFI:
                String ssid = wifiManager.getConnectionInfo().getSSID();
                if (ssid == null || ssid.length() <= 2) break;
                profile = findForWifi(ssid.substring(1, ssid.length() - 1));
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

    private UserProfile getProfile(ConnectivityManager connManager, WifiManager wifiManager) {
        NetworkInfo activeNet = connManager.getActiveNetworkInfo();
        if (activeNet == null) return getDefaultProfile();
        return getProfile(activeNet.getType(), wifiManager);
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

    JSONObject toJson() throws JSONException {
        if (profiles.isEmpty()) throw new IllegalStateException("profiles cannot be empty!");

        JSONObject obj = new JSONObject();
        obj.put("name", name).put("notificationsEnabled", notificationsEnabled);

        JSONArray profilesArray = new JSONArray();
        for (UserProfile profile : profiles) profilesArray.put(profile.toJson());

        obj.put("profiles", profilesArray);
        return obj;
    }

    @Override
    public String toString() {
        return name;
    }

    public void add(@NonNull ConnectivityCondition cond, @NonNull ConnectionFragment.Fields connFields, @NonNull AuthenticationFragment.Fields authFields, @NonNull DirectDownloadFragment.Fields ddFields) {
        profiles.add(new UserProfile(cond, connFields, authFields, ddFields));
    }

    public void setStatus(TestStatus status) {
        this.status = status;
    }

    void updateStatusPing(long ping) {
        if (status != null) this.status = new TestStatus(status.status, ping);
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
        public final String[] ssids;
        public final boolean isDefault;

        ConnectivityCondition(Type type, boolean isDefault, @Nullable String[] ssids) {
            this.type = type;
            this.isDefault = isDefault;
            this.ssids = checkSSIDsArray(ssids);
        }

        ConnectivityCondition(JSONObject obj) throws JSONException {
            type = Type.valueOf(obj.getString("type"));
            isDefault = obj.getBoolean("isDefault");

            if (obj.has("ssids")) {
                ssids = CommonUtils.toStringArray(obj.optJSONArray("ssids"));
            } else if (obj.has("ssid")) {
                ssids = new String[1];
                ssids[0] = obj.getString("ssid");
            } else {
                ssids = null;
            }
        }

        public ConnectivityCondition(Type type, String[] ssids, boolean isDefault) {
            this.type = type;
            this.ssids = checkSSIDsArray(ssids);
            this.isDefault = isDefault;
        }

        public static String[] parseSSIDs(@NonNull String rawSsids) {
            return checkSSIDsArray(rawSsids.split(",\\s+"));
        }

        @Nullable
        private static String[] checkSSIDsArray(String[] ssids) {
            if (ssids == null) return null;

            List<String> tmpList = new ArrayList<>();
            for (String ssid : ssids)
                if (!ssid.trim().isEmpty())
                    tmpList.add(ssid.trim());

            return tmpList.toArray(new String[0]);
        }

        public static ConnectivityCondition newWiFiCondition(String[] ssids, boolean isDefault) {
            return new ConnectivityCondition(Type.WIFI, isDefault, ssids);
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
        public String toString() {
            return "ConnectivityCondition{" +
                    "type=" + type +
                    ", ssids=" + Arrays.toString(ssids) +
                    ", isDefault=" + isDefault +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConnectivityCondition that = (ConnectivityCondition) o;
            return type == that.type && (ssids != null ? Arrays.equals(ssids, that.ssids) : that.ssids == null);
        }

        JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("type", type.name()).put("isDefault", isDefault);
            if (ssids != null) {
                JSONArray ssidsArray = new JSONArray();
                for (String ssid : ssids) ssidsArray.put(ssid);
                obj.put("ssids", ssidsArray);
            }
            return obj;
        }

        public String getFormal(Context context) {
            return type.getFormal(context) + (type == Type.WIFI ? ": " + CommonUtils.join(ssids, ", ") : "");
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
        private HttpUrl cachedUri;

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

        JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("addr", address).put("auth", auth).put("username", username).put("password", password);
            return obj;
        }

        @Nullable
        public HttpUrl getUrl() {
            if (cachedUri == null) cachedUri = HttpUrl.parse(address);
            return cachedUri;
        }
    }

    public static class TestStatus implements Serializable {
        public final Status status;
        public final long latency;
        public final Throwable ex;

        public TestStatus(Status status, long latency) {
            this.latency = latency;
            this.status = status;
            this.ex = null;
        }

        public TestStatus(Status status, @Nullable Throwable ex) {
            this.ex = ex;
            this.latency = -1;
            this.status = status;
        }
    }

    public class UserProfile implements BaseDrawerProfile, Serializable {
        public final String serverAddr;
        public final int serverPort;
        public final String serverEndpoint;
        public final AbstractClient.AuthMethod authMethod;
        public final boolean serverSSL;
        public final boolean hostnameVerifier;
        public final X509Certificate certificate;
        public final String serverUsername;
        public final String serverPassword;
        public final String serverToken;
        public final DirectDownload directDownload;
        public final ConnectionMethod connectionMethod;
        public final ConnectivityCondition connectivityCondition;
        private String encodedCredentials;
        private String fullServerAddress;

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
            certificate = connFields.certificate;
            serverAddr = connFields.address;
            serverPort = connFields.port;
            serverEndpoint = connFields.endpoint;
            directDownload = ddFields.dd;
            hostnameVerifier = connFields.hostnameVerifier;
        }

        private UserProfile(String token, int port) {
            serverAddr = "localhost";
            authMethod = AbstractClient.AuthMethod.TOKEN;
            serverUsername = null;
            serverPassword = null;
            serverToken = token;
            connectionMethod = ConnectionMethod.WEBSOCKET;
            serverPort = port;
            serverEndpoint = "/jsonrpc";
            serverSSL = false;
            certificate = null;
            directDownload = null;
            connectivityCondition = ConnectivityCondition.newUniqueCondition();
            hostnameVerifier = false;
            status = new TestStatus(Status.UNKNOWN, null);
        }

        public UserProfile(JSONObject obj, @Nullable ConnectivityCondition condition) throws JSONException {
            if (obj.has("serverAuth"))
                authMethod = AbstractClient.AuthMethod.TOKEN;
            else
                authMethod = AbstractClient.AuthMethod.valueOf(obj.optString("authMethod", "NONE"));

            serverUsername = obj.optString("serverUsername", null);
            serverPassword = obj.optString("serverPassword", null);
            serverToken = obj.optString("serverToken", null);
            serverSSL = obj.optBoolean("serverSSL", false);

            serverAddr = obj.getString("serverAddr");
            serverPort = obj.getInt("serverPort");
            serverEndpoint = obj.getString("serverEndpoint");
            hostnameVerifier = obj.optBoolean("hostnameVerifier", false);

            if (obj.has("directDownload"))
                directDownload = new DirectDownload(obj.getJSONObject("directDownload"));
            else directDownload = null;

            connectionMethod = ConnectionMethod.valueOf(obj.optString("connectionMethod", ConnectionMethod.HTTP.name()));

            if (obj.isNull("connectivityCondition")) {
                if (condition == null)
                    connectivityCondition = ConnectivityCondition.newUniqueCondition();
                else
                    connectivityCondition = condition;
            } else {
                connectivityCondition = new ConnectivityCondition(obj.getJSONObject("connectivityCondition"));
            }

            if (obj.isNull("certificatePath")) {
                certificate = CertUtils.decodeCertificate(obj.optString("certificate", null));
            } else {
                certificate = CertUtils.loadCertificateFromFile(obj.getString("certificatePath"));
            }

            status = new TestStatus(Status.UNKNOWN, null);
        }

        @Nullable
        public String getEncodedCredentials() {
            if (authMethod != AbstractClient.AuthMethod.HTTP) return null;
            if (encodedCredentials == null)
                encodedCredentials = Base64.encodeToString((serverUsername + ":" + serverPassword).getBytes(), Base64.NO_WRAP);
            return encodedCredentials;
        }

        public MultiProfile getParent() {
            return MultiProfile.this;
        }

        String getFullServerAddress() throws NetUtils.InvalidUrlException {
            if (fullServerAddress == null) {
                switch (connectionMethod) {
                    default:
                    case HTTP:
                        fullServerAddress = NetUtils.createHttpURL(this).toString();
                        break;
                    case WEBSOCKET:
                        fullServerAddress = NetUtils.createWebSocketURL(this).toString();
                        break;
                }
            }

            return fullServerAddress;
        }

        JSONObject toJson() throws JSONException {
            JSONObject profile = new JSONObject();
            profile.put("serverAddr", serverAddr)
                    .put("serverPort", serverPort)
                    .put("serverEndpoint", serverEndpoint)
                    .put("authMethod", authMethod.name())
                    .put("serverToken", serverToken)
                    .put("serverUsername", serverUsername)
                    .put("serverPassword", serverPassword)
                    .put("hostnameVerifier", hostnameVerifier)
                    .put("serverSSL", serverSSL)
                    .put("certificate", CertUtils.encodeCertificate(certificate))
                    .put("connectionMethod", connectionMethod.name())
                    .put("connectivityCondition", connectivityCondition.toJson());

            if (directDownload != null) profile.put("directDownload", directDownload.toJson());
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
            if (certificate != null ? !certificate.equals(profile.certificate) : profile.certificate != null)
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
            try {
                return getFullServerAddress();
            } catch (NetUtils.InvalidUrlException ex) {
                Logging.log(ex);
                return "";
            }
        }

        @Override
        public String getInitials(Context context) {
            String name = getProfileName(context);
            if (name.length() < 2) return name;
            else return getProfileName(context).substring(0, 2);
        }
    }
}
