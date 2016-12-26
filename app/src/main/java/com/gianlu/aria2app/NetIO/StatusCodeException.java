package com.gianlu.aria2app.NetIO;

class StatusCodeException extends Exception {
    public StatusCodeException(int code, String message) {
        super(code + ": " + message);
    }
}
