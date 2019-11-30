package com.gianlu.aria2app.api;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import com.gianlu.aria2app.api.aria2.AriaException;
import com.gianlu.aria2app.profiles.MultiProfile;
import com.gianlu.aria2app.ThisApplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Closeable;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.atomic.AtomicLong;

import okhttp3.OkHttpClient;

public abstract class AbstractClient implements Closeable, ClientInterface {
    protected final OkHttpClient client;
    protected final Handler handler;
    private final AtomicLong requestIds = new AtomicLong(Long.MIN_VALUE);
    protected MultiProfile.UserProfile profile;
    protected volatile boolean closed = false;

    AbstractClient(@NonNull MultiProfile.UserProfile profile) throws GeneralSecurityException, IOException {
        this.client = NetUtils.buildClient(profile);
        this.profile = profile;
        this.handler = new Handler(Looper.getMainLooper());

        ThisApplication.setCrashlyticsString("connectionMethod", profile.connectionMethod.name());
    }

    @Override
    public boolean isInAppDownloader() {
        return profile.isInAppDownloader();
    }

    @Override
    @WorkerThread
    public final void close() {
        closed = true;
        closeClient();
        NetInstanceHolder.hasBeenClosed(this);
    }

    @WorkerThread
    protected abstract void closeClient();

    @Override
    public final <R> void send(@NonNull final AriaRequestWithResult<R> request, final OnResult<R> listener) {
        try {
            JSONObject obj = request.build(this);
            send(request.id, obj, new OnJson() {
                @Override
                public void onResponse(@NonNull JSONObject response) throws JSONException {
                    R result = request.processor.process(NetInstanceHolder.referenceFor(AbstractClient.this), response);
                    handler.post(() -> listener.onResult(result));
                }

                @Override
                public void onException(@NonNull Exception ex) {
                    handler.post(() -> listener.onException(ex));
                }
            });
        } catch (JSONException ex) {
            handler.post(() -> listener.onException(ex));
        }
    }

    public final void sendSync(@NonNull AriaRequest request) throws Exception {
        JSONObject obj = request.build(this);
        sendSync(request.id, obj);
    }

    @NonNull
    public final <R> R sendSync(@NonNull AriaRequestWithResult<R> request) throws Exception {
        JSONObject obj = request.build(this);
        return request.processor.process(this, sendSync(request.id, obj));
    }

    @Override
    public final void send(@NonNull AriaRequest request, OnSuccess listener) {
        try {
            JSONObject obj = request.build(this);
            send(request.id, obj, new OnJson() {
                @Override
                public void onResponse(@NonNull JSONObject response) {
                    handler.post(listener::onSuccess);
                }

                @Override
                public void onException(@NonNull Exception ex) {
                    handler.post(() -> listener.onException(ex));
                }
            });
        } catch (JSONException ex) {
            handler.post(() -> listener.onException(ex));
        }
    }

    protected abstract void send(long id, @NonNull JSONObject request, @NonNull OnJson listener);

    @NonNull
    protected abstract JSONObject sendSync(long id, @NonNull JSONObject request) throws Exception;

    protected final void validateResponse(JSONObject resp) throws JSONException, AriaException {
        if (resp.has("error")) throw new AriaException(resp.getJSONObject("error"));
    }

    public final <R> void batch(@NonNull BatchSandbox<R> sandbox, OnResult<R> listener) {
        batch(sandbox, new DoBatch<R>() {
            @Override
            public void onSandboxReturned(@NonNull R result) {
                handler.post(() -> listener.onResult(result));
            }

            @Override
            public void onException(@NonNull Exception ex) {
                handler.post(() -> listener.onException(ex));
            }
        });
    }

    protected abstract <R> void batch(BatchSandbox<R> sandbox, DoBatch<R> listener);

    @NonNull
    private JSONArray baseRequestParams() {
        JSONArray array = new JSONArray();
        if (profile.authMethod == AuthMethod.TOKEN) array.put("token:" + profile.serverToken);
        return array;
    }

    private long nextRequestId() {
        synchronized (requestIds) {
            return requestIds.getAndIncrement();
        }
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
        GET_DOWNLOAD_OPTIONS("aria2.getOption"),
        GET_FILES("aria2.getFiles"),
        CHANGE_POSITION("aria2.changePosition"),
        CHANGE_DOWNLOAD_OPTIONS("aria2.changeOption");

        private final String method;

        Method(String method) {
            this.method = method;
        }
    }

    public interface BatchSandbox<R> {
        @WorkerThread
        R sandbox(@NonNull AbstractClient client) throws Exception;
    }

    public interface OnResult<R> {
        @UiThread
        void onResult(@NonNull R result);

        @UiThread
        void onException(@NonNull Exception ex);
    }

    public interface OnSuccess {
        @UiThread
        void onSuccess();

        @UiThread
        void onException(@NonNull Exception ex);
    }

    public abstract static class Processor<R> {

        @NonNull
        @WorkerThread
        public abstract R process(@NonNull ClientInterface client, @NonNull JSONObject obj) throws JSONException;
    }

    public static class AriaRequest {
        public final Method method;
        public final Object[] params;
        public long id;

        AriaRequest(Method method, Object... params) {
            this.method = method;
            this.params = params;
        }

        @NonNull
        JSONObject build(@NonNull AbstractClient client) throws JSONException {
            id = client.nextRequestId();

            JSONObject request = new JSONObject();
            request.put("jsonrpc", "2.0");
            request.put("id", String.valueOf(id));
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

    protected class RequestProcessor implements Runnable {
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
            } catch (Exception ex) {
                listener.onException(ex);
            }
        }
    }
}
