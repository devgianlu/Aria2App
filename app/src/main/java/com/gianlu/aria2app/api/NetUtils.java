package com.gianlu.aria2app.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.aria2app.PK;
import com.gianlu.aria2app.profiles.MultiProfile;
import com.gianlu.commonutils.preferences.Prefs;

import org.apache.commons.net.ftp.FTPSClient;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public final class NetUtils {
    public static boolean isUrlValid(String address, int port, String endpoint, boolean encryption) {
        try {
            new HttpUrl.Builder()
                    .scheme(encryption ? "https" : "http")
                    .host(address)
                    .port(port)
                    .addPathSegments(endpoint.charAt(0) == '/' ? endpoint.substring(1) : endpoint)
                    .build();

            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    //region SSL

    @NonNull
    private static X509TrustManager getTrustManager(@NonNull Certificate ca) throws GeneralSecurityException, IOException {
        char[] password = "password".toCharArray();
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, password);
        keyStore.setCertificateEntry("ca", ca);

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager))
            throw new GeneralSecurityException("Unexpected default trust managers:" + Arrays.toString(trustManagers));

        return (X509TrustManager) trustManagers[0];
    }

    @NonNull
    private static SSLSocketFactory getSslSocketFactory(@NonNull TrustManager trustManager) throws GeneralSecurityException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{trustManager}, null);
        return sslContext.getSocketFactory();
    }

    public static void setSslSocketFactory(@NonNull FTPSClient client, @NonNull Certificate ca) throws GeneralSecurityException, IOException {
        X509TrustManager trustManager = getTrustManager(ca);
        SSLSocketFactory sslSocketFactory = getSslSocketFactory(trustManager);
        client.setTrustManager(trustManager);
        client.setSocketFactory(sslSocketFactory);
    }

    public static void setSslSocketFactory(@NonNull OkHttpClient.Builder client, @NonNull Certificate ca) throws GeneralSecurityException, IOException {
        X509TrustManager trustManager = getTrustManager(ca);
        SSLSocketFactory sslSocketFactory = getSslSocketFactory(trustManager);
        client.sslSocketFactory(sslSocketFactory, trustManager);
    }

    //endregion

    @NonNull
    public static OkHttpClient buildClient(@NonNull MultiProfile.UserProfile profile) throws GeneralSecurityException, IOException {
        int timeout = Prefs.getInt(PK.A2_NETWORK_TIMEOUT);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS);

        if (profile.certificate != null)
            setSslSocketFactory(builder, profile.certificate);

        if (!profile.hostnameVerifier)
            builder.hostnameVerifier((s, sslSession) -> true);

        return builder.build();
    }

    @NonNull
    public static URI createHttpURL(@NonNull MultiProfile.UserProfile profile) throws InvalidUrlException {
        try {
            return new URI(profile.serverSsl ? "https" : "http", null, profile.serverAddr, profile.serverPort, profile.serverEndpoint, null, null);
        } catch (Exception ex) {
            throw new InvalidUrlException(ex);
        }
    }

    @NonNull
    public static URI createWebSocketURL(@NonNull MultiProfile.UserProfile profile) throws InvalidUrlException {
        try {
            return new URI(profile.serverSsl ? "wss" : "ws", null, profile.serverAddr, profile.serverPort, profile.serverEndpoint, null, null);
        } catch (Exception ex) {
            throw new InvalidUrlException(ex);
        }
    }

    @NonNull
    static Request createPostRequest(@NonNull MultiProfile.UserProfile profile, @Nullable URI url, @Nullable JSONObject request) throws InvalidUrlException {
        if (url == null) url = createHttpURL(profile);
        Request.Builder builder = new Request.Builder();
        builder.url(url.toString());

        RequestBody body;
        if (request != null)
            body = RequestBody.create(request.toString(), MediaType.parse("application/json"));
        else
            body = RequestBody.create(new byte[0], null);

        builder.post(body);

        if (profile.authMethod == AbstractClient.AuthMethod.HTTP)
            builder.header("Authorization", "Basic " + profile.getEncodedCredentials());

        return builder.build();
    }

    @NonNull
    public static Request createWebsocketRequest(@NonNull MultiProfile.UserProfile profile) throws InvalidUrlException {
        Request.Builder builder = new Request.Builder();
        builder.url(createWebSocketURL(profile).toString());
        return builder.build();
    }

    public static class InvalidUrlException extends Exception {
        InvalidUrlException(String message) {
            super(message);
        }

        InvalidUrlException(Throwable cause) {
            super(cause);
        }
    }
}
