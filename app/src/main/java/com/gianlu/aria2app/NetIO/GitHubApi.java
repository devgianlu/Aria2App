package com.gianlu.aria2app.NetIO;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class GitHubApi {
    public static void getLatestVersion(final OnRelease listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                OkHttpClient client = new OkHttpClient();
                try (Response resp = client.newCall(new Request.Builder()
                        .get().url("https://api.github.com/repos/aria2/aria2/releases/latest").build()).execute()) {

                    if (resp.code() != 200) throw new StatusCodeException(resp);

                    ResponseBody body = resp.body();
                    if (body == null) throw new IOException("Empty body!");

                    listener.onRelease(new JSONObject(body.string()).getString("name").replace("aria2 ", ""));
                } catch (IOException | StatusCodeException | JSONException | NullPointerException ex) {
                    listener.onException(ex);
                }
            }
        }).start();
    }

    @WorkerThread
    public interface OnRelease {
        void onRelease(@NonNull String release);

        void onException(@NonNull Exception ex);
    }
}
