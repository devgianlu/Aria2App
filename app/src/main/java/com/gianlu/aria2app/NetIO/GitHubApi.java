package com.gianlu.aria2app.NetIO;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.HttpStatus;
import cz.msebera.android.httpclient.StatusLine;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.util.EntityUtils;

public class GitHubApi {
    public static void getLatestVersion(final IRelease listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try (CloseableHttpClient client = HttpClients.createDefault()) {
                    HttpGet get = new HttpGet("https://api.github.com/repos/aria2/aria2/releases/latest");
                    HttpResponse resp = client.execute(get);
                    StatusLine sl = resp.getStatusLine();
                    if (sl.getStatusCode() != HttpStatus.SC_OK) throw new StatusCodeException(sl);

                    String json = EntityUtils.toString(resp.getEntity());
                    get.releaseConnection();

                    listener.onRelease(new JSONObject(json).getString("name").replace("aria2 ", ""));
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
