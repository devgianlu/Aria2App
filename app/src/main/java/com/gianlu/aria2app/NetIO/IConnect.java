package com.gianlu.aria2app.NetIO;

public interface IConnect {
    void onConnected(AbstractClient client);

    void onFailedConnecting(Exception ex);
}