package com.gianlu.aria2app.profiles.testers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.core.content.ContextCompat;

import com.gianlu.aria2app.R;
import com.gianlu.aria2app.profiles.MultiProfile;
import com.gianlu.commonutils.CommonUtils;

import org.jetbrains.annotations.NotNull;

public abstract class BaseTester<T> implements Runnable {
    protected final Context context;
    protected final MultiProfile.UserProfile profile;
    private final PublishListener<T> listener;
    private final Handler handler;

    BaseTester(@NotNull Context context, MultiProfile.UserProfile profile, @Nullable PublishListener<T> listener) {
        this.context = context.getApplicationContext();
        this.handler = new Handler(Looper.getMainLooper());
        this.profile = profile;
        this.listener = listener;
    }

    @Override
    public void run() {
        start(null);
    }

    final void publishMessage(String message, Level level) {
        if (listener == null) return;
        handler.post(() -> listener.publishGeneralMessage(message, level.color));
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

    public enum Level {
        INFO(Color.TERTIARY),
        ERROR(Color.RED),
        SUCCESS(Color.GREEN);

        private final Color color;

        Level(Color color) {
            this.color = color;
        }
    }

    public enum Color {
        PRIMARY(android.R.attr.textColorPrimary, true),
        TERTIARY(android.R.attr.textColorTertiary, true),
        GREEN(R.color.green, false),
        RED(R.color.red, false);

        private final int res;
        private final boolean resolve;

        Color(int res, boolean resolve) {
            this.res = res;
            this.resolve = resolve;
        }

        @ColorInt
        public int getColor(@NonNull Context context) {
            if (resolve) return CommonUtils.resolveAttrAsColor(context, res);
            else return ContextCompat.getColor(context, res);
        }
    }

    public interface PublishListener<T> {
        @UiThread
        void startedNewTest(@NonNull BaseTester tester);

        @UiThread
        void publishGeneralMessage(@NonNull String message, @NonNull Color color);

        @UiThread
        void endedTest(@NonNull BaseTester tester, @Nullable T result);
    }
}
