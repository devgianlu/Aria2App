package com.gianlu.aria2app.NetIO;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;

import com.gianlu.aria2app.ProfilesManager.MultiProfile;

import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.net.ssl.SSLContext;

public abstract class AbstractClient {
    private static final List<WeakReference<OnConnectivityChanged>> listeners = new ArrayList<>();
    private final WifiManager wifiManager;
    private final Context context;
    private final ConnectivityChangedReceiver connectivityChangedReceiver;
    protected MultiProfile.UserProfile profile;
    protected SSLContext sslContext;

    AbstractClient(Context context, MultiProfile.UserProfile profile) throws CertificateException, IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.sslContext = NetUtils.createSSLContext(profile.certificate);
        this.profile = profile;
        this.context = context;
        this.connectivityChangedReceiver = new ConnectivityChangedReceiver();

        context.getApplicationContext().registerReceiver(connectivityChangedReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    public static void addConnectivityListener(OnConnectivityChanged listener) {
        for (WeakReference<OnConnectivityChanged> ref : listeners)
            if (ref.get() == listener)
                return;

        listeners.add(new WeakReference<>(listener));
    }

    public static void removeConnectivityListener(OnConnectivityChanged listener) {
        Iterator<WeakReference<OnConnectivityChanged>> iterator = listeners.listIterator();
        while (iterator.hasNext()) {
            WeakReference<OnConnectivityChanged> ref = iterator.next();
            if (ref.get() == null || ref.get() == listener) iterator.remove();
        }
    }

    static void clearConnectivityListener() {
        listeners.clear();
    }

    @Override
    protected void finalize() throws Throwable {
        context.getApplicationContext().unregisterReceiver(connectivityChangedReceiver);
        super.finalize();
    }

    protected abstract void clearInternal();

    public abstract void send(JSONObject request, IReceived handler);

    protected abstract void connectivityChanged(@NonNull Context context, @NonNull MultiProfile.UserProfile profile) throws Exception;

    public interface OnConnectivityChanged {
        void connectivityChanged(@NonNull MultiProfile.UserProfile profile);
    }

    public static class InitializationException extends Exception {
        InitializationException(Throwable cause) {
            super(cause);
        }
    }

    private class ConnectivityChangedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (Objects.equals(intent.getAction(), ConnectivityManager.CONNECTIVITY_ACTION)) {
                boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                if (!noConnectivity) {
                    int networkType = intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE, ConnectivityManager.TYPE_DUMMY);
                    final MultiProfile.UserProfile profile = AbstractClient.this.profile.getParent().getProfile(networkType, wifiManager);
                    if (!Objects.equals(AbstractClient.this.profile.connectivityCondition, profile.connectivityCondition)) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    connectivityChanged(context, profile);
                                    AbstractClient.this.profile = profile;

                                    Iterator<WeakReference<OnConnectivityChanged>> iterator = listeners.listIterator();
                                    while (iterator.hasNext()) {
                                        WeakReference<OnConnectivityChanged> ref = iterator.next();
                                        if (ref.get() == null) iterator.remove();
                                        else ref.get().connectivityChanged(profile);
                                    }
                                } catch (Exception ex) {
                                    ErrorHandler.get().notifyException(ex, true);
                                }
                            }
                        }).start();
                    }
                }
            }
        }
    }
}
