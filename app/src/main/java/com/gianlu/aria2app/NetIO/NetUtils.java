package com.gianlu.aria2app.NetIO;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;

import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.commonutils.Logging;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;

import org.json.JSONException;
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

public class NetUtils {
    private static final int TIMEOUT = 5; // sec

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

    public static WebSocket readyWebSocket(MultiProfile.UserProfile profile) throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException, KeyManagementException, InvalidUrlException {
        WebSocketFactory factory = new WebSocketFactory();
        factory.setConnectionTimeout(5000);
        factory.setVerifyHostname(profile.hostnameVerifier);
        if (profile.serverSSL)
            factory.setSSLContext(createSSLContext(profile.certificate));

        try {
            WebSocket socket = factory.createSocket(createBaseWsURI(profile), 5000);
            socket.setFrameQueueSize(15);

            if (profile.authMethod == JTA2.AuthMethod.HTTP)
                socket.addHeader("Authorization", "Basic " + Base64.encodeToString((profile.serverUsername + ":" + profile.serverPassword).getBytes(), Base64.NO_WRAP));

            return socket;
        } catch (IllegalArgumentException ex) {
            throw new IOException("Just a wrapper", ex);
        }
    }

    public static OkHttpClient buildHttpClient(MultiProfile.UserProfile profile) throws CertificateException, IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        return buildHttpClient(profile, createSSLContext(profile.certificate));
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

    static OkHttpClient buildHttpClient(MultiProfile.UserProfile profile, SSLContext sslContext) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT, TimeUnit.SECONDS);

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
    public static HttpUrl createBaseHttpURI(MultiProfile.UserProfile profile) throws InvalidUrlException {
        try {
            HttpUrl.Builder builder = new HttpUrl.Builder();
            builder.scheme(profile.serverSSL ? "https" : "http")
                    .host(profile.serverAddr)
                    .port(profile.serverPort)
                    .addPathSegments(profile.serverEndpoint.charAt(0) == '/' ? profile.serverEndpoint.substring(1) : profile.serverEndpoint);

            return builder.build();
        } catch (Exception ex) {
            throw new InvalidUrlException(ex);
        }
    }

    @NonNull
    public static URI createBaseWsURI(MultiProfile.UserProfile profile) throws InvalidUrlException {
        try {
            return new URI(profile.serverSSL ? "wss" : "ws", null, profile.serverAddr, profile.serverPort, profile.serverEndpoint, null, null);
        } catch (Exception ex) {
            throw new InvalidUrlException(ex);
        }
    }

    public static Request createGetRequest(MultiProfile.UserProfile profile, @Nullable HttpUrl defaultUri, @Nullable JSONObject request) throws JSONException, InvalidUrlException {
        if (defaultUri == null) defaultUri = createBaseHttpURI(profile);

        HttpUrl.Builder uri = defaultUri.newBuilder();
        if (request != null) {
            uri.addQueryParameter("method", request.getString("method"))
                    .addQueryParameter("id", request.getString("id"));

            if (request.has("params"))
                uri.addQueryParameter("params", Base64.encodeToString(request.get("params").toString().getBytes(), Base64.NO_WRAP));
        }

        Request.Builder builder = new Request.Builder();
        builder.url(uri.build()).get();

        if (profile.authMethod == JTA2.AuthMethod.HTTP)
            builder.header("Authorization", "Basic " + profile.getEncodedCredentials());

        return builder.build();
    }

    static Request createPostRequest(MultiProfile.UserProfile profile, @Nullable HttpUrl defaultUri, @Nullable JSONObject request) throws InvalidUrlException {
        if (defaultUri == null) defaultUri = createBaseHttpURI(profile);
        Request.Builder builder = new Request.Builder();
        builder.url(defaultUri);

        RequestBody body;
        if (request != null)
            body = RequestBody.create(MediaType.parse("application/json"), request.toString());
        else
            body = RequestBody.create(null, new byte[0]);

        builder.post(body);

        if (profile.authMethod == JTA2.AuthMethod.HTTP)
            builder.header("Authorization", "Basic " + profile.getEncodedCredentials());

        return builder.build();
    }

    public static class InvalidUrlException extends Exception {
        InvalidUrlException(Throwable cause) {
            super(cause);
        }
    }
}
