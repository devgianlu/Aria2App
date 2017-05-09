package com.gianlu.aria2app.NetIO;

public class ErrorHandler {
    private static ErrorHandler instance;

    private ErrorHandler() {
    }

    public static ErrorHandler get() {
        if (instance == null) instance = new ErrorHandler();
        return instance;
    }

    public void notifyException(Exception ex, boolean fatal) {
        // TODO: Do something
    }
}
