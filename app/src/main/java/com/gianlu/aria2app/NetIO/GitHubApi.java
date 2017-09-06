package com.gianlu.aria2app.NetIO;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GitHubApi { // TODO: Improve
    public static void getLatestVersion(final IRelease handler) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL("https://api.github.com/repos/aria2/aria2/releases/latest").openConnection();
                    conn.connect();

                    if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        throw new StatusCodeException(conn.getResponseCode(), conn.getResponseMessage());
                    }

                    StringBuilder builder = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    String line;
                    while ((line = reader.readLine()) != null) builder.append(line);

                    reader.close();
                    conn.disconnect();

                    handler.onRelease(new JSONObject(builder.toString()).getString("name").replace("aria2 ", ""));
                } catch (IOException | StatusCodeException | JSONException | NullPointerException ex) {
                    handler.onException(ex);
                }
            }
        }).start();
    }

    public interface IRelease {
        void onRelease(String release);

        void onException(Exception ex);
    }
}
