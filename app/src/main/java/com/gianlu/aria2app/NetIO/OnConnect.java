package com.gianlu.aria2app.NetIO;

import android.support.annotation.NonNull;
import android.support.annotation.UiThread;

public interface OnConnect {
    /**
     * @return Whether we want a ping test
     */
    @UiThread
    boolean onConnected(@NonNull AbstractClient client);

    @UiThread
    void onPingTested(@NonNull AbstractClient client, long latency);

    @UiThread
    void onFailedConnecting(@NonNull Throwable ex);
}