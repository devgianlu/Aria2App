package com.gianlu.aria2app.NetIO;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.JTA2.JTA2InitializingException;
import com.gianlu.aria2app.Prefs;

public abstract class BaseUpdater extends Thread {
    protected final JTA2 jta2;
    protected final Handler handler;
    private final int updateInterval;
    private IThread stopListener;
    private boolean _shouldStop = false;

    public BaseUpdater(Context context) throws JTA2InitializingException {
        this.jta2 = JTA2.instantiate(context);
        this.handler = new Handler(context.getMainLooper());
        this.updateInterval = Prefs.getInt(context, Prefs.Keys.A2_UPDATE_INTERVAL, 1) * 1000;
    }

    @Override
    public void run() {
        while (!_shouldStop) {
            loop();

            try {
                Thread.sleep(updateInterval);
            } catch (InterruptedException ex) {
                _shouldStop = true;
                ErrorHandler.get().notifyException(ex, false);
            }
        }

        if (stopListener != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    stopListener.onStopped();
                }
            });
        }
    }

    public abstract void loop();

    public final void stopThread(@Nullable IThread listener) {
        stopListener = listener;
        _shouldStop = true;
    }

    public final void stopThread() {
        stopThread(null);
    }

    public interface IThread {
        void onStopped();
    }
}
