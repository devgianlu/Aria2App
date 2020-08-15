package com.gianlu.aria2app.api;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.gianlu.aria2app.LoadingActivity;
import com.gianlu.aria2app.downloader.DirectDownloadHelper;
import com.gianlu.aria2app.profiles.MultiProfile;
import com.gianlu.aria2app.profiles.ProfilesManager;

import java.util.Objects;

public final class NetInstanceHolder {
    private static final NetInstanceHolder instance = new NetInstanceHolder();
    private static final String TAG = NetInstanceHolder.class.getSimpleName();
    private final Reference reference = new Reference();
    private AbstractClient current;

    private NetInstanceHolder() {
    }

    static void handleConnectivityChange(Context context, int networkType, @NonNull WifiManager wifiManager) {
        if (instance.current == null) return;

        MultiProfile.UserProfile user = instance.current.profile.getParent().getProfile(networkType, wifiManager);
        if (!Objects.equals(instance.current.profile.connectivityCondition, user.connectivityCondition)) {
            DirectDownloadHelper.invalidate();

            try {
                close();
                instantiate(user);
            } catch (AbstractClient.InitializationException ex) {
                Log.e(TAG, "Failed initializing.", ex);
                NetInstanceHolder.close();
                ProfilesManager.get(context).unsetLastProfile();
                LoadingActivity.startActivity(context, ex);
            }
        }
    }

    @NonNull
    public static Reference instantiate(@NonNull MultiProfile.UserProfile profile) throws AbstractClient.InitializationException {
        instance.handleInstantiate(profile);
        return instance.reference;
    }

    public static void close() {
        if (instance.current != null)
            instance.current.close();
    }

    static void hasBeenClosed(@NonNull AbstractClient client) {
        if (instance.current == client)
            instance.current = null;
    }

    @NonNull
    public static ClientInterface referenceFor(AbstractClient client) {
        if (instance.current == client) return instance.reference;
        else return client;
    }

    private void handleInstantiate(@NonNull MultiProfile.UserProfile profile) throws AbstractClient.InitializationException {
        if (profile.connectionMethod == MultiProfile.ConnectionMethod.WEBSOCKET) {
            if (!(current instanceof WebSocketClient)) {
                if (current != null) current.close();
                current = WebSocketClient.instantiate(profile);
            }
        } else if (profile.connectionMethod == MultiProfile.ConnectionMethod.HTTP) {
            if (!(current instanceof HttpClient)) {
                if (current != null) current.close();
                current = HttpClient.instantiate(profile);
            }
        } else {
            throw new IllegalStateException("Unknown connection method: " + profile.connectionMethod);
        }
    }

    public class Reference implements ClientInterface {

        @Override
        public void close() {
            if (current != null) current.close();
        }

        @Override
        public <R> void send(@NonNull AbstractClient.AriaRequestWithResult<R> request, AbstractClient.OnResult<R> listener) {
            if (current != null) current.send(request, listener);
        }

        @Override
        public void send(@NonNull AbstractClient.AriaRequest request, AbstractClient.OnSuccess listener) {
            if (current != null) current.send(request, listener);
        }

        @Override
        public <R> void batch(@NonNull AbstractClient.BatchSandbox<R> sandbox, AbstractClient.OnResult<R> listener) {
            if (current != null) current.batch(sandbox, listener);
        }

        @Override
        public boolean isInAppDownloader() {
            return current != null && current.isInAppDownloader();
        }
    }
}
