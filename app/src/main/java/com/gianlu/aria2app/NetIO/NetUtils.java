package com.gianlu.aria2app.NetIO;

import android.util.Base64;

import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.commonutils.Logging;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public final class NetUtils {
    public static final int HTTP_TIMEOUT = 5; // sec

    public static boolean isUrlValid(String address, int port, String endpoint, boolean encryption) {
        try {
            new HttpUrl.Builder()
                    .scheme(encryption ? "https" : "http")
                    .host(address)
                    .port(port)
                    .addPathSegments(endpoint.charAt(0) == '/' ? endpoint.substring(1) : endpoint)
                    .build();

            return true;
        } catch (Exception ex) {
            Logging.log(ex);
            return false;
        }
    }

    public static void setSslSocketFactory(@NonNull OkHttpClient.Builder builder, @NonNull Certificate ca) throws GeneralSecurityException, IOException {
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

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{trustManagers[0]}, null);
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustManagers[0]);
    }

    @NonNull
    public static OkHttpClient buildClient(@NonNull MultiProfile.UserProfile profile) throws GeneralSecurityException, IOException {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(HTTP_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(HTTP_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(HTTP_TIMEOUT, TimeUnit.SECONDS);

        if (profile.certificate != null)
            setSslSocketFactory(builder, profile.certificate);

        if (!profile.hostnameVerifier) {
            builder.hostnameVerifier((s, sslSession) -> true);
        }

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
            body = RequestBody.create(MediaType.parse("application/json"), request.toString());
        else
            body = RequestBody.create(null, new byte[0]);

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

    @NonNull
    public static Request createDirectDownloadRequest(@NonNull MultiProfile.DirectDownload dd) throws InvalidUrlException {
        HttpUrl baseUrl = dd.getUrl();
        if (baseUrl == null)
            throw new InvalidUrlException(new NullPointerException("DirectDownload url is invalid."));

        Request.Builder builder = new Request.Builder()
                .get().url(baseUrl);

        if (dd.auth)
            builder.header("Authorization", "Basic " + Base64.encodeToString((dd.username + ":" + dd.password).getBytes(), Base64.NO_WRAP));

        return builder.build();
    }

    public static class InvalidUrlException extends Exception {
        InvalidUrlException(Throwable cause) {
            super(cause);
        }
    }
}
