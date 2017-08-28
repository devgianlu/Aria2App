package com.gianlu.aria2app.NetIO;

import android.support.annotation.Nullable;

import com.gianlu.aria2app.BuildConfig;

import java.util.Timer;
import java.util.TimerTask;

public class ErrorHandler {
    private static ErrorHandler instance;
    private final IErrorHandler handler;
    private int errorCount = 0;
    private volatile boolean locked = false;

    private ErrorHandler(int updateInterval, @Nullable IErrorHandler handler) {
        this.handler = handler;

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                errorCount = 0;
            }
        }, 0, (updateInterval + 1000) * 3);
    }

    public static ErrorHandler get() {
        return setup(0, null);
    }

    public static ErrorHandler setup(int updateInterval, IErrorHandler handler) {
        if (instance == null) instance = new ErrorHandler(updateInterval, handler);
        return instance;
    }

    private void lock() {
        locked = true;
    }

    public void unlock() {
        locked = false;
    }

    public void notifyException(Throwable ex, boolean fatal) {
        if (locked) return;
        errorCount++;

        if (fatal) {
            lock();
            if (handler != null) handler.onFatal(ex);

        } else if (errorCount >= 5) {
            lock();
            if (handler != null) handler.onSubsequentExceptions();
        } else {
            if (handler != null) handler.onException(ex);
        }

        if (BuildConfig.DEBUG) ex.printStackTrace();
    }

    public interface IErrorHandler {
        void onFatal(Throwable ex);

        void onSubsequentExceptions();

        void onException(Throwable ex);
    }
}
