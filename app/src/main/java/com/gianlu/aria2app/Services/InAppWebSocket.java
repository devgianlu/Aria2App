package com.gianlu.aria2app.Services;

import android.app.Activity;

import com.gianlu.aria2app.Utils;
import com.neovisionaries.ws.client.WebSocket;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class InAppWebSocket {
    private WebSocket socket;

    public InAppWebSocket(Activity context) {
        try {
            socket = Utils.readyWebSocket(context);
        } catch (IOException | NoSuchAlgorithmException ex) {
            Utils.UIToast(context, Utils.TOAST_MESSAGES.WS_EXCEPTION, ex);
        }
    }

    public void connect(InAppAdapter adapter) {
        socket.addListener(adapter)
                .connectAsynchronously();
    }
}
