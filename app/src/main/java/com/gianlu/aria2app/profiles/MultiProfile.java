package com.gianlu.aria2app.profiles;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.aria2app.R;
import com.gianlu.aria2app.ThisApplication;
import com.gianlu.aria2app.activities.editprofile.AuthenticationFragment;
import com.gianlu.aria2app.activities.editprofile.ConnectionFragment;
import com.gianlu.aria2app.activities.editprofile.DirectDownloadFragment;
import com.gianlu.aria2app.api.AbstractClient;
import com.gianlu.aria2app.api.CertUtils;
import com.gianlu.aria2app.api.NetUtils;
import com.gianlu.aria2lib.Aria2PK;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.drawer.BaseDrawerProfile;
import com.gianlu.commonutils.preferences.Prefs;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import okhttp3.HttpUrl;

public class MultiProfile implements BaseDrawerProfile, Serializable {
    public static final String IN_APP_DOWNLOADER_NAME = "In-App downloader";
    private static final long serialVersionUID = 1L;
    public final ArrayList<UserProfile> profiles;
    public final String id;
    public final String name;
    public final boolean notificationsEnabled;
    public transient TestStatus status;

    public MultiProfile(@NonNull String name, boolean enableNotifs) {
        this.name = name;
        this.notificationsEnabled = enableNotifs;
        this.id = ProfilesManager.getId(name);
        this.profiles = new ArrayList<>();
        this.status = new TestStatus(Status.UNKNOWN, null);
    }

    MultiProfile(@NonNull JSONObject obj) throws JSONException {
        this.name = obj.getString("name");
        this.notificationsEnabled = obj.optBoolean("notificationsEnabled", true);
        this.id = ProfilesManager.getId(name);
        this.status = new TestStatus(Status.UNKNOWN, null);

        this.profiles = new ArrayList<>();
        if (obj.has("serverAddr")) { // Needed for backward compatibility
            UserProfile unique = new UserProfile(obj);
            this.profiles.add(unique);
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
                this.profiles.add(new UserProfile(profile, condition));
            }
        } else {
            JSONArray profilesArray = obj.getJSONArray("profiles");
            for (int i = 0; i < profilesArray.length(); i++)
                this.profiles.add(new UserProfile(profilesArray.getJSONObject(i)));
        }
    }

    @NonNull
    public static MultiProfile forInAppDownloader() {
        int port = ThreadLocalRandom.current().nextInt(2000, 8000);
        Prefs.putInt(Aria2PK.RPC_PORT, port);

        String token = CommonUtils.randomString(8, ThreadLocalRandom.current());
        Prefs.putString(Aria2PK.RPC_TOKEN, token);

        MultiProfile profile = new MultiProfile(IN_APP_DOWNLOADER_NAME, true);
        profile.add(ConnectivityCondition.newUniqueCondition(),
                new ConnectionFragment.Fields(ConnectionMethod.WEBSOCKET, "localhost", port, "/jsonrpc", false, null, false),
                new AuthenticationFragment.Fields(AbstractClient.AuthMethod.TOKEN, token, null, null),
                new DirectDownloadFragment.Fields(null));

        return profile;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MultiProfile profile = (MultiProfile) o;
        return id.equals(profile.id);
    }

    @Nullable
    public String shouldSkipVersionCheck(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("a2_skipVersionCheck_" + id, null);
    }

    public void skipVersionCheck(Context context, String version) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString("a2_skipVersionCheck_" + id, version).apply();
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
    public UserProfile getProfile(@NotNull ProfilesManager manager) {
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

    @NonNull
    @Override
    public String getPrimaryText(@NonNull Context context) {
        return getProfile(context).getPrimaryText(context);
    }

    @NonNull
    @Override
    public String getSecondaryText(@NonNull Context context) {
        return getProfile(context).getSecondaryText(context);
    }

    @NonNull
    JSONObject toJson() throws JSONException {
        if (profiles.isEmpty()) throw new IllegalStateException("profiles cannot be empty!");

        JSONObject obj = new JSONObject();
        obj.put("name", name).put("notificationsEnabled", notificationsEnabled);

        JSONArray profilesArray = new JSONArray();
        for (UserProfile profile : profiles) profilesArray.put(profile.toJson());

        obj.put("profiles", profilesArray);
        return obj;
    }

    @NotNull
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

    public boolean isInAppDownloader() {
        return name.equals(IN_APP_DOWNLOADER_NAME);
    }

    public boolean isEmpty() {
        return profiles.isEmpty();
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
        private static final long serialVersionUID = 1L;
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

        @NonNull
        public static ConnectivityCondition newWiFiCondition(String[] ssids, boolean isDefault) {
            return new ConnectivityCondition(Type.WIFI, isDefault, ssids);
        }

        @NonNull
        public static ConnectivityCondition newMobileCondition(boolean isDefault) {
            return new ConnectivityCondition(Type.MOBILE, isDefault, null);
        }

        @NonNull
        public static ConnectivityCondition newBluetoothCondition(boolean isDefault) {
            return new ConnectivityCondition(Type.BLUETOOTH, isDefault, null);
        }

        @NonNull
        public static ConnectivityCondition newEthernetCondition(boolean isDefault) {
            return new ConnectivityCondition(Type.ETHERNET, isDefault, null);
        }

        @NonNull
        public static ConnectivityCondition newUniqueCondition() {
            return new ConnectivityCondition(Type.ALWAYS, true, null);
        }

        @NotNull
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

        @NonNull
        public ConnectivityCondition changeDefaultValue(boolean val) {
            return new ConnectivityCondition(type, ssids, val);
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
        private static final long serialVersionUID = 1L;
        public final Type type;
        public final Ftp ftp;
        public final Sftp sftp;
        public final Web web;
        public final Smb smb;

        public DirectDownload(@Nullable Web web, @Nullable Ftp ftp, @Nullable Sftp sftp, @Nullable Smb smb) {
            this.ftp = ftp;
            this.sftp = sftp;
            this.web = web;
            this.smb = smb;

            if (ftp == null && web == null && smb == null && sftp == null)
                throw new IllegalArgumentException();

            if (web != null && (ftp != null || sftp != null || smb != null))
                throw new IllegalArgumentException();

            if (ftp != null && (sftp != null || smb != null))
                throw new IllegalArgumentException();

            if (sftp != null && smb != null)
                throw new IllegalArgumentException();

            if (smb != null) type = Type.SMB;
            else if (ftp != null) type = Type.FTP;
            else if (sftp != null) type = Type.SFTP;
            else type = Type.SMB;
        }

        public DirectDownload(@NonNull JSONObject obj) throws JSONException {
            if (obj.has("type")) {
                type = Type.parse(obj.getString("type"));
                switch (type) {
                    case WEB:
                        web = new Web(obj.getJSONObject("web"));
                        smb = null;
                        ftp = null;
                        sftp = null;
                        break;
                    case FTP:
                        web = null;
                        smb = null;
                        ftp = new Ftp(obj.getJSONObject("ftp"));
                        sftp = null;
                        break;
                    case SFTP:
                        web = null;
                        smb = null;
                        ftp = null;
                        sftp = new Sftp(obj.getJSONObject("sftp"));
                        break;
                    case SMB:
                        web = null;
                        smb = new Smb(obj.getJSONObject("smb"));
                        ftp = null;
                        sftp = null;
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown type: " + type);
                }
            } else {
                type = Type.WEB;
                web = new Web(obj);
                ftp = null;
                smb = null;
                sftp = null;
            }
        }

        @NonNull
        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("type", type.val);
            switch (type) {
                case WEB:
                    obj.put("web", web.toJson());
                    break;
                case FTP:
                    obj.put("ftp", ftp.toJson());
                    break;
                case SFTP:
                    obj.put("sftp", sftp.toJson());
                    break;
                case SMB:
                    obj.put("smb", smb.toJson());
                    break;
            }

            return obj;
        }

        public enum Type {
            WEB("web"), FTP("ftp"), SFTP("sftp"), SMB("smb");

            final String val;

            Type(@NonNull String val) {
                this.val = val;
            }

            @NotNull
            public static Type parse(@NotNull String val) {
                for (Type type : values())
                    if (type.val.equals(val))
                        return type;

                throw new IllegalArgumentException("Unknown type: " + val);
            }
        }

        public static class Ftp {
            public final String hostname;
            public final int port;
            public final String username;
            public final String password;
            public final String path;
            public final boolean hostnameVerifier;
            public final X509Certificate certificate;
            public final boolean serverSsl;

            Ftp(@NonNull JSONObject obj) throws JSONException {
                hostname = obj.getString("hostname");
                port = obj.getInt("port");
                username = obj.getString("username");
                password = obj.getString("password");
                path = obj.getString("path");
                hostnameVerifier = obj.optBoolean("hostnameVerifier", false);
                serverSsl = obj.optBoolean("serverSsl", false);

                String base64 = CommonUtils.optString(obj, "certificate");
                if (base64 == null) certificate = null;
                else certificate = CertUtils.decodeCertificate(base64);
            }

            @NonNull
            public JSONObject toJson() throws JSONException {
                JSONObject obj = new JSONObject();
                obj.put("hostname", hostname).put("port", port).put("serverSsl", serverSsl)
                        .put("username", username).put("password", password).put("path", path)
                        .put("hostnameVerifier", hostnameVerifier);

                if (certificate != null && serverSsl)
                    obj.put("certificate", CertUtils.encodeCertificate(certificate));

                return obj;
            }
        }

        public static class Sftp {
            public final String hostname;
            public final int port;
            public final String username;
            public final String password;

            Sftp(@NonNull JSONObject obj) throws JSONException {
                hostname = obj.getString("hostname");
                port = obj.getInt("port");
                username = obj.getString("username");
                password = obj.getString("password");
            }

            public Sftp(@NonNull String hostname, int port, @NonNull String username, @NonNull String password) {
                this.hostname = hostname;
                this.port = port;
                this.username = username;
                this.password = password;
            }

            @NonNull
            public JSONObject toJson() throws JSONException {
                return new JSONObject().put("hostname", hostname).put("port", port)
                        .put("username", username).put("password", password);
            }
        }

        public static class Smb {

            Smb(@NonNull JSONObject obj) {

            }

            @NonNull
            public JSONObject toJson() {
                return null; // TODO
            }
        }

        public static class Web {
            public final String address;
            public final boolean auth;
            public final String username;
            public final String password;
            public final boolean hostnameVerifier;
            public final X509Certificate certificate;
            public final boolean serverSsl;
            private transient HttpUrl cachedUri;

            Web(@NotNull JSONObject obj) throws JSONException {
                address = obj.getString("addr");
                auth = obj.getBoolean("auth");
                username = CommonUtils.optString(obj, "username");
                password = CommonUtils.optString(obj, "password");
                hostnameVerifier = obj.optBoolean("hostnameVerifier", false);
                serverSsl = obj.optBoolean("serverSsl", false);

                String base64 = CommonUtils.optString(obj, "certificate");
                if (base64 == null) certificate = null;
                else certificate = CertUtils.decodeCertificate(base64);
            }

            public Web(String address, boolean auth, @Nullable String username, @Nullable String password, boolean serverSsl, @Nullable X509Certificate certificate, boolean hostnameVerifier) {
                this.address = address;
                this.auth = auth;
                this.username = username;
                this.password = password;
                this.serverSsl = serverSsl;
                this.certificate = certificate;
                this.hostnameVerifier = hostnameVerifier;
            }

            public String getAuthorizationHeader() {
                if (!auth) return null;
                return "Basic " + Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP);
            }

            @NonNull
            JSONObject toJson() throws JSONException {
                JSONObject obj = new JSONObject();
                obj.put("addr", address).put("auth", auth).put("serverSsl", serverSsl)
                        .put("username", username).put("password", password)
                        .put("hostnameVerifier", hostnameVerifier);

                if (certificate != null && serverSsl)
                    obj.put("certificate", CertUtils.encodeCertificate(certificate));

                return obj;
            }

            @Nullable
            public HttpUrl getUrl() {
                if (cachedUri == null) cachedUri = HttpUrl.parse(address);
                return cachedUri;
            }
        }
    }

    public static class TestStatus {
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
        private static final long serialVersionUID = 1L;
        public final String serverAddr;
        public final int serverPort;
        public final String serverEndpoint;
        public final AbstractClient.AuthMethod authMethod;
        public final boolean serverSsl;
        public final boolean hostnameVerifier;
        public final X509Certificate certificate;
        public final String serverUsername;
        public final String serverPassword;
        public final String serverToken;
        public final DirectDownload directDownload;
        public final ConnectionMethod connectionMethod;
        public final ConnectivityCondition connectivityCondition;
        private transient String encodedCredentials;
        private transient String fullServerAddress;

        public UserProfile(@NonNull JSONObject obj) throws JSONException {
            this(obj, null);
        }

        public UserProfile(@NonNull ConnectivityCondition cond, ConnectionFragment.Fields connFields, AuthenticationFragment.Fields authFields, DirectDownloadFragment.Fields ddFields) {
            connectivityCondition = cond;
            authMethod = authFields.authMethod;
            serverUsername = authFields.username;
            serverPassword = authFields.password;
            serverToken = authFields.token;
            connectionMethod = connFields.connectionMethod;
            serverSsl = connFields.encryption;
            certificate = connFields.certificate;
            serverAddr = connFields.address;
            serverPort = connFields.port;
            serverEndpoint = connFields.endpoint;
            directDownload = ddFields.dd;
            hostnameVerifier = connFields.hostnameVerifier;
        }

        public UserProfile(@NonNull JSONObject obj, @Nullable ConnectivityCondition condition) throws JSONException {
            if (obj.has("serverAuth"))
                authMethod = AbstractClient.AuthMethod.TOKEN;
            else
                authMethod = AbstractClient.AuthMethod.valueOf(obj.optString("authMethod", "NONE"));

            serverUsername = CommonUtils.optString(obj, "serverUsername");
            serverPassword = CommonUtils.optString(obj, "serverPassword");
            serverToken = CommonUtils.optString(obj, "serverToken");
            serverSsl = obj.optBoolean("serverSsl", false);

            serverAddr = obj.getString("serverAddr");
            serverPort = obj.getInt("serverPort");
            serverEndpoint = obj.getString("serverEndpoint");
            hostnameVerifier = obj.optBoolean("hostnameVerifier", false);

            if (obj.has("directDownload"))
                directDownload = new DirectDownload(obj.getJSONObject("directDownload"));
            else
                directDownload = null;

            connectionMethod = ConnectionMethod.valueOf(obj.optString("connectionMethod", ConnectionMethod.HTTP.name()));

            if (obj.isNull("connectivityCondition")) {
                if (condition != null) connectivityCondition = condition;
                else connectivityCondition = ConnectivityCondition.newUniqueCondition();
            } else {
                connectivityCondition = new ConnectivityCondition(obj.getJSONObject("connectivityCondition"));
            }

            if (obj.isNull("certificatePath")) {
                String base64 = CommonUtils.optString(obj, "certificate");
                if (base64 == null) certificate = null;
                else certificate = CertUtils.decodeCertificate(base64);
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

        @NonNull
        public MultiProfile getParent() {
            return MultiProfile.this;
        }

        @NonNull
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

        @NonNull
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
                    .put("serverSsl", serverSsl)
                    .put("connectionMethod", connectionMethod.name())
                    .put("connectivityCondition", connectivityCondition.toJson());

            if (certificate != null && serverSsl)
                profile.put("certificate", CertUtils.encodeCertificate(certificate));

            if (directDownload != null)
                profile.put("directDownload", directDownload.toJson());

            return profile;
        }

        @Override
        @SuppressWarnings("SimplifiableIfStatement")
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UserProfile profile = (UserProfile) o;
            if (serverPort != profile.serverPort) return false;
            if (serverSsl != profile.serverSsl) return false;
            if (!serverAddr.equals(profile.serverAddr)) return false;
            if (!serverEndpoint.equals(profile.serverEndpoint)) return false;
            if (authMethod != profile.authMethod) return false;
            if (!Objects.equals(certificate, profile.certificate)) return false;
            if (!Objects.equals(serverUsername, profile.serverUsername)) return false;
            if (!Objects.equals(serverPassword, profile.serverPassword)) return false;
            if (!Objects.equals(serverToken, profile.serverToken)) return false;
            if (connectionMethod != profile.connectionMethod) return false;
            return connectivityCondition.equals(profile.connectivityCondition);
        }

        @NonNull
        @Override
        public String getPrimaryText(@NonNull Context context) {
            return name;
        }

        @NonNull
        @Override
        public String getSecondaryText(@NonNull Context context) {
            if (isInAppDownloader()) {
                ThisApplication app = ((ThisApplication) context.getApplicationContext());
                if (app.hasAria2ServiceEnv()) {
                    if (app.getLastAria2UiState())
                        return context.getString(R.string.inAppDownloader_serviceStarted);
                    else
                        return context.getString(R.string.inAppDownloader_serviceNotStarted);
                } else {
                    return context.getString(R.string.inAppDownloader_serviceNotConfigured);
                }
            }

            try {
                return getFullServerAddress();
            } catch (NetUtils.InvalidUrlException ex) {
                return "";
            }
        }

        public boolean isInAppDownloader() {
            return name.equals(IN_APP_DOWNLOADER_NAME);
        }
    }
}
