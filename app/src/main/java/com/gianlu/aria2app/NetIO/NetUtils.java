package com.gianlu.aria2app.NetIO;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;

import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

import cz.msebera.android.httpclient.client.config.RequestConfig;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.client.utils.URIBuilder;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;
import cz.msebera.android.httpclient.impl.client.HttpClients;

public class NetUtils {

    @NonNull
    public static SSLContext createSSLContext(@Nullable Certificate ca) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, KeyManagementException {
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

    public static WebSocket readyWebSocket(String url, boolean hostnameVerifier, @NonNull String username, @NonNull String password, @Nullable Certificate ca) throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException, KeyManagementException {
        if (ca != null) {
            WebSocketFactory factory = new WebSocketFactory();
            factory.setVerifyHostname(hostnameVerifier);
            factory.setSSLContext(createSSLContext(ca));

            return factory.createSocket(url, 5000)
                    .addHeader("Authorization", "Basic " + Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP));
        } else {
            return new WebSocketFactory().createSocket(url, 5000)
                    .addHeader("Authorization", "Basic " + Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP));
        }
    }

    public static WebSocket readyWebSocket(String url, boolean hostnameVerifier, @Nullable Certificate ca) throws NoSuchAlgorithmException, IOException, CertificateException, KeyStoreException, KeyManagementException {
        try {
            WebSocketFactory factory = new WebSocketFactory();
            factory.setVerifyHostname(hostnameVerifier);
            factory.setConnectionTimeout(5000);
            if (ca != null) factory.setSSLContext(createSSLContext(ca));
            return factory.createSocket(url, 5000);
        } catch (IllegalArgumentException ex) {
            throw new IOException("Just a wrapper for the real exception", ex);
        }
    }

    public static WebSocket readyWebSocket(MultiProfile.UserProfile profile) throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException, KeyManagementException {
        WebSocketFactory factory = new WebSocketFactory();
        factory.setConnectionTimeout(5000);
        factory.setVerifyHostname(profile.hostnameVerifier);
        if (profile.serverSSL)
            factory.setSSLContext(createSSLContext(profile.certificate));

        try {
            WebSocket socket = factory.createSocket(profile.buildWebSocketUrl(), 5000);
            socket.setFrameQueueSize(15);

            if (profile.authMethod == JTA2.AuthMethod.HTTP)
                socket.addHeader("Authorization", "Basic " + Base64.encodeToString((profile.serverUsername + ":" + profile.serverPassword).getBytes(), Base64.NO_WRAP));

            return socket;
        } catch (IllegalArgumentException ex) {
            throw new IOException(ex);
        }
    }

    public static CloseableHttpClient buildHttpClient(MultiProfile.UserProfile profile) throws CertificateException, IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        return buildHttpClient(profile, createSSLContext(profile.certificate));
    }

    public static CloseableHttpClient buildHttpClient(MultiProfile.UserProfile profile, SSLContext sslContext) {
        HttpClientBuilder builder = HttpClients.custom()
                .setUserAgent("Aria2App")
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(5000)
                        .setSocketTimeout(5000)
                        .setConnectionRequestTimeout(5000)
                        .build())
                .setSslcontext(sslContext);

        if (!profile.hostnameVerifier) {
            builder.setSSLHostnameVerifier(new HostnameVerifier() {
                @SuppressLint("BadHostnameVerifier")
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            });
        }

        return builder.build();
    }

    public static URI createBaseURI(MultiProfile.UserProfile profile) throws URISyntaxException {
        return new URIBuilder()
                .setScheme(profile.serverSSL ? "https" : "http")
                .setHost(profile.serverAddr)
                .setPort(profile.serverPort)
                .setPath(profile.serverEndpoint).build();
    }

    public static HttpGet createGetRequest(MultiProfile.UserProfile profile, @Nullable URI defaultUri, @Nullable JSONObject request) throws URISyntaxException, JSONException {
        if (defaultUri == null) defaultUri = createBaseURI(profile);
        URIBuilder builder = new URIBuilder(defaultUri);
        if (request != null) {
            builder.addParameter("method", request.getString("method"))
                    .addParameter("id", request.getString("id"));

            if (request.has("params"))
                builder.addParameter("params", Base64.encodeToString(request.get("params").toString().getBytes(), Base64.NO_WRAP));
        }

        HttpGet get = new HttpGet(builder.build());
        if (profile.authMethod == JTA2.AuthMethod.HTTP)
            get.addHeader("Authorization", "Basic " + profile.getEncodedCredentials());

        return get;
    }

    public static HttpPost createPostRequest(MultiProfile.UserProfile profile, @Nullable URI defaultUri, @Nullable JSONObject request) throws URISyntaxException, JSONException, UnsupportedEncodingException {
        if (defaultUri == null) defaultUri = createBaseURI(profile);
        HttpPost post = new HttpPost(defaultUri);

        if (request != null)
            post.setEntity(new StringEntity(request.toString(), Charset.forName("UTF-8")));

        if (profile.authMethod == JTA2.AuthMethod.HTTP)
            post.addHeader("Authorization", "Basic " + profile.getEncodedCredentials());

        return post;
    }
}
