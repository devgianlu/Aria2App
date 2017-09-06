package com.gianlu.aria2app.NetIO;

import cz.msebera.android.httpclient.StatusLine;

public class StatusCodeException extends Exception {
    public StatusCodeException(int code, String message) {
        super(code + ": " + message);
    }

    public StatusCodeException(StatusLine sl) {
        this(sl.getStatusCode(), sl.getReasonPhrase());
    }
}
