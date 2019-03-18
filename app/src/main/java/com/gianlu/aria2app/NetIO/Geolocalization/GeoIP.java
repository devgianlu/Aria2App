package com.gianlu.aria2app.NetIO.Geolocalization;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import com.gianlu.commonutils.Lifecycle.LifecycleAwareHandler;
import com.gianlu.commonutils.Lifecycle.LifecycleAwareRunnable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class GeoIP {
    private static final Pattern IPV4_PATTERN = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
    private static final Pattern IPV6_PATTERN = Pattern.compile("^(?:[0-9a-f]|:){1,4}(?::(?:[0-9a-f]{0,4})*){1,7}$");
    private static GeoIP instance;
    private final ExecutorService executorService;
    private final LifecycleAwareHandler handler;
    private final OkHttpClient client;
    private final LruCache<String, IPDetails> cache;

    private GeoIP() {
        handler = new LifecycleAwareHandler(new Handler(Looper.getMainLooper()));
        client = new OkHttpClient();
        executorService = Executors.newCachedThreadPool();
        cache = new LruCache<>(50);
    }

    @NonNull
    public static GeoIP get() {
        if (instance == null) instance = new GeoIP();
        return instance;
    }

    @NonNull
    private static String buildIpString(byte[] addr) {
        return (addr[0] & 0xFF) + "." + (addr[1] & 0xFF) + "." + (addr[2] & 0xFF) + "." + (addr[3] & 0xFF);
    }

    @Nullable
    public IPDetails getCached(@NonNull String ip) {
        return cache.get(ip);
    }

    private boolean hitCache(String ip, @Nullable Activity activity, @NonNull OnIpDetails listener) {
        if (ip == null) return false;

        IPDetails cachedDetails = cache.get(ip);
        if (cachedDetails != null) {
            handler.post(activity == null ? listener : activity, () -> listener.onDetails(cachedDetails));
            return true;
        }

        return false;
    }

    public void getIPDetails(@NonNull String ip, @Nullable Activity activity, @NonNull OnIpDetails listener) {
        if (hitCache(ip, activity, listener)) return;

        executorService.execute(new LifecycleAwareRunnable(handler, activity == null ? listener : activity) {
            @Override
            public void run() {
                try {
                    String realIP;
                    if (IPV4_PATTERN.matcher(ip).matches() || IPV6_PATTERN.matcher(ip).matches()) {
                        realIP = ip;
                    } else {
                        List<InetAddress> ips = client.dns().lookup(ip);
                        if (ips.isEmpty()) throw new UnknownHostException(ip);
                        realIP = buildIpString(ips.get(0).getAddress());
                    }

                    if (realIP.startsWith("[") && realIP.endsWith("]"))
                        realIP = realIP.substring(1, realIP.length() - 1);

                    try (Response resp = client.newCall(new Request.Builder()
                            .get().url("https://geoip.gianlu.xyz/Lookup/" + realIP).build()).execute()) {
                        ResponseBody body = resp.body();
                        if (body == null) throw new IOException("Empty body!");

                        if (resp.code() == 200) {
                            JSONObject obj = new JSONObject(body.string());
                            IPDetails details = new IPDetails(obj);
                            cache.put(ip, details);

                            post(() -> listener.onDetails(details));
                        } else {
                            throw new ServiceException(realIP, resp.code(), body.string());
                        }
                    }
                } catch (IOException | JSONException | ServiceException | RuntimeException ex) {
                    post(() -> listener.onException(ex));
                }
            }
        });
    }

    public interface OnIpDetails {
        @UiThread
        void onDetails(@NonNull IPDetails details);

        @UiThread
        void onException(@NonNull Exception ex);
    }

    private static class ServiceException extends Exception {
        private ServiceException(String ip, int code, String msg) {
            super(ip + ": (" + code + ") " + msg);
        }
    }
}
