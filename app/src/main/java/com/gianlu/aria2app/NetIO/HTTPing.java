package com.gianlu.aria2app.NetIO;

import android.content.Context;
import android.util.Base64;

import com.gianlu.aria2app.CurrentProfile;
import com.gianlu.aria2app.NetIO.JTA2.Aria2Exception;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.Profile.SingleModeProfileItem;
import com.gianlu.aria2app.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

public class HTTPing extends AbstractClient {
    private static HTTPing httping;
    private final SingleModeProfileItem profile;
    private final SSLContext sslContext;

    private HTTPing(Context context) throws CertificateException, IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        profile = CurrentProfile.getCurrentProfile(context);
        sslContext = Utils.readySSLContext(Utils.readyCertificate(context));
    }

    public static HTTPing newInstance(Context context) throws NoSuchAlgorithmException, CertificateException, KeyManagementException, KeyStoreException, IOException {
        if (httping == null)
            httping = new HTTPing(context);

        return httping;
    }

    @Override
    public void send(JSONObject request, IReceived handler) {
        URL url;
        try {
            url = new URL(
                    profile.serverSSL ? "https" : "http",
                    profile.serverAddr,
                    "/jsonrpc?method="
                            + request.getString("method")
                            + "&id=" + request.getString("id")
                            + "&params=" + URLEncoder.encode(Base64.encodeToString(request.get("params").toString().getBytes(), Base64.NO_WRAP), "UTF-8"));
        } catch (JSONException | MalformedURLException | UnsupportedEncodingException ex) {
            handler.onException(false, ex);
            return;
        }

        try {
            HttpURLConnection conn = readyHttpConnection(url);
            conn.connect();

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String rawResponse = reader.readLine();

                if (rawResponse == null) {
                    handler.onException(false, new NullPointerException("Empty response"));
                } else {
                    JSONObject response = new JSONObject(rawResponse);
                    if (response.isNull("error")) {
                        handler.onResponse(response);
                    } else {
                        handler.onException(false, new Aria2Exception(response.getJSONObject("error").getString("message"), response.getJSONObject("error").getInt("code")));
                    }
                }
            } else {
                handler.onException(false, new StatusCodeException(conn.getResponseCode(), conn.getResponseMessage()));
            }
        } catch (JSONException | IOException | NoSuchAlgorithmException | CertificateException | KeyManagementException | KeyStoreException ex) {
            handler.onException(false, ex);
        }
    }

    public HttpURLConnection readyHttpConnection(URL url) throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException, KeyManagementException {
        if (profile.serverSSL) {
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
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
}
