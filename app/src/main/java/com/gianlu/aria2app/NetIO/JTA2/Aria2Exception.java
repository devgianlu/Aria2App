package com.gianlu.aria2app.NetIO.JTA2;

public class Aria2Exception extends Exception {
    private final int code;

    Aria2Exception(String detailMessage, int code) {
        super(detailMessage);
        this.code = code;
    }

    @Override
    public String toString() {
        return "Aria2Exception #" + code + ": " + getMessage();
    }
}
