package com.gianlu.aria2app.NetIO;

import com.gianlu.aria2app.ProfilesManager.MultiProfile;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

@UiThread
public interface OnConnect {
    /**
     * @return Whether we want a ping test
     */
    boolean onConnected(@NonNull AbstractClient client);

    void onPingTested(@NonNull AbstractClient client, long latency);

    void onFailedConnecting(@NonNull MultiProfile.UserProfile profile, @NonNull Throwable ex);
}