package com.gianlu.aria2app.NetIO;

import com.gianlu.aria2app.NetIO.Aria2.AriaException;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Closeable;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketClient extends AbstractClient {
    private final Map<Long, OnJson> requests = new ConcurrentHashMap<>();
    private final WebSocket webSocket;
    private final long initializedAt;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private OnConnect connectionListener = null;

    @UiThread
    private WebSocketClient(@NonNull MultiProfile.UserProfile profile) throws GeneralSecurityException, NetUtils.InvalidUrlException, IOException {
        super(profile);
        webSocket = client.newWebSocket(NetUtils.createWebsocketRequest(profile), new Listener());
        initializedAt = System.currentTimeMillis();
    }

    @UiThread
    private WebSocketClient(@NonNull MultiProfile.UserProfile profile, @Nullable OnConnect listener) throws GeneralSecurityException, NetUtils.InvalidUrlException, IOException {
        this(profile);
        connectionListener = listener;
    }

    @NonNull
    static WebSocketClient instantiate(@NonNull MultiProfile.UserProfile profile) throws InitializationException {
        try {
            return new WebSocketClient(profile);
        } catch (NetUtils.InvalidUrlException | GeneralSecurityException | IOException ex) {
            throw new InitializationException(ex);
        }
    }

    @UiThread
    public static Closeable checkConnection(@NonNull MultiProfile.UserProfile profile, @NonNull OnConnect listener) {
        try {
            return new WebSocketClient(profile, listener);
        } catch (NetUtils.InvalidUrlException | GeneralSecurityException | IOException ex) {
            listener.onFailedConnecting(profile, ex);
            return null;
        }
    }

    @Override
    protected void closeClient() {
        connectionListener = null;
        if (webSocket != null) webSocket.close(1000, null);

        for (OnJson listener : requests.values())
            listener.onException(new IOException("Client has been closed."));

        requests.clear();
    }

    @Override
    public void send(long id, @NonNull JSONObject request, @NonNull OnJson listener) {
        if (closed) {
            listener.onException(new IllegalStateException("Client is closed: " + this));
            return;
        }

        if (requests.size() > 10) requests.clear();

        try {
            requests.put(id, listener);
            webSocket.send(request.toString());
        } catch (Exception ex) {
            listener.onException(ex);
        }
    }

    @NonNull
    @Override
    protected JSONObject sendSync(long id, @NonNull JSONObject request) throws Exception {
        final AtomicReference<Object> lock = new AtomicReference<>(null);

        send(id, request, new OnJson() {
            @Override
            public void onResponse(@NonNull JSONObject response) {
                synchronized (lock) {
                    lock.set(response);
                    lock.notifyAll();
                }
            }

            @Override
            public void onException(@NonNull Exception ex) {
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
        executorService.execute(new SandboxRunnable<>(sandbox, listener));
    }

    @WorkerThread
    private class Listener extends WebSocketListener {
        @Override
        public void onMessage(WebSocket webSocket, String text) {
            if (closed) return;

            try {
                JSONObject response = new JSONObject(text);

                String method = response.optString("method", null);
                if (method != null && method.startsWith("aria2.on")) return;

                OnJson listener = requests.remove(Long.parseLong(response.getString("id")));
                if (listener == null) return;

                try {
                    validateResponse(response);
                    listener.onResponse(response);
                } catch (AriaException ex) {
                    listener.onException(ex);
                }
            } catch (JSONException ex) {
                ErrorHandler.get().notifyException(ex, false);
            }
        }

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            long ping = System.currentTimeMillis() - initializedAt;
            handler.post(() -> {
                if (connectionListener != null) {
                    if (connectionListener.onConnected(WebSocketClient.this)) {
                        connectionListener.onPingTested(WebSocketClient.this, ping);
                    }

                    connectionListener = null;
                    close();
                }
            });
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable throwable, Response response) {
            handler.post(() -> {
                if (connectionListener != null) {
                    connectionListener.onFailedConnecting(profile, throwable);
                    connectionListener = null;
                }

                close();
            });
        }
    }

    private class SandboxRunnable<R> implements Runnable {
        private final BatchSandbox<R> sandbox;
        private final DoBatch<R> listener;

        SandboxRunnable(BatchSandbox<R> sandbox, DoBatch<R> listener) {
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
