package com.gianlu.aria2app.ProfilesManager.Testers;


import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Base64;

import com.gianlu.aria2app.NetIO.StatusCodeException;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.R;

import java.io.IOException;
import java.net.URISyntaxException;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.HttpStatus;
import cz.msebera.android.httpclient.StatusLine;
import cz.msebera.android.httpclient.client.config.RequestConfig;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;

class DirectDownloadTester extends BaseTester {
    private final MultiProfile.DirectDownload dd;

    DirectDownloadTester(Context context, MultiProfile.UserProfile profile, @Nullable IPublish listener) {
        super(context, profile, listener);
        this.dd = profile.directDownload;
    }

    private void publishError(Exception ex) {
        if (ex instanceof StatusCodeException) {
            publishMessage("Server returned " + ex.getMessage(), R.color.red);
            if (((StatusCodeException) ex).code == HttpStatus.SC_UNAUTHORIZED)
                publishMessage("Your username and/or password may be wrong", R.color.red);
        } else {
            publishMessage(ex.getMessage(), R.color.red);
        }
    }

    @Override
    protected Boolean call() {
        try (CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom()
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(5000)
                .setSocketTimeout(5000)
                .build()).build()) {

            HttpGet get = new HttpGet(dd.getURLAddress());
            if (dd.auth)
                get.addHeader("Authorization", "Basic " + Base64.encodeToString((dd.username + ":" + dd.password).getBytes(), Base64.NO_WRAP));

            HttpResponse resp = client.execute(get);
            StatusLine sl = resp.getStatusLine();

            get.releaseConnection();

            if (sl.getStatusCode() == HttpStatus.SC_OK) {
                publishMessage("Your DirectDownload configuration is working", R.color.green);
                return true;
            } else {
                publishError(new StatusCodeException(sl));
                return false;
            }
        } catch (IOException | URISyntaxException ex) {
            publishError(ex);
            return false;
        }
    }

    @Override
    public String describe() {
        return "DirectDownload test";
    }
}
