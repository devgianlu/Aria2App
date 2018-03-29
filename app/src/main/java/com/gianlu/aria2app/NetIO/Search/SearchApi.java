package com.gianlu.aria2app.NetIO.Search;


import android.os.Handler;
import android.os.Looper;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class SearchApi {
    public static final int RESULTS_PER_REQUEST = 20;
    private static final int TIMEOUT = 15;
    private static final String BASE_URL = "https://torrent-search-engine.herokuapp.com/";
    private static SearchApi instance;
    private final OkHttpClient client;
    private final ExecutorService executorService;
    private final Handler handler;
    private List<SearchEngine> cachedEngines = null;

    private SearchApi() {
        client = new OkHttpClient.Builder().connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                .build();
        executorService = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());
    }

    public static SearchApi get() {
        if (instance == null) instance = new SearchApi();
        return instance;
    }

    @NonNull
    private String request(Request get) throws IOException, StatusCodeException {
        try (Response resp = client.newCall(get).execute()) {
            if (resp.code() != 200) throw new StatusCodeException(resp);

            ResponseBody body = resp.body();
            if (body == null) throw new IOException("Empty body!");

            return body.string();
        }
    }

    public void search(final String token, final int maxResults, final ISearch listener) {
        search(null, token, maxResults, null, listener);
    }

    private void search(@Nullable String query, @Nullable String token, int maxResults, @Nullable Collection<String> engines, final ISearch listener) {
        HttpUrl baseUrl = HttpUrl.parse(BASE_URL + "search");
        if (baseUrl == null) {
            listener.onException(new NullPointerException("Invalid URL."));
            return;
        }

        final HttpUrl.Builder builder = baseUrl.newBuilder();
        builder.addQueryParameter("m", String.valueOf(maxResults));
        if (token != null) {
            builder.addQueryParameter("t", token);
        } else {
            builder.addQueryParameter("q", query);
            if (engines != null)
                for (String engineId : engines)
                    builder.addQueryParameter("e", engineId);
        }

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject obj = new JSONObject(request(new Request.Builder().get().url(builder.build()).build()));
                    final List<SearchResult> results = CommonUtils.toTList(obj.getJSONArray("result"), SearchResult.class);

                    cacheEnginesBlocking();
                    JSONArray missingEnginesArray = obj.getJSONArray("missing");
                    final List<MissingSearchEngine> missingEngines = new ArrayList<>();
                    for (int i = 0; i < missingEnginesArray.length(); i++)
                        missingEngines.add(new MissingSearchEngine(SearchApi.this, missingEnginesArray.getJSONObject(i)));

                    final String token = obj.optString("token", null);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onResult(results, missingEngines, token);
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

    public void search(final String query, final int maxResults, @Nullable final Collection<String> engines, final ISearch listener) {
        search(query, null, maxResults, engines, listener);
    }

    public void getTorrent(SearchResult result, final ITorrent listener) {
        HttpUrl baseUrl = HttpUrl.parse(BASE_URL + "getTorrent");
        if (baseUrl == null) {
            listener.onException(new NullPointerException("Invalid URL."));
            return;
        }

        final HttpUrl.Builder builder = baseUrl.newBuilder();
        builder.addQueryParameter("e", result.engineId)
                .addQueryParameter("url", Base64.encodeToString(result.url.getBytes(), Base64.NO_WRAP | Base64.URL_SAFE));

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject obj = new JSONObject(request(new Request.Builder().get().url(builder.build()).build()));
                    final Torrent torrent = new Torrent(obj);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDone(torrent);
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
        JSONArray array = new JSONArray(request(new Request.Builder().get().url(BASE_URL + "listEngines").build()));
        return cachedEngines = CommonUtils.toTList(array, SearchEngine.class);
    }

    private void cacheEnginesBlocking() throws IOException, StatusCodeException, JSONException {
        if (cachedEngines == null) listSearchEnginesSync();
    }

    public void cacheSearchEngines() {
        listSearchEngines(new IResult<List<SearchEngine>>() {
            @Override
            public void onResult(List<SearchEngine> result) {
            }

            @Override
            public void onException(Exception ex) {
                Logging.log(ex);
            }
        });
    }

    public interface ITorrent {
        void onDone(Torrent torrent);

        void onException(Exception ex);
    }

    public interface ISearch {
        void onResult(List<SearchResult> results, List<MissingSearchEngine> missingEngines, @Nullable String nextPageToken);

        void onException(Exception ex);
    }

    public interface IResult<E> {
        void onResult(E result);

        void onException(Exception ex);
    }
}
