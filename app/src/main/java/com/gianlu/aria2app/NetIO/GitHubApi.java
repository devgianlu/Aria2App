package com.gianlu.aria2app.NetIO;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class GitHubApi {
    public static void getLatestVersion(final IRelease listener) {
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

    public interface IRelease {
        void onRelease(String release);

        void onException(Exception ex);
    }
}
