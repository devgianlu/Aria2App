package com.gianlu.aria2app.ProfilesManager.Testers;

import android.content.Context;

import com.gianlu.aria2app.NetIO.NetUtils;
import com.gianlu.aria2app.NetIO.StatusCodeException;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.Utils;

import org.json.JSONException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.Callable;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.HttpStatus;
import cz.msebera.android.httpclient.StatusLine;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.conn.ConnectTimeoutException;
import cz.msebera.android.httpclient.conn.HttpHostConnectException;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;

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
        try (CloseableHttpClient client = NetUtils.buildHttpClient(profile)) {
            HttpGet get = NetUtils.createGetRequest(profile, null, Utils.readyRequest().put("method", "system.listMethods"));

            long startTime = System.currentTimeMillis();
            HttpResponse resp = client.execute(get);
            StatusLine sl = resp.getStatusLine();

            boolean a;
            if (sl.getStatusCode() == HttpStatus.SC_OK) {
                a = true;
                publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.ONLINE, System.currentTimeMillis() - startTime));
            } else {
                a = false;
                publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.OFFLINE, new StatusCodeException(sl)));
            }

            get.releaseConnection();
            return a;
        } catch (IOException | CertificateException | URISyntaxException | JSONException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException | RuntimeException ex) {
            if (ex instanceof HttpHostConnectException || ex instanceof ConnectTimeoutException) {
                publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.OFFLINE, ex));
            } else {
                publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.ERROR, ex));
            }
        }

        return false;
    }
}
