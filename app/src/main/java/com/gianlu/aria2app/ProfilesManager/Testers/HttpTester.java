package com.gianlu.aria2app.ProfilesManager.Testers;

import android.content.Context;

import com.gianlu.aria2app.NetIO.NetUtils;
import com.gianlu.aria2app.NetIO.StatusCodeException;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.Utils;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.Callable;

import okhttp3.OkHttpClient;
import okhttp3.Response;

public class HttpTester extends NetTester implements Callable<Boolean> {

    HttpTester(Context context, MultiProfile.UserProfile profile, IPublish listener) {
        super(context, profile, listener);
    }

    public HttpTester(Context context, MultiProfile.UserProfile profile, IProfileTester profileListener) {
        super(context, profile, profileListener);
    }

    @Override
    public String describe() {
        return "HTTP connection test";
    }

    @Override
    public Boolean call() {
        try {
            OkHttpClient client = NetUtils.buildHttpClient(profile);

            long startTime = System.currentTimeMillis();
            try (Response resp = client.newCall(NetUtils.createGetRequest(profile, null, Utils.readyRequest().put("method", "system.listMethods"))).execute()) {
                boolean a;
                if (resp.code() == 200) {
                    a = true;
                    publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.ONLINE, System.currentTimeMillis() - startTime));
                } else {
                    a = false;
                    publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.OFFLINE, new StatusCodeException(resp)));
                }

                return a;
            }
        } catch (IOException | CertificateException | URISyntaxException | JSONException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException | RuntimeException ex) {
            if (ex instanceof ConnectTimeoutException) {
                publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.OFFLINE, ex));
            } else {
                publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.ERROR, ex));
            }
        }

        return false;
    }
}
