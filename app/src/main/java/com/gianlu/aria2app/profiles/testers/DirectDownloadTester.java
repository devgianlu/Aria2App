package com.gianlu.aria2app.profiles.testers;


import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.gianlu.aria2app.api.StatusCodeException;
import com.gianlu.aria2app.profiles.MultiProfile;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

class DirectDownloadTester extends BaseTester<Boolean> {
    private static final int TIMEOUT = 5;

    DirectDownloadTester(Context context, @NonNull MultiProfile.UserProfile profile, @Nullable PublishListener<Boolean> listener) {
        super(context, profile, listener);
    }

    private void publishError(Exception ex) {
        if (ex instanceof StatusCodeException) {
            publishMessage("Server returned " + ex.getMessage(), Level.ERROR);
            if (((StatusCodeException) ex).code == 401)
                publishMessage("Your username and/or password may be wrong", Level.ERROR);
        } else {
            publishMessage(ex.getMessage(), Level.ERROR);
        }
    }

    @Nullable
    @Override
    @WorkerThread
    public Boolean call(@Nullable Object prevResult) {
        switch (profile.directDownload.type) {
            case WEB:
                return callWeb();
            case FTP:
                return callFtp();
            case SMB:
                return callSmb();
            default:
                throw new IllegalArgumentException("Unknown type: " + profile.directDownload.type);
        }
    }

    @Nullable
    @WorkerThread
    private Boolean callWeb() {
        OkHttpClient client = new OkHttpClient.Builder()
                .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                .build();

        try {
            MultiProfile.DirectDownload.Web dd = profile.directDownload.web;
            HttpUrl baseUrl = dd.getUrl();
            if (baseUrl == null) {
                publishMessage("Invalid DirectDownload url", Level.ERROR);
                return null;
            }

            Request.Builder builder = new Request.Builder().get().url(baseUrl);
            if (dd.auth) builder.header("Authorization", dd.getAuthorizationHeader());
            try (Response resp = client.newCall(builder.build()).execute()) {
                if (resp.code() == 200) {
                    publishMessage("Your DirectDownload configuration is working", Level.SUCCESS);
                    return true;
                } else {
                    publishError(new StatusCodeException(resp));
                    return null;
                }
            }
        } catch (IOException ex) {
            publishError(ex);
            return null;
        } finally {
            client.connectionPool().evictAll();
            client.dispatcher().executorService().shutdown();
        }
    }

    @Nullable
    @WorkerThread
    private Boolean callFtp() {
        return null; // TODO
    }

    @Nullable
    @WorkerThread
    private Boolean callSmb() {
        return null; // TODO
    }

    @NonNull
    @Override
    public String describe() {
        return "DirectDownload test";
    }
}
