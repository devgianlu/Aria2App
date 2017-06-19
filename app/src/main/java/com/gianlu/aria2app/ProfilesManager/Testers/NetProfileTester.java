package com.gianlu.aria2app.ProfilesManager.Testers;


import android.content.Context;

import com.gianlu.aria2app.NetIO.IConnect;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;

public abstract class NetProfileTester implements Runnable {
    protected final Context context;
    protected final MultiProfile.UserProfile profile;
    protected final ITesting listener;
    protected long startTime;

    public NetProfileTester(Context context, MultiProfile.UserProfile profile, ITesting listener) {
        this.context = context;
        this.profile = profile;
        this.listener = listener;
        this.startTime = System.currentTimeMillis();
    }

    protected final void publishResult(MultiProfile.UserProfile profile, MultiProfile.TestStatus status) {
        if (listener != null)
            listener.onConnectionResult(this, profile, System.currentTimeMillis() - startTime, status);
    }

    protected final void publishUpdate(String message) {
        if (listener != null)
            listener.onUpdate(String.valueOf(System.currentTimeMillis() - startTime) + ": " + message);
    }

    public abstract void getClient(IConnect listener);
}
