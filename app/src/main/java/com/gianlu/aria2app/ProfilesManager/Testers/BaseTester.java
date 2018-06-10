package com.gianlu.aria2app.ProfilesManager.Testers;

import android.content.Context;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;

import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.R;

public abstract class BaseTester<T> implements Runnable {
    protected final Context context;
    protected final MultiProfile.UserProfile profile;
    private final PublishListener<T> listener;

    BaseTester(Context context, MultiProfile.UserProfile profile, @Nullable PublishListener<T> listener) {
        this.context = context;
        this.profile = profile;
        this.listener = listener;
    }

    @Override
    public void run() {
        start(null);
    }

    @UiThread
    protected final void publishMessage(String message, Level level) {
        if (listener != null) listener.publishGeneralMessage(message, level.color);
    }

    public enum Level {
        INFO(android.R.color.tertiary_text_light),
        ERROR(R.color.red),
        SUCCESS(R.color.green);

        private final int color;

        Level(@ColorRes int color) {
            this.color = color;
        }
    }

    public final T start(@Nullable Object prevResult) {
        if (listener != null) listener.startedNewTest(this);
        T a = call(prevResult);
        if (listener != null) listener.endedTest(this, a);
        return a;
    }

    /**
     * @return true if the test was successful, false otherwise
     */
    @Nullable
    protected abstract T call(@Nullable Object prevResult);

    @NonNull
    public abstract String describe();

    public interface PublishListener<T> {
        @UiThread
        void startedNewTest(@NonNull BaseTester tester);

        @UiThread
        void publishGeneralMessage(@NonNull String message, @ColorRes int color);

        @UiThread
        void endedTest(@NonNull BaseTester tester, @Nullable T result);
    }
}
