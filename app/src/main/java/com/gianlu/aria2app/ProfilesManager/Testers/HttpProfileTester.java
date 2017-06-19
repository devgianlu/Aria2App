package com.gianlu.aria2app.ProfilesManager.Testers;

import android.content.Context;

import com.gianlu.aria2app.NetIO.HTTPing;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.NetUtils;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class HttpProfileTester extends ProfileTester implements Runnable {

    public HttpProfileTester(Context context, MultiProfile.UserProfile profile, IResult listener) {
        super(context, profile, listener);
    }

    @Override
    public void run() {
        try {
            HttpURLConnection conn;
            if (profile.authMethod.equals(JTA2.AuthMethod.HTTP) && profile.serverUsername != null && profile.serverPassword != null)
                conn = HTTPing.readyHttpConnection(profile.buildHttpUrl(), profile.serverUsername, profile.serverPassword, NetUtils.readyCertificate(context, profile));
            else
                conn = HTTPing.readyHttpConnection(profile.buildHttpUrl(), NetUtils.readyCertificate(context, profile));

            long start = System.currentTimeMillis();
            conn.connect();

            if (conn.getResponseCode() == 400) {
                publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.ONLINE, System.currentTimeMillis() - start));
            } else {
                publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.OFFLINE));
            }
        } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException ex) {
            publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.ERROR));
        }
    }
}