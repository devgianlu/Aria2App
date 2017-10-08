package com.gianlu.aria2app.ProfilesManager.Testers;

import android.content.Context;

import com.gianlu.aria2app.NetIO.HTTPing;
import com.gianlu.aria2app.NetIO.IConnect;
import com.gianlu.aria2app.NetIO.NetUtils;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.commonutils.Logging;

import org.json.JSONException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.HttpStatus;
import cz.msebera.android.httpclient.StatusLine;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.conn.ConnectTimeoutException;
import cz.msebera.android.httpclient.conn.HttpHostConnectException;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;

public class HttpProfileTester extends NetProfileTester {
    public HttpProfileTester(Context context, MultiProfile.UserProfile profile, ITesting listener) {
        super(context, profile, listener);
    }

    @Override
    public void getClient(IConnect listener) {
        try {
            listener.onConnected(new HTTPing(context, profile));
        } catch (CertificateException | IOException | URISyntaxException | KeyManagementException | NoSuchAlgorithmException | KeyStoreException ex) {
            listener.onFailedConnecting(ex);
            Logging.logMe(context, ex);
        }
    }

    @Override
    public void run() {
        publishUpdate("Started connection test...");

        try (CloseableHttpClient client = NetUtils.buildHttpClient(context, profile)) {
            HttpGet get = NetUtils.createGetRequest(profile, null, null);
            HttpResponse resp = client.execute(get);
            StatusLine sl = resp.getStatusLine();

            if (sl.getStatusCode() == HttpStatus.SC_BAD_REQUEST) {
                publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.ONLINE, System.currentTimeMillis() - startTime));
                publishUpdate("Connection took " + (System.currentTimeMillis() - startTime) + "ms.");
            } else {
                publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.OFFLINE));
                publishUpdate(sl.getStatusCode() + ": " + sl.getReasonPhrase());
            }

            get.releaseConnection();
        } catch (IOException | CertificateException | URISyntaxException | JSONException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException | RuntimeException ex) {
            if (ex instanceof HttpHostConnectException || ex instanceof ConnectTimeoutException) {
                publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.OFFLINE));
            } else {
                publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.ERROR));
            }

            if (ex.getMessage() == null) {
                if (ex.getCause() != null) publishUpdate(ex.getCause().getMessage());
                else publishUpdate(null);
            } else {
                publishUpdate(ex.getMessage());
            }
        }
    }
}