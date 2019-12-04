package com.gianlu.aria2app.api;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.aria2app.PK;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.lifecycle.LifecycleAwareHandler;
import com.gianlu.commonutils.lifecycle.LifecycleAwareRunnable;
import com.gianlu.commonutils.preferences.Prefs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class TrackersListFetch {
    private static final String BASE_URL = "https://raw.githubusercontent.com/ngosang/trackerslist/master/trackers_%s.txt";
    private static TrackersListFetch instance;
    private final OkHttpClient client;
    private final LifecycleAwareHandler handler;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private TrackersListFetch() {
        this.client = new OkHttpClient();
        this.handler = new LifecycleAwareHandler(new Handler(Looper.getMainLooper()));
    }

    @NonNull
    public static TrackersListFetch get() {
        if (instance == null) instance = new TrackersListFetch();
        return instance;
    }

    public void getTrackers(@NonNull Type type, @Nullable Activity activity, @NonNull Listener listener) {
        if (Prefs.has(PK.TRACKERS_LIST_CACHE) && !CommonUtils.isDebug()) {
            long age = Prefs.getLong(PK.TRACKERS_LIST_CACHE_AGE, 0);
            if (System.currentTimeMillis() - age < TimeUnit.DAYS.toMillis(1)) {
                Set<String> set = Prefs.getSet(PK.TRACKERS_LIST_CACHE, null);
                if (set != null && set.size() > 0)
                    listener.onDone(new ArrayList<>(set));
            }
        }

        executorService.execute(new LifecycleAwareRunnable(handler, activity == null ? listener : activity) {
            @Override
            public void run() {
                try {
                    Response resp = client.newCall(new Request.Builder().get().url(String.format(BASE_URL, type.getId())).build()).execute();
                    if (resp.code() != 200)
                        throw new IOException(resp.code() + ": " + resp.message());

                    ResponseBody body = resp.body();
                    if (body == null)
                        throw new IOException("Body is empty!");

                    List<String> trackers = new ArrayList<>();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(body.byteStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null && !line.isEmpty())
                            trackers.add(line);
                    }

                    Prefs.putLong(PK.TRACKERS_LIST_CACHE_AGE, System.currentTimeMillis());
                    Prefs.putSet(PK.TRACKERS_LIST_CACHE, new HashSet<>(trackers));

                    post(() -> listener.onDone(trackers));
                } catch (IOException ex) {
                    post(() -> listener.onFailed(ex));
                }
            }
        });
    }

    public enum Type {
        ALL("all"), ALL_HTTP("all_http"), ALL_HTTPS("all_https"), ALL_IP("all_ip"),
        ALL_UDP("all_udp"), ALL_WS("all_ws"), BEST("best"), BEST_IP("best_ip");

        private final String id;

        Type(@NonNull String id) {
            this.id = id;
        }

        @NonNull
        public String getId() {
            return id;
        }
    }

    public interface Listener {
        void onDone(@NonNull List<String> trackers);

        void onFailed(@NonNull Exception ex);
    }
}
