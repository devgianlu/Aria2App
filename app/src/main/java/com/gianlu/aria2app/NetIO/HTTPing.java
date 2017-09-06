package com.gianlu.aria2app.NetIO;

import android.content.Context;
import android.support.annotation.NonNull;

import com.gianlu.aria2app.NetIO.JTA2.Aria2Exception;
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
        this.client = NetUtils.buildHttpClient(context, profile);
        this.defaultUri = NetUtils.createBaseURI(profile);
    }

    public static HTTPing newInstance(Context context) throws NoSuchAlgorithmException, CertificateException, KeyManagementException, KeyStoreException, IOException, URISyntaxException {
        if (httping == null) httping = new HTTPing(context);
        return httping;
    }

    @Override
    public void send(final JSONObject request, final IReceived handler) {
        executorService.execute(new RequestProcessor(request, handler));
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
                if (sl.getStatusCode() == HttpStatus.SC_OK) {
                    String rawResponse = EntityUtils.toString(resp.getEntity());

                    if (rawResponse == null) {
                        listener.onException(new NullPointerException("Empty response"));
                    } else {
                        JSONObject response = new JSONObject(rawResponse);
                        if (response.has("error"))
                            listener.onException(new Aria2Exception(response.getJSONObject("error")));
                        else
                            listener.onResponse(response);
                    }
                } else {
                    listener.onException(new StatusCodeException(sl)); // FIXME: WHY THE FUCK SOME REQUESTS RETURN 400
                }

                get.releaseConnection();
            } catch (OutOfMemoryError ex) {
                System.gc();
            } catch (JSONException | IOException | URISyntaxException ex) {
                listener.onException(ex);
            }
        }
    }
}
