package com.gianlu.aria2app.NetIO;

import cz.msebera.android.httpclient.StatusLine;

public class StatusCodeException extends Exception {
    public final int code;

    public StatusCodeException(int code, String message) {
        super(code + ": " + message);
        this.code = code;
    }

    public StatusCodeException(StatusLine sl) {
        this(sl.getStatusCode(), sl.getReasonPhrase());
    }
}
