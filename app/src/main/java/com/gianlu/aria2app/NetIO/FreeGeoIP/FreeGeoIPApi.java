package com.gianlu.aria2app.NetIO.FreeGeoIP;

import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import com.gianlu.aria2app.NetIO.StatusCodeException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.HttpStatus;
import cz.msebera.android.httpclient.StatusLine;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.util.EntityUtils;

public class FreeGeoIPApi {
    private static FreeGeoIPApi instance;
    private final ExecutorService executorService;
    private final Handler handler;
    private final HttpClient client;
    private final LruCache<String, IPDetails> cache;

    private FreeGeoIPApi() {
        handler = new Handler(Looper.getMainLooper());
        client = HttpClients.createDefault();
        executorService = Executors.newCachedThreadPool();
        cache = new LruCache<>(50);
    }

    public static FreeGeoIPApi get() {
        if (instance == null) instance = new FreeGeoIPApi();
        return instance;
    }

    public void getIPDetails(final String ip, final IIPDetails listener) {
        if (ip == null) return;
        final IPDetails cachedDetails = cache.get(ip);
        if (cachedDetails != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onDetails(cachedDetails);
                }
            });
        } else {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        HttpGet get = new HttpGet("http://freegeoip.net/json/" + ip);
                        HttpResponse resp = client.execute(get);
                        StatusLine sl = resp.getStatusLine();
                        if (sl.getStatusCode() != HttpStatus.SC_OK) {
                            get.releaseConnection();
                            throw new StatusCodeException(sl);
                        }

                        String json = EntityUtils.toString(resp.getEntity());
                        get.releaseConnection();

                        final IPDetails details = new IPDetails(new JSONObject(json));
                        cache.put(ip, details);

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                listener.onDetails(details);
                            }
                        });
                    } catch (IOException | StatusCodeException | JSONException | NullPointerException ex) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                listener.onException(ex);

                            }
                        });
                    }
                }
            });
        }
    }

    public interface IIPDetails {
        void onDetails(IPDetails details);

        void onException(Exception ex);
    }
}
