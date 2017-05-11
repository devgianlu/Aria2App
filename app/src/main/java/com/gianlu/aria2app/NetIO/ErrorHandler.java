package com.gianlu.aria2app.NetIO;

import android.support.annotation.Nullable;

import java.util.Timer;
import java.util.TimerTask;

public class ErrorHandler {
    private static ErrorHandler instance;
    private final IErrorHandler handler;
    private int errorCount = 0;

    private ErrorHandler(int updateInterval, @Nullable IErrorHandler handler) {
        this.handler = handler;

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                errorCount = 0;
            }
        }, 0, (updateInterval + 1000) * 5);
    }

    public static ErrorHandler get() {
        return setup(-1, null);
    }

    public static ErrorHandler setup(int updateInterval, IErrorHandler handler) {
        if (instance == null) instance = new ErrorHandler(updateInterval, handler);
        return instance;
    }

    public void notifyException(Exception ex, boolean fatal) {
        errorCount++;

        if (fatal) {
            if (handler != null) handler.onFatal(ex);
        } else if (errorCount >= 5) {
            if (handler != null) handler.onSubsequentExceptions();
        }
    }

    public interface IErrorHandler {
        void onFatal(Exception ex);

        void onSubsequentExceptions();
    }
}
