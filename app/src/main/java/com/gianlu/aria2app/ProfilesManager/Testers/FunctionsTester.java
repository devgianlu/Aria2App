package com.gianlu.aria2app.ProfilesManager.Testers;

import android.util.Base64;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.IConnect;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.HttpStatus;
import cz.msebera.android.httpclient.StatusLine;
import cz.msebera.android.httpclient.client.config.RequestConfig;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;

public class FunctionsTester implements Runnable {
    private final ITesting listener;
    private final long startTime;
    private final NetProfileTester tester;
    private final MultiProfile.UserProfile profile;

    public FunctionsTester(NetProfileTester tester) {
        this.listener = tester.listener;
        this.startTime = tester.startTime;
        this.tester = tester;
        this.profile = tester.profile;
    }

    private void publishUpdate(String message) {
        if (listener != null)
            listener.onUpdate(String.valueOf(System.currentTimeMillis() - startTime) + ": " + message);
    }

    private void publishResult(boolean successful, String message) {
        if (listener != null)
            listener.onAria2Result(successful, String.valueOf(System.currentTimeMillis() - startTime) + ": " + message);
    }

    private void publishEnd() {
        if (listener != null) listener.onEnd();
    }

    @Override
    public void run() {
        publishUpdate("Started aria2 test...");

        tester.getClient(new IConnect() {
            @Override
            public void onConnected(AbstractClient client) {
                final JTA2 jta2 = new JTA2(tester.context, client);

                publishUpdate("Trying no-auth request...");
                jta2.listMethods(new JTA2.IMethod() {
                    @Override
                    public void onMethods(List<String> methods) {
                        publishUpdate("No-auth request was successful.");

                        publishUpdate("Trying auth request...");
                        jta2.getVersion(profile, new JTA2.IVersion() {
                            @Override
                            public void onVersion(List<String> rawFeatures, String version) {
                                publishUpdate("Auth request was successful.");

                                if (profile.isDirectDownloadEnabled()) {
                                    MultiProfile.DirectDownload dd = profile.directDownload;
                                    if (dd == null) return;

                                    publishUpdate("Started DirectDownload test...");

                                    try (CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom()
                                            .setConnectTimeout(5000)
                                            .setConnectionRequestTimeout(5000)
                                            .setSocketTimeout(5000)
                                            .build()).build()) {

                                        HttpGet get = new HttpGet(dd.getURLAddress());
                                        if (dd.auth)
                                            get.addHeader("Authorization", "Basic " + Base64.encodeToString((dd.username + ":" + dd.password).getBytes(), Base64.NO_WRAP));

                                        HttpResponse resp = client.execute(get);
                                        StatusLine sl = resp.getStatusLine();
                                        if (sl.getStatusCode() == HttpStatus.SC_OK) {
                                            publishUpdate("DirectDownload is set up properly.");
                                            publishResult(true, "Everything is ok!");
                                            publishEnd();
                                        } else if (sl.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                                            publishResult(false, "401: " + sl.getReasonPhrase());
                                            publishUpdate("Username and/or password are wrong.");
                                        } else {
                                            publishResult(false, sl.getStatusCode() + ": " + sl.getReasonPhrase());
                                        }

                                        get.releaseConnection();
                                    } catch (IOException | URISyntaxException ex) {
                                        publishResult(false, ex.getMessage());
                                    }
                                } else {
                                    publishResult(true, "Everything is ok!");
                                    publishEnd();
                                }
                            }

                            @Override
                            public void onException(Exception ex) {
                                publishResult(false, "Auth request failed! (" + ex.getMessage() + ")");
                                publishUpdate("Token may be wrong.");
                            }
                        });
                    }

                    @Override
                    public void onException(Exception ex) {
                        publishResult(false, "No-auth request failed! (" + ex.getMessage() + ")");
                        publishUpdate("Username and password may be wrong.");
                    }
                });
            }

            @Override
            public void onFailedConnecting(Exception ex) {
                publishResult(false, "Connection failed! (" + ex.getMessage() + ")");
            }
        });
    }
}
