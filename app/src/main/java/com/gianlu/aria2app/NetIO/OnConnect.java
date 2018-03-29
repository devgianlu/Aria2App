package com.gianlu.aria2app.NetIO;

public interface OnConnect {
    void onConnected(AbstractClient client);

    void onFailedConnecting(Throwable ex);
}