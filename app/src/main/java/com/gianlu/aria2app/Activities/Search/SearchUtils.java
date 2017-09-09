package com.gianlu.aria2app.Activities.Search;


import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;

import com.gianlu.aria2app.NetIO.StatusCodeException;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.HttpStatus;
import cz.msebera.android.httpclient.StatusLine;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.utils.URIBuilder;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.util.EntityUtils;

public class SearchUtils {
    private static final String BASE_URL = "http://torrent-search-engine-torrent-search-engine.a3c1.starter-us-west-1.openshiftapps.com/";
    private static SearchUtils instance;
    private final HttpClient client;
    private final ExecutorService executorService;
    private final Handler handler;
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

    public void search(final String query, final int maxResults, @Nullable final List<SearchEngine> engines, final ISearch listener) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    URIBuilder builder = new URIBuilder(BASE_URL + "search");
                    builder.addParameter("q", query)
                            .addParameter("m", String.valueOf(maxResults));

                    if (engines != null) {
                        for (SearchEngine engine : engines)
                            builder.addParameter("e", engine.id);
                    }

                    JSONObject obj = new JSONObject(request(new HttpGet(builder.build())));
                    JSONArray resultsArray = obj.getJSONArray("result");
                    final List<SearchResult> results = new ArrayList<>();
                    for (int i = 0; i < resultsArray.length(); i++)
                        results.add(new SearchResult(resultsArray.getJSONObject(i)));

                    getCachedEnginesBlocking();
                    JSONArray missingEnginesArray = obj.getJSONArray("missing");
                    final List<SearchEngine> missingEngines = new ArrayList<>();
                    for (int i = 0; i < missingEnginesArray.length(); i++) {
                        SearchEngine engine = findEngine(missingEnginesArray.getString(i));
                        if (engine != null) missingEngines.add(engine);
                    }

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onResult(results, missingEngines);
                        }
                    });
                } catch (IOException | StatusCodeException | URISyntaxException | JSONException ex) {
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

    public void getTorrent(final SearchResult result, final ITorrent listener) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    URIBuilder builder = new URIBuilder(BASE_URL + "getTorrent");
                    builder.addParameter("e", result.engineId)
                            .addParameter("url", Base64.encodeToString(result.url.getBytes(), Base64.NO_WRAP));

                    JSONObject obj = new JSONObject(request(new HttpGet(builder.build())));
                    final Torrent torrent = new Torrent(obj);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(torrent);
                        }
                    });
                } catch (IOException | StatusCodeException | URISyntaxException | JSONException ex) {
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

    @Nullable
    public SearchEngine findEngine(String id) {
        if (cachedEngines == null) return null;
        for (SearchEngine engine : cachedEngines)
            if (Objects.equals(engine.id, id))
                return engine;

        return null;
    }

    public void listSearchEngines(final IResult<List<SearchEngine>> listener) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<SearchEngine> engines = listSearchEnginesSync();

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

    private List<SearchEngine> listSearchEnginesSync() throws JSONException, IOException, StatusCodeException {
        JSONArray array = new JSONArray(request(new HttpGet(BASE_URL + "listEngines")));
        return cachedEngines = CommonUtils.toTList(array, SearchEngine.class);
    }

    public List<SearchEngine> getCachedEnginesBlocking() throws IOException, StatusCodeException, JSONException {
        if (cachedEngines != null) return cachedEngines;
        return listSearchEnginesSync();
    }

    public void cacheSearchEngines(final Context context) {
        listSearchEngines(new IResult<List<SearchEngine>>() {
            @Override
            public void onResult(List<SearchEngine> result) {
            }

            @Override
            public void onException(Exception ex) {
                Logging.logMe(context, ex);
            }
        });
    }

    public interface ITorrent {
        void onDone(Torrent torrent);

        void onException(Exception ex);
    }

    public interface ISearch {
        void onResult(List<SearchResult> results, List<SearchEngine> missingEngines);

        void onException(Exception ex);
    }

    public interface IResult<E> {
        void onResult(E result);

        void onException(Exception ex);
    }
}
