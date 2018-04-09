package com.gianlu.aria2app.NetIO;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Response;
import okhttp3.ResponseBody;


public class HttpClient extends AbstractClient {
    private static HttpClient instance;
    private final ExecutorService executorService;
    private final URI url;

    private HttpClient(Context context) throws CertificateException, IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException, NetUtils.InvalidUrlException, ProfilesManager.NoCurrentProfileException {
        this(context, ProfilesManager.get(context).getCurrentSpecific());
    }

    private HttpClient(Context context, MultiProfile.UserProfile profile) throws CertificateException, IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException, NetUtils.InvalidUrlException {
        super(context, profile);
        ErrorHandler.get().unlock();
        this.executorService = Executors.newCachedThreadPool();
        this.url = NetUtils.createHttpURL(profile);
    }

    private HttpClient(Context context, final MultiProfile.UserProfile profile, @Nullable final OnConnect connectionListener) throws CertificateException, IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException, NetUtils.InvalidUrlException {
        this(context, profile);

        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try (Socket socket = new Socket()) {
                    long initializedAt = System.currentTimeMillis();
                    socket.connect(new InetSocketAddress(profile.serverAddr, profile.serverPort), (int) TimeUnit.SECONDS.toMillis(NetUtils.HTTP_TIMEOUT));
                    if (connectionListener != null)
                        if (connectionListener.onConnected(HttpClient.this))
                            connectionListener.onPingTested(HttpClient.this, System.currentTimeMillis() - initializedAt);
                } catch (IOException ex) {
                    if (connectionListener != null) connectionListener.onFailedConnecting(ex);
                    else Logging.log(ex);
                }
            }
        });
    }

    public static HttpClient instantiate(Context context) throws InitializationException {
        if (instance == null) {
            try {
                instance = new HttpClient(context);
            } catch (CertificateException | IOException | NoSuchAlgorithmException | KeyManagementException | NetUtils.InvalidUrlException | KeyStoreException | ProfilesManager.NoCurrentProfileException ex) {
                throw new InitializationException(ex);
            }
        }

        return instance;
    }

    public static void instantiate(Context context, MultiProfile.UserProfile profile, @NonNull final OnConnect listener) {
        try {
            instance = new HttpClient(context, profile, listener);
        } catch (CertificateException | IOException | KeyManagementException | NoSuchAlgorithmException | NetUtils.InvalidUrlException | KeyStoreException ex) {
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
    public void send(@NonNull JSONObject request, @NonNull OnJson listener) {
        executeRunnable(new RequestProcessor(request, listener));
    }

    @NonNull
    @Override
    protected JSONObject sendSync(@NonNull JSONObject request) throws JSONException, NetUtils.InvalidUrlException, IOException, StatusCodeException, AriaException {
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
        public void run() {
            try {
                listener.onSandboxReturned(sandbox.sandbox(HttpClient.this, shouldForce));
            } catch (Exception ex) {
                listener.onException(ex);
            }
        }
    }

    private class RequestProcessor implements Runnable {
        private final JSONObject request;
        private final OnJson listener;

        RequestProcessor(JSONObject request, OnJson listener) {
            this.request = request;
            this.listener = listener;
        }

        @Override
        public void run() {
            try {
                listener.onResponse(sendSync(request));
            } catch (IllegalArgumentException | JSONException | StatusCodeException | AriaException | IOException | NetUtils.InvalidUrlException | IllegalStateException ex) {
                listener.onException(ex);
            }
        }
    }
}
