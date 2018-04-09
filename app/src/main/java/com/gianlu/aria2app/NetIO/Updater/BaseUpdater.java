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

public abstract class BaseUpdater<P> extends Thread {
    protected final Aria2Helper helper;
    protected final UpdaterListener<P> listener;
    private final Handler handler;
    private final int updateInterval;
    private final ErrorHandler errorHandler;
    private OnStop stopListener;
    private volatile boolean _shouldStop = false;

    public BaseUpdater(Context context, UpdaterListener<P> listener) throws Aria2Helper.InitializingException {
        this.helper = Aria2Helper.instantiate(context);
        this.listener = listener;
        this.handler = new Handler(Looper.getMainLooper());
        this.errorHandler = ErrorHandler.get();
        this.updateInterval = Prefs.getFakeInt(context, PKeys.A2_UPDATE_INTERVAL, 1) * 1000;
    }

    protected void errorOccurred(Exception ex) {
        errorHandler.notifyException(ex, false);
    }

    protected void hasResult(P result) {
        listener.onUpdateUi(result);
    }

    @Override
    public void run() {
        while (!_shouldStop) {
            loop();

            try {
                Thread.sleep(updateInterval);
            } catch (InterruptedException ex) {
                Logging.log(ex);
                _shouldStop = true;
            }
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (stopListener != null) stopListener.onStopped();
            }
        });
    }

    protected abstract void loop();

    public final void safeStop(@Nullable OnStop listener) {
        stopListener = listener;
        _shouldStop = true;
    }

    public interface OnStop {
        void onStopped();
    }

    public interface UpdaterListener<P> {
        void onUpdateUi(@NonNull P payload);
    }
}
