package com.gianlu.aria2app.ProfilesManager.Testers;

import android.content.Context;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.HttpClient;
import com.gianlu.aria2app.NetIO.OnConnect;
import com.gianlu.aria2app.NetIO.WebSocketClient;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.commonutils.Logging;

import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

public class NetTester extends BaseTester<AbstractClient> {
    private final boolean close;
    private final ProfileTesterCallback profileListener;

    NetTester(Context context, MultiProfile.UserProfile profile, PublishListener<AbstractClient> listener) {
        super(context, profile, listener);
        this.profileListener = null;
        this.close = false;
    }

    public NetTester(Context context, MultiProfile.UserProfile profile, boolean close, ProfileTesterCallback profileListener) {
        super(context, profile, null);
        this.close = close;
        this.profileListener = profileListener;
    }

    @Nullable
    public AbstractClient call(@Nullable Object prevResult) {
        final AtomicReference<AbstractClient> lock = new AtomicReference<>(null);

        OnConnect listener = new OnConnect() {
            @Override
            public boolean onConnected(@NonNull AbstractClient client) {
                publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.ONLINE, -1));
                return true;
            }

            @Override
            public void onPingTested(@NonNull AbstractClient client, long latency) {
                publishPing(profile, latency);
                synchronized (lock) {
                    lock.set(client);
                    lock.notifyAll();
                }
            }

            @Override
            public void onFailedConnecting(@NonNull MultiProfile.UserProfile profile, @NonNull Throwable ex) {
                publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.OFFLINE, ex));
                synchronized (lock) {
                    lock.set(null);
                    lock.notifyAll();
                }
            }
        };

        switch (profile.connectionMethod) {
            case HTTP:
                HttpClient.checkConnection(profile, listener, false);
                break;
            case WEBSOCKET:
                WebSocketClient.checkConnection(profile, listener, false);
                break;
            default:
                return null;
        }

        synchronized (lock) {
            try {
                lock.wait(5000);
            } catch (InterruptedException ex) {
                Logging.log(ex);
            }

            AbstractClient client = lock.get();
            if (close && client != null) client.close();
            return client;
        }
    }

    @NonNull
    @Override
    public String describe() {
        return (profile.connectionMethod == MultiProfile.ConnectionMethod.WEBSOCKET ? "WebSocket" : "HTTP")
                + " connection test";
    }

    @UiThread
    private void publishResult(MultiProfile.UserProfile profile, MultiProfile.TestStatus status) {
        if (profileListener != null) {
            profileListener.statusUpdated(profile.getParent().id, status);
            return;
        }

        switch (status.status) {
            case OFFLINE:
                publishMessage("Host is offline, check your connection parameters", Level.ERROR);
                break;
            case ERROR:
                publishMessage("Failed establishing a connection", Level.ERROR);
                break;
            case ONLINE:
                publishMessage("Host is online, connection parameters are configured correctly", Level.SUCCESS);
                break;
            default:
            case UNKNOWN:
                break;
        }

        if (status.ex != null) {
            if (status.ex.getMessage() == null) {
                Throwable cause = status.ex.getCause();
                if (cause != null && cause.getMessage() != null)
                    publishMessage(cause.getMessage(), Level.ERROR);
            } else {
                publishMessage(status.ex.getMessage(), Level.ERROR);
            }
        }
    }

    @UiThread
    private void publishPing(MultiProfile.UserProfile profile, long ping) {
        if (profileListener != null) {
            profileListener.pingUpdated(profile.getParent().id, ping);
            return;
        }

        publishMessage("Latency is " + ping + "ms", Level.INFO);
    }

    public interface ProfileTesterCallback {
        @UiThread
        void statusUpdated(@NonNull String profileId, @NonNull MultiProfile.TestStatus status);

        @UiThread
        void pingUpdated(@NonNull String profileId, long ping);
    }
}
