package com.gianlu.aria2app.api;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public final class ErrorHandler {
    private static ErrorHandler instance;
    private final Listener handler;
    private final AtomicInteger errorCount = new AtomicInteger(0);
    private volatile boolean locked = false;

    private ErrorHandler(int updateInterval, @Nullable Listener handler) {
        this.handler = handler;

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                errorCount.set(0);
            }
        }, 0, (updateInterval + 1000) * 3);
    }

    public static ErrorHandler get() {
        return setup(0, null);
    }

    public static ErrorHandler setup(int updateInterval, Listener handler) {
        if (instance == null) instance = new ErrorHandler(updateInterval, handler);
        return instance;
    }

    private void lock() {
        locked = true;
    }

    public void unlock() {
        errorCount.set(0);
        locked = false;
    }

    private static final String TAG = ErrorHandler.class.getSimpleName();

    public void notifyException(Throwable ex, boolean fatal) {
        if (locked) return;
        errorCount.incrementAndGet();

        if (fatal) {
            lock();
            if (handler != null) handler.onFatal(ex);
        } else if (errorCount.get() >= 5) {
            lock();
            if (handler != null) handler.onSubsequentExceptions();
        } else {
            if (handler != null) handler.onException(ex);
        }

        Log.e(TAG, "Exception encountered, fatal: " + fatal, ex);
    }

    public interface Listener {
        void onFatal(@NonNull Throwable ex);

        void onSubsequentExceptions();

        void onException(@NonNull Throwable ex);
    }
}
