package com.gianlu.aria2app.NetIO.Updater;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.ErrorHandler;
import com.gianlu.aria2app.PKeys;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Preferences.Prefs;

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
        this.updateInterval = Prefs.getFakeInt(context, PKeys.A2_UPDATE_INTERVAL, 1) * 1000;
    }

    protected abstract void loop();

    protected final void errorOccurred(Exception ex) {
        errorHandler.notifyException(ex, false);
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
                Logging.log(ex);
                shouldStop = true;
            }
        }

        shouldStop = false;
        running = false;

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (stopListener != null) stopListener.onStopped();
            }
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

    public interface OnStop {
        void onStopped();
    }

    public interface OnPayload<P> {
        void onPayload(@NonNull P payload);
    }
}
