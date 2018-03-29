package com.gianlu.aria2app.ProfilesManager.Testers;

import android.content.Context;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.VersionInfo;
import com.gianlu.aria2app.NetIO.AriaRequests;
import com.gianlu.aria2app.NetIO.HttpClient;
import com.gianlu.aria2app.NetIO.OnConnect;
import com.gianlu.aria2app.NetIO.WebSocketClient;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.Logging;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

class Aria2Tester extends BaseTester {
    Aria2Tester(Context context, MultiProfile.UserProfile profile, IPublish listener) {
        super(context, profile, listener);
    }

    @Nullable
    private AbstractClient getClient() {
        final AtomicReference<AbstractClient> returnValue = new AtomicReference<>();

        OnConnect listener = new OnConnect() {
            @Override
            public void onConnected(AbstractClient client) {
                synchronized (returnValue) {
                    returnValue.set(client);
                    returnValue.notify();
                }
            }

            @Override
            public void onFailedConnecting(Throwable ex) {
                synchronized (returnValue) {
                    returnValue.set(null);
                    returnValue.notify();
                }
            }
        };

        if (profile.connectionMethod == MultiProfile.ConnectionMethod.HTTP) {
            HttpClient.instantiate(context, profile, listener);
        } else {
            WebSocketClient.instantiate(context, profile, listener);
        }

        synchronized (returnValue) {
            try {
                returnValue.wait();
            } catch (InterruptedException ignored) {
            }

            return returnValue.get();
        }
    }

    private void publishError(Exception ex, boolean authenticated) {
        publishMessage(ex.getMessage(), R.color.red);
        if (authenticated)
            publishMessage("Your token or username and password may be wrong", R.color.red);
    }

    private boolean listMethods(Aria2Helper aria2Helper) {
        final AtomicBoolean returnValue = new AtomicBoolean(false);

        aria2Helper.request(AriaRequests.listMethods(), new AbstractClient.OnResult<List<String>>() {
            @Override
            public void onResult(List<String> result) {
                synchronized (returnValue) {
                    returnValue.set(true);
                    returnValue.notify();
                }
            }

            @Override
            public void onException(Exception ex) {
                publishError(ex, false);

                synchronized (returnValue) {
                    returnValue.set(false);
                    returnValue.notify();
                }
            }
        });

        synchronized (returnValue) {
            try {
                returnValue.wait();
            } catch (InterruptedException ignored) {
            }

            return returnValue.get();
        }
    }

    private boolean getVersion(Aria2Helper aria2Helper) {
        final AtomicBoolean lock = new AtomicBoolean(false);

        aria2Helper.request(AriaRequests.getVersion(), new AbstractClient.OnResult<VersionInfo>() {
            @Override
            public void onResult(VersionInfo info) {
                synchronized (lock) {
                    lock.set(true);
                    lock.notify();
                }
            }

            @Override
            public void onException(Exception ex) {
                publishError(ex, true);

                synchronized (lock) {
                    lock.set(false);
                    lock.notify();
                }
            }
        });

        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException ex) {
                Logging.log(ex);
            }

            return lock.get();
        }
    }

    @Override
    public Boolean call() {
        AbstractClient client = getClient();
        if (client == null) return false;
        Aria2Helper aria2Helper = new Aria2Helper(context, client);

        publishMessage("Started not authenticated request...", android.R.color.tertiary_text_light);
        if (!listMethods(aria2Helper)) return false;
        publishMessage("Not authenticated request was successful", R.color.green);
        publishMessage("Started authenticated request...", android.R.color.tertiary_text_light);
        if (!getVersion(aria2Helper)) return false;
        publishMessage("Authenticated request was successful", R.color.green);

        return true;
    }

    @Override
    public String describe() {
        return "aria2 test";
    }
}
