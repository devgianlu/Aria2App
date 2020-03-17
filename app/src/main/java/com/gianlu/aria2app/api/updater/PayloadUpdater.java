package com.gianlu.aria2app.api.updater;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.aria2app.PK;
import com.gianlu.aria2app.api.ErrorHandler;
import com.gianlu.aria2app.api.aria2.Aria2Helper;
import com.gianlu.commonutils.preferences.Prefs;

public abstract class PayloadUpdater<P> implements Runnable {
    protected final Aria2Helper helper;
    protected final OnPayload<P> listener;
    private final Handler handler;
    private final int updateInterval;
    private final ErrorHandler errorHandler;
    private OnStop stopListener;
    private volatile boolean shouldStop = false;
    private volatile boolean running = false;

    public PayloadUpdater(Context context, OnPayload<P> listener) throws Aria2Helper.InitializingException {
        this.helper = Aria2Helper.instantiate(context);
        this.listener = listener;
        this.handler = new Handler(Looper.getMainLooper());
        this.errorHandler = ErrorHandler.get();
        this.updateInterval = Prefs.getInt(PK.A2_UPDATE_INTERVAL) * 1000;
    }

    protected abstract void loop();

    protected final void errorOccurred(Exception ex) {
        if (!listener.onException(ex)) errorHandler.notifyException(ex, false);
    }

    protected final void hasResult(P result) {
        listener.onPayload(result);
    }

    public final boolean isRunning() {
        return running;
    }

    @Override
    public final void run() {
        shouldStop = false;

        while (!shouldStop) {
            running = true;
            loop();

            try {
                Thread.sleep(updateInterval);
            } catch (InterruptedException ex) {
                shouldStop = true;
            }
        }

        shouldStop = false;
        running = false;

        handler.post(() -> {
            if (stopListener != null) stopListener.onStopped();
        });
    }

    /**
     * @return true if the updater is running; false otherwise
     */
    public final boolean safeStop(@Nullable OnStop listener) {
        if (isRunning()) {
            stopListener = listener;
            shouldStop = true;
            return true;
        } else {
            if (listener != null) listener.onStopped();
            return false;
        }
    }

    @NonNull
    public Aria2Helper getHelper() {
        return helper;
    }

    public interface OnStop {
        void onStopped();
    }

    public interface OnPayload<P> {
        void onPayload(@NonNull P payload);

        /**
         * @return Whether the exception has been handled by the addListener.
         * If it hasn't been handled it will be reported to the {@link ErrorHandler}.
         */
        boolean onException(@NonNull Exception ex);
    }
}
