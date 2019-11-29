package com.gianlu.aria2app.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import com.gianlu.aria2app.api.aria2.AriaException;
import com.gianlu.aria2app.profiles.MultiProfile;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketClient extends AbstractClient {
    private final Map<Long, InternalResponse> requests = new ConcurrentHashMap<>();
    private final WeakReference<WebSocket> webSocket;
    private final long initializedAt;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final boolean closeAfterTest;
    private OnConnect connectionListener = null;

    @UiThread
    private WebSocketClient(@NonNull MultiProfile.UserProfile profile, boolean close) throws GeneralSecurityException, NetUtils.InvalidUrlException, IOException {
        super(profile);
        webSocket = new WeakReference<>(client.newWebSocket(NetUtils.createWebsocketRequest(profile), new Listener()));
        initializedAt = System.currentTimeMillis();
        closeAfterTest = close;
    }

    @UiThread
    private WebSocketClient(@NonNull MultiProfile.UserProfile profile, @Nullable OnConnect listener, boolean close) throws GeneralSecurityException, NetUtils.InvalidUrlException, IOException {
        this(profile, close);
        connectionListener = listener;
    }

    @NonNull
    static WebSocketClient instantiate(@NonNull MultiProfile.UserProfile profile) throws InitializationException {
        try {
            return new WebSocketClient(profile, false);
        } catch (NetUtils.InvalidUrlException | GeneralSecurityException | IOException ex) {
            throw new InitializationException(ex);
        }
    }

    @UiThread
    public static Closeable checkConnection(@NonNull MultiProfile.UserProfile profile, @NonNull OnConnect listener, boolean close) {
        try {
            return new WebSocketClient(profile, listener, close);
        } catch (NetUtils.InvalidUrlException | GeneralSecurityException | IOException ex) {
            listener.onFailedConnecting(profile, ex);
            return null;
        }
    }

    @Override
    protected void closeClient() {
        connectionListener = null;
        if (webSocket != null && webSocket.get() != null) {
            webSocket.get().close(1000, null);
            webSocket.clear();
        }

        for (InternalResponse internal : requests.values())
            internal.exception(new IOException("Client has been closed."));

        requests.clear();
    }

    @Override
    public void send(long id, @NonNull JSONObject request, @NonNull OnJson listener) {
        if (closed) {
            listener.onException(new IllegalStateException("Client is closed: " + this));
            return;
        }

        executorService.execute(new RequestProcessor(id, request, listener));
    }

    @NonNull
    @Override
    @WorkerThread
    public JSONObject sendSync(long id, @NonNull JSONObject request) throws Exception {
        if (closed || webSocket.get() == null)
            throw new IllegalStateException("Client is closed: " + this);

        InternalResponse internal = new InternalResponse();

        requests.put(id, internal);
        webSocket.get().send(request.toString());

        synchronized (internal) {
            internal.wait(5000);
        }

        requests.remove(id);

        if (internal.obj != null) return internal.obj;
        else throw internal.ex;
    }

    @Override
    protected <R> void batch(BatchSandbox<R> sandbox, DoBatch<R> listener) {
        if (closed) {
            listener.onException(new IllegalStateException("Client is closed: " + this));
            return;
        }

        executorService.execute(new SandboxRunnable<>(sandbox, listener));
    }

    private static class InternalResponse {
        private JSONObject obj;
        private Exception ex;

        InternalResponse() {
            this.obj = null;
            this.ex = null;
        }

        synchronized void json(@NonNull JSONObject obj) {
            this.obj = obj;
            this.ex = null;
            notifyAll();
        }

        synchronized void exception(@NonNull Exception ex) {
            this.ex = ex;
            this.obj = null;
            notifyAll();
        }
    }

    @WorkerThread
    private class Listener extends WebSocketListener {
        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            if (closed) return;

            try {
                JSONObject response = new JSONObject(text);

                String method = response.optString("method", null);
                if (method != null && method.startsWith("aria2.on")) return;

                InternalResponse internal = requests.get(Long.parseLong(response.getString("id")));
                if (internal == null) return;

                try {
                    validateResponse(response);
                    internal.json(response);
                } catch (AriaException ex) {
                    internal.exception(ex);
                }
            } catch (JSONException ex) {
                ErrorHandler.get().notifyException(ex, false);
            }
        }

        @Override
        public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
            long ping = System.currentTimeMillis() - initializedAt;
            handler.post(() -> {
                if (connectionListener != null) {
                    if (connectionListener.onConnected(WebSocketClient.this)) {
                        connectionListener.onPingTested(WebSocketClient.this, ping);
                    }

                    connectionListener = null;
                    if (closeAfterTest) close();
                }
            });
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable throwable, Response response) {
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
