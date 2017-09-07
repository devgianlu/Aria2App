package com.gianlu.aria2app.NetIO;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;

import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;

import org.json.JSONObject;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.net.ssl.SSLContext;

public abstract class AbstractClient {
    private static final List<OnConnectivityChanged> listeners = new ArrayList<>();
    private final WifiManager wifiManager;
    protected MultiProfile.UserProfile profile;
    protected SSLContext sslContext;

    public AbstractClient(Context context, MultiProfile.UserProfile profile) throws CertificateException, IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.sslContext = NetUtils.readySSLContext(NetUtils.readyCertificate(context, profile));
        this.profile = profile;
        context.getApplicationContext().registerReceiver(new ConnectivityChangedReceiver(), new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    public static void addConnectivityListener(OnConnectivityChanged listener) {
        listeners.add(listener);
    }

    public static void removeConnectivityListener(OnConnectivityChanged listener) {
        listeners.remove(listener);
    }

    public static void clearConnectivityListener() {
        listeners.clear();
    }

    protected abstract void clearInternal();

    public abstract void send(JSONObject request, IReceived handler);

    public abstract void connectivityChanged(@NonNull Context context, @NonNull MultiProfile.UserProfile profile) throws Exception;

    public interface OnConnectivityChanged {
        void connectivityChanged(@NonNull MultiProfile.UserProfile profile);
    }

    private class ConnectivityChangedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), ConnectivityManager.CONNECTIVITY_ACTION)) {
                boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                if (!noConnectivity) {
                    int networkType = intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE, ConnectivityManager.TYPE_DUMMY);
                    MultiProfile.UserProfile profile = ProfilesManager.get(context).getCurrent(context).getProfile(networkType, wifiManager);
                    if (!Objects.equals(AbstractClient.this.profile.connectivityCondition, profile.connectivityCondition)) {
                        try {
                            connectivityChanged(context, profile);
                            AbstractClient.this.profile = profile;

                            for (OnConnectivityChanged listener : listeners)
                                listener.connectivityChanged(profile);
                        } catch (Exception ex) {
                            ErrorHandler.get().notifyException(ex, true);
                        }
                    }
                }
            }
        }
    }
}
