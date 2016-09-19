package com.gianlu.aria2app.Options;

import android.app.Activity;
import android.content.Context;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class DownloadSource implements Runnable {
    private Activity context;
    private IDownload handler;

    public DownloadSource(Activity context, IDownload handler) {
        this.context = context;
        this.handler = handler;
    }

    @Override
    public void run() {
        handler.onStart();

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("http://gianlu.16mb.com/repos/optionsDownload.php?lang=" + Locale.getDefault().getLanguage()).openConnection();
            conn.connect();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                handler.onConnectionFailed(conn.getResponseCode(), conn.getResponseMessage());
                return;
            }

            ObjectOutputStream out = new ObjectOutputStream(context.openFileOutput("options.ser", Context.MODE_PRIVATE));
            out.writeObject(Option.fromJSONtoMap(new JSONObject(new BufferedReader(new InputStreamReader(conn.getInputStream())).readLine())));
            out.close();

            handler.onDone();
        } catch (Exception ex) {
            handler.onException(ex);
        }
    }

    public interface IDownload {
        void onStart();

        void onDone();

        void onConnectionFailed(int code, String message);

        void onException(Exception ex);
    }
}

