package com.gianlu.aria2app.NetIO;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Base64;

import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class NetUtils {

    public static SSLContext readySSLContext(Certificate ca) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, KeyManagementException {
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

    @Nullable
    public static Certificate readyCertificate(Context context) throws CertificateException, FileNotFoundException {
        return readyCertificate(context, ProfilesManager.get(context).getCurrentAssert().getProfile(context));
    }

    @Nullable
    public static Certificate readyCertificate(Context context, MultiProfile.UserProfile profile) throws CertificateException, FileNotFoundException {
        if (!profile.serverSSL || profile.certificatePath == null || profile.certificatePath.isEmpty())
            return null;

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            return null;

        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return factory.generateCertificate(new FileInputStream(profile.certificatePath));
    }

    public static WebSocket readyWebSocket(String url, @NonNull String username, @NonNull String password, @Nullable Certificate ca) throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException, KeyManagementException {
        if (ca != null) {
            WebSocketFactory factory = new WebSocketFactory();
            factory.setSSLContext(readySSLContext(ca));

            return factory.createSocket(url, 5000)
                    .addHeader("Authorization", "Basic " + Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP));
        } else {
            return new WebSocketFactory().createSocket(url, 5000)
                    .addHeader("Authorization", "Basic " + Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP));
        }
    }

    public static WebSocket readyWebSocket(String url, @Nullable Certificate ca) throws NoSuchAlgorithmException, IOException, CertificateException, KeyStoreException, KeyManagementException, IllegalArgumentException {
        WebSocketFactory factory = new WebSocketFactory();
        factory.setConnectionTimeout(5000);
        if (ca != null) factory.setSSLContext(readySSLContext(ca));
        return factory.createSocket(url, 5000);
    }

    public static WebSocket readyWebSocket(Context context) throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException, KeyManagementException {
        MultiProfile.UserProfile profile = ProfilesManager.get(context).getCurrentAssert().getProfile(context);

        WebSocketFactory factory = new WebSocketFactory();
        factory.setConnectionTimeout(5000);
        if (profile.serverSSL) factory.setSSLContext(readySSLContext(readyCertificate(context)));
        WebSocket socket = factory.createSocket(profile.buildWebSocketUrl(), 5000);

        if (profile.authMethod == JTA2.AuthMethod.HTTP)
            socket.addHeader("Authorization", "Basic " + Base64.encodeToString((profile.serverUsername + ":" + profile.serverPassword).getBytes(), Base64.NO_WRAP));

        return socket;
    }
}
