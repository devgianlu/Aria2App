package com.gianlu.aria2app.NetIO;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.commonutils.Logging;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

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

    @NonNull
    static SSLContext createSSLContext(@Nullable Certificate ca) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, KeyManagementException {
        if (ca == null) return SSLContext.getDefault();

        String keyStoreType = KeyStore.getDefaultType();
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);

        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init(keyStore);

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, tmf.getTrustManagers(), null);

        return context;
    }

    private static void setSslSocketFactory(OkHttpClient.Builder builder, SSLContext sslContext) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager))
            return;

        X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
        if (SSLContext.getDefault() != sslContext)
            sslContext.init(null, new TrustManager[]{trustManager}, null);

        builder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
    }

    public static OkHttpClient buildClient(MultiProfile.UserProfile profile) throws CertificateException, IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        return buildClient(profile, createSSLContext(profile.certificate));
    }

    static OkHttpClient buildClient(MultiProfile.UserProfile profile, SSLContext sslContext) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(HTTP_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(HTTP_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(HTTP_TIMEOUT, TimeUnit.SECONDS);

        setSslSocketFactory(builder, sslContext);

        if (!profile.hostnameVerifier) {
            builder.hostnameVerifier(new HostnameVerifier() {
                @SuppressLint("BadHostnameVerifier")
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            });
        }

        return builder.build();
    }

    @NonNull
    public static URI createHttpURL(MultiProfile.UserProfile profile) throws InvalidUrlException {
        try {
            return new URI(profile.serverSSL ? "https" : "http", null, profile.serverAddr, profile.serverPort, profile.serverEndpoint, null, null);
        } catch (Exception ex) {
            throw new InvalidUrlException(ex);
        }
    }

    @NonNull
    public static URI createWebSocketURL(MultiProfile.UserProfile profile) throws InvalidUrlException {
        try {
            return new URI(profile.serverSSL ? "wss" : "ws", null, profile.serverAddr, profile.serverPort, profile.serverEndpoint, null, null);
        } catch (Exception ex) {
            throw new InvalidUrlException(ex);
        }
    }

    @NonNull
    static Request createPostRequest(MultiProfile.UserProfile profile, @Nullable URI url, @Nullable JSONObject request) throws InvalidUrlException {
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
    public static Request createWebsocketRequest(MultiProfile.UserProfile profile) throws InvalidUrlException {
        Request.Builder builder = new Request.Builder();
        builder.url(createWebSocketURL(profile).toString());
        return builder.build();
    }

    public static class InvalidUrlException extends Exception {
        InvalidUrlException(Throwable cause) {
            super(cause);
        }
    }
}
