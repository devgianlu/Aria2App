package com.gianlu.aria2app.NetIO;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import com.gianlu.aria2app.NetIO.Aria2.AriaException;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketClient extends AbstractClient {
    private static WebSocketClient instance;
    private final Map<Long, OnJson> requests = new ConcurrentHashMap<>();
    private final WebSocket webSocket;
    private final long initializedAt;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private OnConnect connectionListener = null;

    private WebSocketClient(Context context, MultiProfile.UserProfile profile) throws GeneralSecurityException, NetUtils.InvalidUrlException, IOException {
        super(context, profile);
        webSocket = client.newWebSocket(NetUtils.createWebsocketRequest(profile), new Listener());
        initializedAt = System.currentTimeMillis();
    }

    private WebSocketClient(Context context) throws GeneralSecurityException, ProfilesManager.NoCurrentProfileException, NetUtils.InvalidUrlException, IOException {
        this(context, ProfilesManager.get(context).getCurrentSpecific());
    }

    private WebSocketClient(Context context, MultiProfile.UserProfile profile, @Nullable OnConnect listener) throws GeneralSecurityException, NetUtils.InvalidUrlException, IOException {
        this(context, profile);
        connectionListener = listener;
    }

    @NonNull
    public static WebSocketClient instantiate(Context context) throws InitializationException {
        if (instance == null) {
            try {
                instance = new WebSocketClient(context);
            } catch (NetUtils.InvalidUrlException | ProfilesManager.NoCurrentProfileException | GeneralSecurityException | IOException ex) {
                throw new InitializationException(ex);
            }
        }

        return instance;
    }

    public static void instantiate(Context context, MultiProfile.UserProfile profile, @NonNull OnConnect listener) {
        try {
            instance = new WebSocketClient(context, profile, listener);
        } catch (NetUtils.InvalidUrlException | GeneralSecurityException | IOException ex) {
            listener.onFailedConnecting(ex);
        }
    }

    public static void clear() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }

    @Override
    protected void closeClient() {
        connectionListener = null;
        if (webSocket != null) webSocket.close(1000, null);
        if (requests != null) requests.clear();
    }

    @Override
    public void send(long id, @NonNull JSONObject request, @NonNull OnJson listener) {
        if (shouldIgnoreCommunication) return;
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

    @Override
    public void connectivityChanged(@NonNull Context context, @NonNull MultiProfile.UserProfile profile) throws Exception {
        instance = new WebSocketClient(context, profile, null);
    }

    @WorkerThread
    private class Listener extends WebSocketListener {
        @Override
        public void onMessage(WebSocket webSocket, String text) {
            if (shouldIgnoreCommunication) return;

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
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (connectionListener != null) {
                        if (connectionListener.onConnected(WebSocketClient.this)) {
                            connectionListener.onPingTested(WebSocketClient.this, System.currentTimeMillis() - initializedAt);
                        }

                        connectionListener = null;
                    }
                }
            });
        }

        @Override
        public void onFailure(WebSocket webSocket, final Throwable throwable, Response response) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (connectionListener != null) {
                        connectionListener.onFailedConnecting(throwable);
                        connectionListener = null;
                    }
                }
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
