package com.gianlu.aria2app.ProfilesManager.Testers;


import android.content.Context;

import com.gianlu.aria2app.ProfilesManager.MultiProfile;

// TODO: Send data with onUpdate() method
public abstract class ProfileTester implements Runnable {
    protected final Context context;
    protected final MultiProfile.UserProfile profile;
    protected final IResult listener;

    public ProfileTester(Context context, MultiProfile.UserProfile profile, IResult listener) {
        this.context = context;
        this.profile = profile;
        this.listener = listener;
    }

    protected final void publishResult(MultiProfile.UserProfile profile, MultiProfile.TestStatus status) {
        if (listener != null) listener.onResult(profile, status);
    }

    public interface IResult {
        void onUpdate();

        void onResult(MultiProfile.UserProfile profile, MultiProfile.TestStatus status);
    }
}
