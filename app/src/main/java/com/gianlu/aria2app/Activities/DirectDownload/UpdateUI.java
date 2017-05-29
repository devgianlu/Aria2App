package com.gianlu.aria2app.Activities.DirectDownload;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.NetIO.BaseUpdater;
import com.gianlu.aria2app.NetIO.ErrorHandler;
import com.gianlu.aria2app.Prefs;

public class UpdateUI extends Thread {
    protected final Handler handler;
    private final int updateInterval;
    private final IUI listener;
    private BaseUpdater.IThread stopListener;
    private boolean _shouldStop = false;

    public UpdateUI(Context context, IUI listener) {
        this.handler = new Handler(context.getMainLooper());
        this.updateInterval = Prefs.getFakeInt(context, Prefs.Keys.A2_UPDATE_INTERVAL, 1) * 1000;
        this.listener = listener;
    }

    @Override
    public void run() {
        while (!_shouldStop) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null) listener.onRefresh();
                }
            });

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

    public final void stopThread(@Nullable BaseUpdater.IThread listener) {
        stopListener = listener;
        _shouldStop = true;
    }

    public interface IUI {
        void onRefresh();
    }
}
