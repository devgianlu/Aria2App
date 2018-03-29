package com.gianlu.aria2app.NetIO;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.NetIO.Aria2.AriaException;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketClient extends AbstractClient {
    private static final AtomicInteger sandboxCount = new AtomicInteger(0);
    private static WebSocketClient instance;
    private static boolean locked = false;
    private final Map<Integer, OnJson> requests = new ConcurrentHashMap<>();
    private final WebSocket webSocket;
    private OnConnect connectionListener = null;

    private WebSocketClient(Context context, MultiProfile.UserProfile profile) throws CertificateException, IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException, NetUtils.InvalidUrlException {
        super(context, profile);
        webSocket = client.newWebSocket(NetUtils.createWebsocketRequest(profile), new Listener());
    }

    private WebSocketClient(Context context) throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException, KeyManagementException, ProfilesManager.NoCurrentProfileException, NetUtils.InvalidUrlException {
        this(context, ProfilesManager.get(context).getCurrentSpecific());
    }

    private WebSocketClient(Context context, MultiProfile.UserProfile profile, @Nullable OnConnect listener) throws CertificateException, IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException, NetUtils.InvalidUrlException {
        this(context, profile);
        connectionListener = listener;
    }

    public static WebSocketClient instantiate(Context context) throws InitializationException {
        if (instance == null) {
            try {
                instance = new WebSocketClient(context);
            } catch (NetUtils.InvalidUrlException | ProfilesManager.NoCurrentProfileException | IOException | NoSuchAlgorithmException | KeyStoreException | CertificateException | KeyManagementException ex) {
                throw new InitializationException(ex);
            }
        }

        return instance;
    }

    public static void instantiate(Context context, MultiProfile.UserProfile profile, @NonNull OnConnect listener) {
        try {
            unlock();
            instance = new WebSocketClient(context, profile, listener);
        } catch (CertificateException | NetUtils.InvalidUrlException | NoSuchAlgorithmException | KeyManagementException | KeyStoreException | IOException ex) {
            listener.onFailedConnecting(ex);
        }
    }

    public static void clear() {
        locked = true;
        clearConnectivityListener();
        if (instance != null) {
            instance.clearInternal();
            instance = null;
        }
    }

    private static void unlock() {
        locked = false;
        ErrorHandler.get().unlock();
    }

    @Override
    protected void clearInternal() {
        connectionListener = null;
        if (webSocket != null) webSocket.close(1000, null);
        if (requests != null) requests.clear();
    }

    @Override
    public void send(@NonNull JSONObject request, @NonNull OnJson listener) {
        if (locked) return;
        if (requests.size() > 10) requests.clear();

        try {
            requests.put(request.getInt("id"), listener);
            webSocket.send(request.toString());
        } catch (Exception ex) {
            listener.onException(ex);
        }
    }

    @NonNull
    @Override
    protected JSONObject sendSync(@NonNull JSONObject request) throws Exception {
        final AtomicReference<Object> lock = new AtomicReference<>(null);

        send(request, new OnJson() {
            @Override
            public void onResponse(JSONObject response) {
                synchronized (lock) {
                    lock.set(response);
                    lock.notifyAll();
                }
            }

            @Override
            public void onException(Exception ex) {
                synchronized (lock) {
                    lock.set(ex);
                    lock.notifyAll();
                }
            }
        });

        synchronized (lock) {
            lock.wait();
        }

        Object result = lock.get();
        if (result instanceof Exception) throw (Exception) result;
        else return (JSONObject) result;
    }

    @Override
    protected <R> void batch(BatchSandbox<R> sandbox, DoBatch<R> listener) {
        new SandboxThread<>(sandbox, listener).start();
    }

    @Override
    public void connectivityChanged(@NonNull Context context, @NonNull MultiProfile.UserProfile profile) throws Exception {
        instance = new WebSocketClient(context, profile, null);
    }

    private class Listener extends WebSocketListener {
        @Override
        public void onMessage(WebSocket webSocket, String text) {
            if (locked) return;

            try {
                JSONObject response = new JSONObject(text);

                String method = response.optString("method", null);
                if (method != null && method.startsWith("aria2.on")) return;

                OnJson listener = requests.remove(response.getInt("id"));
                if (listener == null) return;
                if (response.isNull("error")) listener.onResponse(response);
                else listener.onException(new AriaException(response.getJSONObject("error")));
            } catch (JSONException ex) {
                ErrorHandler.get().notifyException(ex, false);
            }
        }

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            if (connectionListener != null) {
                connectionListener.onConnected(WebSocketClient.this);
                connectionListener = null;
            }
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable throwable, Response response) {
            if (connectionListener != null) {
                connectionListener.onFailedConnecting(throwable);
                connectionListener = null;
            }
        }
    }

    private class SandboxThread<R> extends Thread {

        private final BatchSandbox<R> sandbox;
        private final DoBatch<R> listener;

        SandboxThread(BatchSandbox<R> sandbox, DoBatch<R> listener) {
            super("sandbox-thread-" + sandboxCount.getAndIncrement());
            this.sandbox = sandbox;
            this.listener = listener;
        }

        @Override
        public void run() {
            try {
                listener.onSandboxReturned(sandbox.sandbox(WebSocketClient.this));
            } catch (Exception ex) {
                listener.onException(ex);
            }
        }
    }
}
