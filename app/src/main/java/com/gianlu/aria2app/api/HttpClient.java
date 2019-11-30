package com.gianlu.aria2app.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import com.gianlu.aria2app.api.aria2.AriaException;
import com.gianlu.aria2app.profiles.MultiProfile;
import com.gianlu.commonutils.logging.Logging;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import okhttp3.Response;
import okhttp3.ResponseBody;


public class HttpClient extends AbstractClient {
    private final ExecutorService executorService;
    private final URI url;

    @UiThread
    private HttpClient(@NonNull MultiProfile.UserProfile profile) throws GeneralSecurityException, IOException, NetUtils.InvalidUrlException {
        super(profile);
        ErrorHandler.get().unlock();
        this.executorService = Executors.newCachedThreadPool();
        this.url = NetUtils.createHttpURL(profile);
    }

    @UiThread
    private HttpClient(@NonNull MultiProfile.UserProfile profile, @Nullable OnConnect connectionListener, boolean close) throws GeneralSecurityException, IOException, NetUtils.InvalidUrlException {
        this(profile);

        executorService.submit(() -> {
            try (Socket socket = new Socket()) {
                final long initializedAt = System.currentTimeMillis();
                socket.connect(new InetSocketAddress(profile.serverAddr, profile.serverPort), (int) TimeUnit.SECONDS.toMillis(NetUtils.HTTP_TIMEOUT));
                if (connectionListener != null) {
                    handler.post(() -> {
                        if (connectionListener.onConnected(HttpClient.this))
                            connectionListener.onPingTested(HttpClient.this, System.currentTimeMillis() - initializedAt);
                    });
                }

                if (close) close();
            } catch (IOException ex) {
                if (connectionListener != null) {
                    handler.post(() -> connectionListener.onFailedConnecting(profile, ex));
                } else {
                    Logging.log(ex);
                }

                close();
            }
        });
    }

    @NonNull
    static HttpClient instantiate(@NonNull MultiProfile.UserProfile profile) throws InitializationException {
        try {
            return new HttpClient(profile);
        } catch (GeneralSecurityException | IOException | NetUtils.InvalidUrlException ex) {
            throw new InitializationException(ex);
        }
    }

    @UiThread
    public static Closeable checkConnection(@NonNull MultiProfile.UserProfile profile, @NonNull OnConnect listener, boolean close) {
        try {
            return new HttpClient(profile, listener, close);
        } catch (GeneralSecurityException | IOException | NetUtils.InvalidUrlException ex) {
            listener.onFailedConnecting(profile, ex);
            return null;
        }
    }

    @Override
    protected void closeClient() {
        executorService.shutdownNow();
    }

    @Override
    public void send(long id, @NonNull JSONObject request, @NonNull OnJson listener) {
        executeRunnable(new RequestProcessor(id, request, listener));
    }

    @NonNull
    @Override
    public JSONObject sendSync(long id, @NonNull JSONObject request) throws JSONException, NetUtils.InvalidUrlException, IOException, StatusCodeException, AriaException {
        try (Response resp = client.newCall(NetUtils.createPostRequest(profile, url, request)).execute()) {
            ResponseBody body = resp.body();
            if (body != null) {
                String json = body.string();
                if (json.isEmpty()) {
                    throw new IOException("Empty response");
                } else {
                    JSONObject obj = new JSONObject(json);
                    validateResponse(obj);
                    return obj;
                }
            } else {
                throw new StatusCodeException(resp);
            }
        }
    }

    @Override
    public <R> void batch(BatchSandbox<R> sandbox, DoBatch<R> listener) {
        executeRunnable(new SandboxRunnable<>(sandbox, listener));
    }

    private void executeRunnable(Runnable runnable) {
        try {
            if (!closed && !executorService.isShutdown() && !executorService.isTerminated())
                executorService.execute(runnable);
        } catch (RejectedExecutionException ignored) {
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
        @WorkerThread
        public void run() {
            try {
                listener.onSandboxReturned(sandbox.sandbox(HttpClient.this));
            } catch (Exception ex) {
                listener.onException(ex);
            }
        }
    }
}
