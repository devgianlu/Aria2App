package com.gianlu.aria2app.ProfilesManager.Testers;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.HttpClient;
import com.gianlu.aria2app.NetIO.OnConnect;
import com.gianlu.aria2app.NetIO.WebSocketClient;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.Logging;

import java.util.concurrent.atomic.AtomicReference;

public class NetTester extends BaseTester<AbstractClient> {
    private final IProfileTester profileListener;

    NetTester(Context context, MultiProfile.UserProfile profile, IPublish<AbstractClient> listener) {
        super(context, profile, listener);
        this.profileListener = null;
    }

    public NetTester(Context context, MultiProfile.UserProfile profile, IProfileTester profileListener) {
        super(context, profile, null);
        this.profileListener = profileListener;
    }

    @Nullable
    public AbstractClient call(@Nullable Object prevResult) {
        final AtomicReference<AbstractClient> lock = new AtomicReference<>(null);

        OnConnect listener = new OnConnect() {
            @Override
            public boolean onConnected(AbstractClient client) {
                publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.ONLINE, -1));
                return true;
            }

            @Override
            public void onPingTested(AbstractClient client, long latency) {
                publishPing(profile, latency);
                synchronized (lock) {
                    lock.set(client);
                    lock.notifyAll();
                }
            }

            @Override
            public void onFailedConnecting(Throwable ex) {
                publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.OFFLINE, ex));
                synchronized (lock) {
                    lock.set(null);
                    lock.notifyAll();
                }
            }
        };

        switch (profile.connectionMethod) {
            case HTTP:
                HttpClient.instantiate(context, profile, listener);
                break;
            case WEBSOCKET:
                WebSocketClient.instantiate(context, profile, listener);
                break;
            default:
                return null;
        }

        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException ex) {
                Logging.log(ex);
            }

            return lock.get();
        }
    }

    @NonNull
    @Override
    public String describe() {
        return "connection test";
    }

    private void publishResult(MultiProfile.UserProfile profile, MultiProfile.TestStatus status) {
        if (profileListener != null) {
            profileListener.statusUpdated(profile.getParent().id, status);
            return;
        }

        switch (status.status) {
            case OFFLINE:
                publishMessage("Host is offline, check your connection parameters", R.color.red);
                break;
            case ERROR:
                publishMessage("Failed establishing a connection", R.color.red);
                break;
            case ONLINE:
                publishMessage("Host is online, connection parameters are configured correctly", R.color.green);
                break;
            default:
            case UNKNOWN:
                break;
        }

        if (status.ex != null) {
            if (status.ex.getMessage() == null) {
                Throwable cause = status.ex.getCause();
                if (cause != null && cause.getMessage() != null)
                    publishMessage(cause.getMessage(), R.color.red);
            } else {
                publishMessage(status.ex.getMessage(), R.color.red);
            }
        }
    }

    private void publishPing(MultiProfile.UserProfile profile, long ping) {
        if (profileListener != null) {
            profileListener.pingUpdated(profile.getParent().id, ping);
            return;
        }

        publishMessage("Latency is " + ping + "ms", android.R.color.tertiary_text_light);
    }

    public interface IProfileTester {
        void statusUpdated(String profileId, MultiProfile.TestStatus status);

        void pingUpdated(String profileId, long ping);
    }
}
