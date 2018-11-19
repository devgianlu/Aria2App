package com.gianlu.aria2app.NetIO;

import android.content.Context;

import com.gianlu.aria2app.NetIO.Aria2.AriaException;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.commonutils.Logging;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import okhttp3.Response;
import okhttp3.ResponseBody;


public class HttpClient extends AbstractClient {
    private static HttpClient instance;
    private final ExecutorService executorService;
    private final URI url;

    private HttpClient(Context context) throws GeneralSecurityException, IOException, NetUtils.InvalidUrlException, ProfilesManager.NoCurrentProfileException {
        this(context, ProfilesManager.get(context).getCurrentSpecific());
    }

    private HttpClient(Context context, MultiProfile.UserProfile profile) throws GeneralSecurityException, IOException, NetUtils.InvalidUrlException {
        super(context, profile);
        ErrorHandler.get().unlock();
        this.executorService = Executors.newCachedThreadPool();
        this.url = NetUtils.createHttpURL(profile);
    }

    private HttpClient(Context context, final MultiProfile.UserProfile profile, @Nullable final OnConnect connectionListener) throws GeneralSecurityException, IOException, NetUtils.InvalidUrlException {
        this(context, profile);

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
            } catch (final IOException ex) {
                if (connectionListener != null) {
                    handler.post(() -> connectionListener.onFailedConnecting(ex));
                } else {
                    Logging.log(ex);
                }

                close();
            }
        });
    }

    @NonNull
    public static HttpClient instantiate(Context context) throws InitializationException {
        if (instance == null) {
            try {
                instance = new HttpClient(context);
            } catch (GeneralSecurityException | IOException | NetUtils.InvalidUrlException | ProfilesManager.NoCurrentProfileException ex) {
                throw new InitializationException(ex);
            }
        }

        return instance;
    }

    public static void instantiate(Context context, MultiProfile.UserProfile profile, @NonNull final OnConnect listener) {
        try {
            instance = new HttpClient(context, profile, listener);
        } catch (GeneralSecurityException | IOException | NetUtils.InvalidUrlException ex) {
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
        executorService.shutdownNow();
    }

    @Override
    public void send(long id, @NonNull JSONObject request, @NonNull OnJson listener) {
        executeRunnable(new RequestProcessor(id, request, listener));
    }

    @NonNull
    @Override
    protected JSONObject sendSync(long id, @NonNull JSONObject request) throws JSONException, NetUtils.InvalidUrlException, IOException, StatusCodeException, AriaException {
        try (Response resp = client.newCall(NetUtils.createPostRequest(profile, url, request)).execute()) {
            ResponseBody body = resp.body();
            if (body != null) {
                String json = body.string();
                if (json == null || json.isEmpty()) {
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
        if (!shouldIgnoreCommunication && !executorService.isShutdown() && !executorService.isTerminated())
            executorService.execute(runnable);
    }

    @Override
    public void connectivityChanged(@NonNull Context context, @NonNull MultiProfile.UserProfile profile) throws Exception {
        instance = new HttpClient(context, profile);
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

    private class RequestProcessor implements Runnable {
        private final long id;
        private final JSONObject request;
        private final OnJson listener;

        RequestProcessor(long id, JSONObject request, OnJson listener) {
            this.id = id;
            this.request = request;
            this.listener = listener;
        }

        @Override
        @WorkerThread
        public void run() {
            try {
                listener.onResponse(sendSync(id, request));
            } catch (IllegalArgumentException | JSONException | StatusCodeException | AriaException | IOException | NetUtils.InvalidUrlException | IllegalStateException ex) {
                listener.onException(ex);
            }
        }
    }
}
