package com.gianlu.aria2app.profiles.testers;


import android.content.Context;

import com.gianlu.aria2app.api.NetUtils;
import com.gianlu.aria2app.api.StatusCodeException;
import com.gianlu.aria2app.profiles.MultiProfile;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.OkHttpClient;
import okhttp3.Response;

class DirectDownloadTester extends BaseTester<Boolean> {
    private static final int TIMEOUT = 5;
    private final OkHttpClient client;

    DirectDownloadTester(Context context, MultiProfile.UserProfile profile, @Nullable PublishListener<Boolean> listener) {
        super(context, profile, listener);

        client = new OkHttpClient.Builder()
                .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                .build();
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
    public Boolean call(@Nullable Object prevResult) {
        try (Response resp = client.newCall(NetUtils.createDirectDownloadRequest(profile.directDownload)).execute()) {
            if (resp.code() == 200) {
                publishMessage("Your DirectDownload configuration is working", Level.SUCCESS);
                return true;
            } else {
                publishError(new StatusCodeException(resp));
                return null;
            }
        } catch (IOException | NetUtils.InvalidUrlException ex) {
            publishError(ex);
            return null;
        } finally {
            client.connectionPool().evictAll();
            client.dispatcher().executorService().shutdown();
        }
    }

    @NonNull
    @Override
    public String describe() {
        return "DirectDownload test";
    }
}
