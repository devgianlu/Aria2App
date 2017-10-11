package com.gianlu.aria2app.NetIO;

import android.content.Context;
import android.support.annotation.NonNull;

import com.gianlu.aria2app.NetIO.JTA2.AriaException;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cz.msebera.android.httpclient.Consts;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.HttpStatus;
import cz.msebera.android.httpclient.StatusLine;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.util.EntityUtils;

public class HTTPing extends AbstractClient {
    private static HTTPing httping;
    private final ExecutorService executorService;
    private final CloseableHttpClient client;
    private final URI defaultUri;

    private HTTPing(Context context) throws CertificateException, IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException, URISyntaxException {
        this(context, ProfilesManager.get(context).getCurrent(context).getProfile(context));
    }

    public HTTPing(Context context, MultiProfile.UserProfile profile) throws CertificateException, IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException, URISyntaxException {
        super(context, profile);
        ErrorHandler.get().unlock();
        this.executorService = Executors.newCachedThreadPool();
        this.client = NetUtils.buildHttpClient(profile, sslContext);
        this.defaultUri = NetUtils.createBaseURI(profile);
    }

    public static HTTPing instantiate(Context context) throws NoSuchAlgorithmException, CertificateException, KeyManagementException, KeyStoreException, IOException, URISyntaxException {
        if (httping == null) httping = new HTTPing(context);
        return httping;
    }

    public static HTTPing instantiate(Context context, MultiProfile.UserProfile profile) throws NoSuchAlgorithmException, CertificateException, KeyManagementException, KeyStoreException, IOException, URISyntaxException {
        if (httping == null) httping = new HTTPing(context, profile);
        return httping;
    }

    public static void instantiate(Context context, MultiProfile.UserProfile profile, @NonNull final IConnect listener) {
        try {
            httping = new HTTPing(context, profile);
            httping.sendConnectionTest(new IReceived() {
                @Override
                public void onResponse(JSONObject response) throws JSONException {
                    listener.onConnected(httping);
                }

                @Override
                public void onException(Exception ex) {
                    listener.onFailedConnecting(ex);
                }
            });
        } catch (CertificateException | IOException | KeyManagementException | NoSuchAlgorithmException | URISyntaxException | KeyStoreException ex) {
            listener.onFailedConnecting(ex);
        }
    }

    public static void clear() {
        clearConnectivityListener();
        if (httping != null) httping.clearInternal();
    }

    @Override
    protected void clearInternal() {
        executorService.shutdownNow();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    client.close();
                } catch (IOException ignored) {
                }
            }
        }).start();
    }

    @Override
    public void send(@NonNull final JSONObject request, final IReceived handler) {
        if (!executorService.isShutdown())
            executorService.execute(new RequestProcessor(request, handler));
    }

    private void sendConnectionTest(IReceived handler) {
        if (!executorService.isShutdown())
            executorService.execute(new RequestProcessor(null, handler));
    }

    @Override
    public void connectivityChanged(@NonNull Context context, @NonNull MultiProfile.UserProfile profile) throws Exception {
        if (httping != null) httping.client.close();
        httping = new HTTPing(context, profile);
    }

    private class RequestProcessor implements Runnable {
        private final JSONObject request;
        private final IReceived listener;

        RequestProcessor(JSONObject request, IReceived listener) {
            this.request = request;
            this.listener = listener;
        }

        @Override
        public void run() {
            try {
                HttpGet get = NetUtils.createGetRequest(profile, defaultUri, request);
                HttpResponse resp = client.execute(get);
                StatusLine sl = resp.getStatusLine();

                if (request == null) {
                    if (sl.getStatusCode() == HttpStatus.SC_BAD_REQUEST) listener.onResponse(null);
                    else listener.onException(new StatusCodeException(sl));
                    return;
                }

                HttpEntity entity = resp.getEntity();
                if (entity != null) {
                    String json = EntityUtils.toString(entity, Consts.UTF_8);
                    if (json == null || json.isEmpty()) {
                        listener.onException(new NullPointerException("Empty response"));
                    } else {
                        JSONObject obj = new JSONObject(json);
                        if (obj.has("error")) {
                            listener.onException(new AriaException(obj.getJSONObject("error")));
                        } else {
                            listener.onResponse(obj);
                        }
                    }
                } else {
                    listener.onException(new StatusCodeException(sl));
                }

                get.releaseConnection();
            } catch (OutOfMemoryError ex) {
                System.gc();
            } catch (JSONException | IOException | URISyntaxException | IllegalStateException ex) {
                listener.onException(ex);
            }
        }
    }
}
