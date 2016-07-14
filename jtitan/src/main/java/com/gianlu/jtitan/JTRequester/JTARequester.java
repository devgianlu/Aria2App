package com.gianlu.jtitan.JTRequester;

import java.io.IOException;

public class JTARequester implements Runnable {
    String url;
    String req;
    JTHandler handler;

    public JTARequester(String url, String request, JTHandler handler) {
        this.url = url;
        this.handler = handler;
        req = request;
    }

    @Override
    public void run() {
        try {
            JTResponse resp = new JTRequester(url).send(req);
            handler.onReceive(resp);
        } catch (IOException ex) {
            handler.onException(ex);
        }
    }
}
