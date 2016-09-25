package com.gianlu.aria2app.NetIO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AsyncRequest implements Runnable {
    private final String url;
    private final IResponse handler;

    public AsyncRequest(String url, IResponse handler) {
        this.url = url;
        this.handler = handler;
    }

    @Override
    public void run() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");

            conn.connect();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                handler.onFailed(conn.getResponseCode(), conn.getResponseMessage());
                return;
            }

            StringBuilder result = new StringBuilder();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null)
                result.append(line);
            rd.close();

            handler.onResponse(result.toString());
        } catch (IOException ex) {
            handler.onException(ex);
        }
    }
}
