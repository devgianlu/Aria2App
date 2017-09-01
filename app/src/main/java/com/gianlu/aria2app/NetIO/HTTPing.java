package com.gianlu.aria2app.NetIO;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Base64;

import com.gianlu.aria2app.NetIO.JTA2.Aria2Exception;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class HTTPing extends AbstractClient {
    private static HTTPing httping;
    private final ExecutorService executorService;

    private HTTPing(Context context) throws CertificateException, IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        this(context, ProfilesManager.get(context).getCurrent(context).getProfile(context));
    }

    public HTTPing(Context context, MultiProfile.UserProfile profile) throws CertificateException, IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        super(context, profile);
        this.executorService = Executors.newCachedThreadPool();
        ErrorHandler.get().unlock();
    }

    public static HTTPing newInstance(Context context) throws NoSuchAlgorithmException, CertificateException, KeyManagementException, KeyStoreException, IOException {
        if (httping == null) httping = new HTTPing(context);
        return httping;
    }

    @Override
    public void send(final JSONObject request, final IReceived handler) {
        executorService.execute(new RequestProcessor(request, handler));
    }

    @Override
    public void connectivityChanged(@NonNull Context context, @NonNull MultiProfile.UserProfile profile) throws Exception {
        this.sslContext = NetUtils.readySSLContext(NetUtils.readyCertificate(context, profile));
    }

    @SuppressLint("BadHostnameVerifier")
    public HttpURLConnection readyHttpConnection(URL url) throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException, KeyManagementException {
        if (profile.serverSSL) {
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            conn.setSSLSocketFactory(sslContext.getSocketFactory());
            conn.setConnectTimeout(5000);

            if (profile.authMethod == JTA2.AuthMethod.HTTP)
                conn.addRequestProperty("Authorization", "Basic " + Base64.encodeToString((profile.serverUsername + ":" + profile.serverPassword).getBytes(), Base64.NO_WRAP));

            return conn;
        } else {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);

            if (profile.authMethod == JTA2.AuthMethod.HTTP)
                conn.addRequestProperty("Authorization", "Basic " + Base64.encodeToString((profile.serverUsername + ":" + profile.serverPassword).getBytes(), Base64.NO_WRAP));

            return conn;
        }
    }

    private class RequestProcessor implements Runnable {
        private final JSONObject request;
        private final IReceived listener;

        public RequestProcessor(JSONObject request, IReceived listener) {
            this.request = request;
            this.listener = listener;
        }

        @Override
        public void run() {
            try {
                String urlPath = profile.serverEndpoint +
                        "?method="
                        + request.getString("method")
                        + "&id=" + request.getString("id");

                if (request.has("params"))
                    urlPath += "&params=" + URLEncoder.encode(Base64.encodeToString(request.get("params").toString().getBytes(), Base64.NO_WRAP), "UTF-8");

                URL url = new URL(
                        profile.serverSSL ? "https" : "http",
                        profile.serverAddr,
                        profile.serverPort,
                        urlPath);

                HttpURLConnection conn = readyHttpConnection(url);
                conn.connect();

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String rawResponse = reader.readLine();

                    if (rawResponse == null) {
                        listener.onException(new NullPointerException("Empty response"));
                    } else {
                        JSONObject response = new JSONObject(rawResponse);
                        if (response.isNull("error")) {
                            listener.onResponse(response);
                        } else {
                            listener.onException(new Aria2Exception(response.getJSONObject("error")));
                        }
                    }
                } else {
                    listener.onException(new StatusCodeException(conn.getResponseCode(), conn.getResponseMessage()));
                }
            } catch (OutOfMemoryError ex) {
                System.gc();
            } catch (JSONException | IOException | NoSuchAlgorithmException | CertificateException | KeyManagementException | KeyStoreException ex) {
                listener.onException(ex);
            }
        }
    }
}
