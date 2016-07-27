package com.gianlu.aria2app.NetIO.JTA2;

public class Aria2Exception extends Exception {
    private int code;

    public Aria2Exception(String detailMessage, int code) {
        super(detailMessage);
        this.code = code;
    }

    @Override
    public String toString() {
        return "Aria2Exception #" + code + ": " + getMessage();
    }
}
