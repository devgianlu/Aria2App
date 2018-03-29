package com.gianlu.aria2app.ProfilesManager.Testers;

import android.content.Context;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.ProfilesManager.MultiProfile;

public abstract class BaseTester { // TODO: Rewrite
    protected final Context context;
    protected final MultiProfile.UserProfile profile;
    private final IPublish listener;

    BaseTester(Context context, MultiProfile.UserProfile profile, @Nullable IPublish listener) {
        this.context = context;
        this.profile = profile;
        this.listener = listener;
    }

    protected final void publishMessage(String message, @ColorRes int color) {
        if (listener != null) listener.publishGeneralMessage(message, color);
    }

    public final boolean start() {
        if (listener != null) listener.startedNewTest(this);
        boolean a = call();
        if (listener != null) listener.endedTest(this, a);
        return a;
    }

    /**
     * @return true if the test was successful, false otherwise
     */
    protected abstract Boolean call();

    public abstract String describe();

    public interface IPublish {
        void startedNewTest(BaseTester tester);

        void publishGeneralMessage(String message, @ColorRes int color);

        void endedTest(BaseTester tester, boolean successful);
    }
}
