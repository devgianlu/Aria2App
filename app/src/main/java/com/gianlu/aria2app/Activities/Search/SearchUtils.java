package com.gianlu.aria2app.Activities.Search;


import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.gianlu.aria2app.NetIO.StatusCodeException;
import com.gianlu.commonutils.Logging;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.HttpStatus;
import cz.msebera.android.httpclient.StatusLine;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.util.EntityUtils;

public class SearchUtils {
    private static final String BASE_URL = "http://torrent-search-engine-torrent-search-engine.a3c1.starter-us-west-1.openshiftapps.com/";
    private static SearchUtils instance;
    private final HttpClient client;
    private final ExecutorService executorService;
    private final Handler handler;
    private volatile boolean isCachingEngines;
    private List<SearchEngine> cachedEngines = null;

    private SearchUtils(Context context) {
        client = HttpClients.createDefault();
        executorService = Executors.newSingleThreadExecutor();
        handler = new Handler(context.getMainLooper());
    }

    public static SearchUtils get(Context context) {
        if (instance == null) instance = new SearchUtils(context);
        return instance;
    }

    @NonNull
    private String request(HttpGet get) throws IOException, StatusCodeException {
        HttpResponse resp = client.execute(get);
        StatusLine sl = resp.getStatusLine();
        if (sl.getStatusCode() != HttpStatus.SC_OK) throw new StatusCodeException(sl);
        return EntityUtils.toString(resp.getEntity());
    }

    public void listSearchEngines(final IResult<List<SearchEngine>> listener) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONArray array = new JSONArray(request(new HttpGet(BASE_URL + "listEngines")));
                    final List<SearchEngine> engines = new ArrayList<>();
                    for (int i = 0; i < array.length(); i++)
                        engines.add(new SearchEngine(array.getJSONObject(i)));

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onResult(engines);
                        }
                    });
                } catch (IOException | StatusCodeException | JSONException ex) {
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

    public boolean isCachingEngines() {
        return isCachingEngines;
    }

    public void cacheSearchEngines(final Context context) {
        isCachingEngines = true;
        listSearchEngines(new IResult<List<SearchEngine>>() {
            @Override
            public void onResult(List<SearchEngine> result) {
                isCachingEngines = false;
                cachedEngines = result;
            }

            @Override
            public void onException(Exception ex) {
                isCachingEngines = false;
                Logging.logMe(context, ex);
            }
        });
    }

    public interface IResult<E> {
        void onResult(E result);

        void onException(Exception ex);
    }
}
