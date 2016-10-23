package com.gianlu.aria2app.Terminal;

import android.content.Context;

import com.gianlu.aria2app.Utils;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class WebSocketRequester {
    public static WebSocketRequester instance;
    public WebSocket socket;

    private WebSocketRequester(Context context, WebSocketAdapter adapter) throws IOException, NoSuchAlgorithmException {
        socket = Utils.readyWebSocket(context)
                .addListener(adapter)
                .connectAsynchronously();
    }

    public static WebSocketRequester getInstance(Context context, WebSocketAdapter adapter) throws IOException, NoSuchAlgorithmException {
        if (instance == null)
            instance = new WebSocketRequester(context, adapter);
        return instance;
    }

    public static WebSocketRequester getInstance(Context context) throws IOException, NoSuchAlgorithmException {
        return getInstance(context, null);
    }

    public static void destroy() {
        if (instance == null)
            return;

        instance.socket.disconnect();
        instance = null;
    }

    public void request(JSONObject obj) {
        socket.sendText(obj.toString());
    }

    public void sendPing() {
        socket.sendPing(String.valueOf(System.currentTimeMillis()));
    }

    public void request(String id, String jsonrpc, String method, String params) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id)
                .put("jsonrpc", jsonrpc)
                .put("method", method)
                .put("params", new JSONArray("[" + params + "]"));

        request(obj);
    }
}
