package com.gianlu.aria2app.NetIO;

public interface OnConnect {
    /**
     * @return Whether we want a ping test
     */
    boolean onConnected(AbstractClient client);

    void onPingTested(AbstractClient client, long latency);

    void onFailedConnecting(Throwable ex);
}