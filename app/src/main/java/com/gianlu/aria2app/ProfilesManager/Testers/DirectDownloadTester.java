package com.gianlu.aria2app.ProfilesManager.Testers;


import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Base64;

import com.gianlu.aria2app.NetIO.StatusCodeException;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.R;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

class DirectDownloadTester extends BaseTester {
    private static final int TIMEOUT = 5;
    private final MultiProfile.DirectDownload dd;
    private final OkHttpClient client;

    DirectDownloadTester(Context context, MultiProfile.UserProfile profile, @Nullable IPublish listener) {
        super(context, profile, listener);
        this.dd = profile.directDownload;

        client = new OkHttpClient.Builder()
                .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                .build();
    }

    private void publishError(Exception ex) {
        if (ex instanceof StatusCodeException) {
            publishMessage("Server returned " + ex.getMessage(), R.color.red);
            if (((StatusCodeException) ex).code == 401)
                publishMessage("Your username and/or password may be wrong", R.color.red);
        } else {
            publishMessage(ex.getMessage(), R.color.red);
        }
    }

    @Override
    protected Boolean call() {
        Request.Builder builder = new Request.Builder();
        builder.get().url(dd.getUrl());

        if (dd.auth)
            builder.header("Authorization", "Basic " + Base64.encodeToString((dd.username + ":" + dd.password).getBytes(), Base64.NO_WRAP));

        try (Response resp = client.newCall(builder.build()).execute()) {
            if (resp.code() == 200) {
                publishMessage("Your DirectDownload configuration is working", R.color.green);
                return true;
            } else {
                publishError(new StatusCodeException(resp));
                return false;
            }
        } catch (IOException ex) {
            publishError(ex);
            return false;
        }
    }

    @Override
    public String describe() {
        return "DirectDownload test";
    }
}
