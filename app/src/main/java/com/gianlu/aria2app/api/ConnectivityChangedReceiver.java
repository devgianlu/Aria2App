package com.gianlu.aria2app.api;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;

import java.util.Objects;

import androidx.annotation.NonNull;

public class ConnectivityChangedReceiver extends BroadcastReceiver {
    private final WifiManager wifiManager;

    public ConnectivityChangedReceiver(@NonNull Context context) {
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), ConnectivityManager.CONNECTIVITY_ACTION)) {
            boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            if (!noConnectivity)
                NetInstanceHolder.handleConnectivityChange(context, intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE, ConnectivityManager.TYPE_DUMMY), wifiManager);
        }
    }
}
