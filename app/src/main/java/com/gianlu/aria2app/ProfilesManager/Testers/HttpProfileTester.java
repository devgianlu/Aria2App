package com.gianlu.aria2app.ProfilesManager.Testers;

import android.content.Context;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.HTTPing;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.NetUtils;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.commonutils.Logging;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class HttpProfileTester extends NetProfileTester implements Runnable {

    public HttpProfileTester(Context context, MultiProfile.UserProfile profile, ITesting listener) {
        super(context, profile, listener);
    }

    @Override
    @Nullable
    public AbstractClient getClient() {
        try {
            return new HTTPing(context, profile);
        } catch (CertificateException | IOException | KeyManagementException | NoSuchAlgorithmException | KeyStoreException ex) {
            Logging.logMe(context, ex);
            return null;
        }
    }

    @Override
    public void run() {
        publishUpdate("Started connection test");

        try {
            HttpURLConnection conn;
            if (profile.authMethod.equals(JTA2.AuthMethod.HTTP) && profile.serverUsername != null && profile.serverPassword != null)
                conn = NetUtils.readyHttpConnection(profile.buildHttpUrl(), profile.serverUsername, profile.serverPassword, NetUtils.readyCertificate(context, profile));
            else
                conn = NetUtils.readyHttpConnection(profile.buildHttpUrl(), NetUtils.readyCertificate(context, profile));

            conn.connect();

            if (conn.getResponseCode() == 400) {
                publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.ONLINE, System.currentTimeMillis() - startTime));
                publishUpdate("Connection took " + (System.currentTimeMillis() - startTime) + "ms");
            } else {
                publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.OFFLINE));
                publishUpdate(conn.getResponseCode() + ": " + conn.getResponseMessage());
            }
        } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException ex) {
            publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.ERROR));
            publishUpdate(ex.getMessage());
        }
    }
}