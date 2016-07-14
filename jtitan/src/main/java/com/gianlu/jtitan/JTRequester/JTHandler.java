package com.gianlu.jtitan.JTRequester;

public interface JTHandler {
    void onReceive(JTResponse response);

    void onException(Exception exception);
}
