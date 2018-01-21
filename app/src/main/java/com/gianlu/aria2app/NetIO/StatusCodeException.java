package com.gianlu.aria2app.NetIO;

import okhttp3.Response;

public class StatusCodeException extends Exception {
    public final int code;

    public StatusCodeException(int code, String message) {
        super(code + ": " + message);
        this.code = code;
    }

    public StatusCodeException(Response resp) {
        this(resp.code(), resp.message());
    }
}
