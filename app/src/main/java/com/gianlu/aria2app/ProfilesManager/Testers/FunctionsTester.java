package com.gianlu.aria2app.ProfilesManager.Testers;

import android.util.Base64;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.IConnect;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class FunctionsTester implements Runnable {
    private final ITesting listener;
    private final long startTime;
    private final NetProfileTester tester;
    private final MultiProfile.UserProfile profile;

    public FunctionsTester(NetProfileTester tester) throws IllegalStateException {
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

                                    try {
                                        HttpURLConnection conn = (HttpURLConnection) new URL(dd.address).openConnection();
                                        if (dd.auth)
                                            conn.addRequestProperty("Authorization", "Basic " + Base64.encodeToString((dd.username + ":" + dd.password).getBytes(), Base64.NO_WRAP));

                                        conn.connect();

                                        if (conn.getResponseCode() == 200) {
                                            publishUpdate("DirectDownload is set up properly.");
                                            publishResult(true, "Everything is ok!");
                                            publishEnd();
                                        } else if (conn.getResponseCode() == 401) {
                                            publishResult(false, "401: " + conn.getResponseMessage());
                                            publishUpdate("Username and/or password are wrong.");
                                        }

                                        conn.disconnect();
                                    } catch (IOException ex) {
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
