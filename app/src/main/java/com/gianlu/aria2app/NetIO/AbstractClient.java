package com.gianlu.aria2app.NetIO;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.gianlu.aria2app.ProfilesManager.MultiProfile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import javax.net.ssl.SSLContext;

public abstract class AbstractClient {
    private static final List<WeakReference<OnConnectivityChanged>> listeners = new ArrayList<>();
    private final Handler handler;
    private final WifiManager wifiManager;
    private final WeakReference<Context> context;
    private final ConnectivityChangedReceiver connectivityChangedReceiver;
    protected MultiProfile.UserProfile profile;
    protected SSLContext sslContext;

    AbstractClient(Context context, MultiProfile.UserProfile profile) throws CertificateException, IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.sslContext = NetUtils.createSSLContext(profile.certificate);
        this.profile = profile;
        this.context = new WeakReference<>(context);
        this.handler = new Handler(Looper.getMainLooper());
        this.connectivityChangedReceiver = new ConnectivityChangedReceiver();

        context.getApplicationContext().registerReceiver(connectivityChangedReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    public static void addConnectivityListener(OnConnectivityChanged listener) {
        for (WeakReference<OnConnectivityChanged> ref : listeners)
            if (ref.get() == listener)
                return;

        listeners.add(new WeakReference<>(listener));
    }

    public static void removeConnectivityListener(OnConnectivityChanged listener) {
        Iterator<WeakReference<OnConnectivityChanged>> iterator = listeners.listIterator();
        while (iterator.hasNext()) {
            WeakReference<OnConnectivityChanged> ref = iterator.next();
            if (ref.get() == null || ref.get() == listener) iterator.remove();
        }
    }

    static void clearConnectivityListener() {
        listeners.clear();
    }

    @Override
    protected void finalize() throws Throwable {
        if (context.get() != null)
            context.get().getApplicationContext().unregisterReceiver(connectivityChangedReceiver);

        super.finalize();
    }

    protected abstract void clearInternal();

    public final <R> void send(final AriaRequestWithResult<R> request, final OnResult<R> listener) {
        try {
            send(request.build(this), new IReceived() {
                @Override
                public void onResponse(JSONObject response) throws JSONException {
                    final R result = request.processor.process(AbstractClient.this, response);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onResult(result);
                        }
                    });
                }

                @Override
                public void onException(final Exception ex) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onException(ex);
                        }
                    });
                }
            });
        } catch (final JSONException ex) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onException(ex);
                }
            });
        }
    }

    public final void sendSync(AriaRequest request) throws Exception {
        sendSync(request.build(this));
    }

    @NonNull
    public final <R> R sendSync(AriaRequestWithResult<R> request) throws Exception {
        return request.processor.process(AbstractClient.this, sendSync(request.build(this)));
    }

    public final void send(AriaRequest request, final OnSuccess listener) {
        try {
            send(request.build(this), new IReceived() {
                @Override
                public void onResponse(JSONObject response) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onSuccess();
                        }
                    });
                }

                @Override
                public void onException(final Exception ex) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onException(ex);
                        }
                    });
                }
            });
        } catch (final JSONException ex) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onException(ex);
                }
            });
        }
    }

    protected abstract void send(JSONObject request, IReceived listener);

    @NonNull
    protected abstract JSONObject sendSync(JSONObject request) throws Exception;

    public final <R> void batch(BatchSandbox<R> sandbox, final OnResult<R> listener) {
        batch(sandbox, new IBatch<R>() {
            @Override
            public void onSandboxReturned(final R result) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onResult(result);
                    }
                });
            }

            @Override
            public void onException(final Exception ex) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onException(ex);
                    }
                });
            }
        });
    }

    protected abstract <R> void batch(BatchSandbox<R> sandbox, IBatch<R> listener);

    protected abstract void connectivityChanged(@NonNull Context context, @NonNull MultiProfile.UserProfile profile) throws Exception;

    @NonNull
    private JSONArray baseRequestParams() {
        JSONArray array = new JSONArray();
        if (profile.authMethod == AuthMethod.TOKEN) array.put("token:" + profile.serverToken);
        return array;
    }

    public enum AuthMethod {
        NONE,
        HTTP,
        TOKEN
    }

    public enum Method {
        TELL_STATUS("aria2.tellStatus"),
        TELL_ACTIVE("aria2.tellActive"),
        TELL_WAITING("aria2.tellWaiting"),
        TELL_STOPPED("aria2.tellStopped"),
        UNPAUSE("aria2.unpause"),
        REMOVE("aria2.remove"),
        FORCE_PAUSE("aria2.forcePause"),
        FORCE_REMOVE("aria2.forceRemove"),
        REMOVE_RESULT("aria2.removeDownloadResult"),
        GET_VERSION("aria2.getVersion"),
        PAUSE_ALL("aria2.pauseAll"),
        GET_SESSION_INFO("aria2.getSessionInfo"),
        SAVE_SESSION("aria2.saveSession"),
        UNPAUSE_ALL("aria2.unpauseAll"),
        FORCE_PAUSE_ALL("aria2.forcePauseAll"),
        PURGE_DOWNLOAD_RESULTS("aria2.purgeDownloadResult"),
        PAUSE("aria2.pause"),
        LIST_METHODS("system.listMethods"),
        GET_GLOBAL_STATS("aria2.getGlobalStat"),
        GET_GLOBAL_OPTIONS("aria2.getGlobalOption"),
        CHANGE_GLOBAL_OPTIONS("aria2.changeGlobalOption"),
        ADD_URI("aria2.addUri"),
        ADD_TORRENT("aria2.addTorrent"),
        ADD_METALINK("aria2.addMetalink"),
        GET_SERVERS("aria2.getServers"),
        GET_PEERS("aria2.getPeers"),
        GET_OPTIONS("aria2.getOption"),
        GET_FILES("aria2.getFiles"),
        CHANGE_POSITION("aria2.changePosition"),
        CHANGE_OPTIONS("aria2.changeOptions");

        private final String method;

        Method(String method) {
            this.method = method;
        }
    }

    public interface BatchSandbox<R> {
        R sandbox(AbstractClient client) throws Exception;
    }

    public interface OnResult<R> {
        void onResult(R result);

        void onException(Exception ex);
    }

    public interface OnSuccess {
        void onSuccess();

        void onException(Exception ex);
    }

    public interface OnConnectivityChanged {
        void connectivityChanged(@NonNull MultiProfile.UserProfile profile);
    }

    public abstract static class Processor<R> {

        @NonNull
        public abstract R process(AbstractClient client, JSONObject obj) throws JSONException;
    }

    public static class AriaRequest {
        private final Method method;
        private final Object[] params;

        AriaRequest(Method method, Object... params) {
            this.method = method;
            this.params = params;
        }

        @NonNull
        JSONObject build(AbstractClient client) throws JSONException {
            JSONObject request = new JSONObject();
            request.put("jsonrpc", "2.0");
            request.put("id", ThreadLocalRandom.current().nextInt());
            request.put("method", method.method);
            JSONArray params = client.baseRequestParams();
            for (Object obj : this.params) params.put(obj);
            request.put("params", params);
            return request;
        }
    }

    public static class AriaRequestWithResult<R> extends AriaRequest {
        private final Processor<R> processor;

        AriaRequestWithResult(Method method, Processor<R> processor, Object... params) {
            super(method, params);
            this.processor = processor;
        }
    }

    public static class InitializationException extends Exception {
        InitializationException(Throwable cause) {
            super(cause);
        }
    }

    private class ConnectivityChangedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (Objects.equals(intent.getAction(), ConnectivityManager.CONNECTIVITY_ACTION)) {
                boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                if (!noConnectivity) {
                    int networkType = intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE, ConnectivityManager.TYPE_DUMMY);
                    final MultiProfile.UserProfile profile = AbstractClient.this.profile.getParent().getProfile(networkType, wifiManager);
                    if (!Objects.equals(AbstractClient.this.profile.connectivityCondition, profile.connectivityCondition)) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    connectivityChanged(context, profile);
                                    AbstractClient.this.profile = profile;

                                    Iterator<WeakReference<OnConnectivityChanged>> iterator = listeners.listIterator();
                                    while (iterator.hasNext()) {
                                        WeakReference<OnConnectivityChanged> ref = iterator.next();
                                        if (ref.get() == null) iterator.remove();
                                        else ref.get().connectivityChanged(profile);
                                    }
                                } catch (Exception ex) {
                                    ErrorHandler.get().notifyException(ex, true);
                                }
                            }
                        }).start();
                    }
                }
            }
        }
    }
}
