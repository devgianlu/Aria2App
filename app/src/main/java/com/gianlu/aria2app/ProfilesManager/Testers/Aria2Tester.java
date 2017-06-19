package com.gianlu.aria2app.ProfilesManager.Testers;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.IConnect;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;

import java.util.List;

public class Aria2Tester implements Runnable {
    private final ITesting listener;
    private final long startTime;
    private final NetProfileTester tester;

    public Aria2Tester(NetProfileTester tester) throws IllegalStateException {
        this.listener = tester.listener;
        this.startTime = tester.startTime;
        this.tester = tester;
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
                        jta2.getVersion(new JTA2.IVersion() {
                            @Override
                            public void onVersion(List<String> rawFeatures, String version) {
                                publishUpdate("Auth request was successful.");
                                publishResult(true, "Everything is ok!");
                                publishEnd();
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
                // TODO
            }
        });
    }
}
