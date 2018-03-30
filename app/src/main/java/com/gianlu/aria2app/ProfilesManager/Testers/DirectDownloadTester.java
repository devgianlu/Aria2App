package com.gianlu.aria2app.ProfilesManager.Testers;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.NetIO.NetUtils;
import com.gianlu.aria2app.NetIO.StatusCodeException;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.R;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Response;

class DirectDownloadTester extends BaseTester<Boolean> {
    private static final int TIMEOUT = 5;
    private final OkHttpClient client;

    DirectDownloadTester(Context context, MultiProfile.UserProfile profile, @Nullable IPublish<Boolean> listener) {
        super(context, profile, listener);

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

    @Nullable
    public Boolean call(@Nullable Object prevResult) {
        try (Response resp = client.newCall(NetUtils.createDirectDownloadRequest(profile.directDownload)).execute()) {
            if (resp.code() == 200) {
                publishMessage("Your DirectDownload configuration is working", R.color.green);
                return true;
            } else {
                publishError(new StatusCodeException(resp));
                return null;
            }
        } catch (IOException | NetUtils.InvalidUrlException ex) {
            publishError(ex);
            return null;
        }
    }

    @NonNull
    @Override
    public String describe() {
        return "DirectDownload test";
    }
}
