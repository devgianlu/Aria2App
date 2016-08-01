package com.gianlu.aria2app.NetIO;

import android.app.Activity;
import android.util.ArrayMap;
import android.util.Pair;

import com.gianlu.aria2app.MainActivity;
import com.gianlu.aria2app.Utils;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketState;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WebSocketing extends WebSocketAdapter {
    private static WebSocketing webSocketing;
    private WebSocket socket;
    private Activity context;
    private Map<Integer, IReceived> requests = new ArrayMap<>();
    private List<Pair<JSONObject, IReceived>> connectionQueue = new ArrayList<>();

    private WebSocketing(Activity context) throws IOException, NoSuchAlgorithmException {
        this.context = context;
        socket = Utils.readyWebSocket(context)
                .addListener(this)
                .connectAsynchronously();
    }

    public static WebSocketing newInstance(Activity context) throws IOException, NoSuchAlgorithmException {
        if (webSocketing == null) webSocketing = new WebSocketing(context);
        return webSocketing;
    }

    public static void enableEventManager(final MainActivity mainActivity) throws IOException, NoSuchAlgorithmException {
        if (mainActivity == null) return;

        newInstance(mainActivity).socket.addListener(new WebSocketAdapter() {
            @Override
            public void onFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
                if (new JSONObject(frame.getPayloadText()).getString("method").startsWith("aria2.on"))
                    mainActivity.reloadPage();
            }
        });
    }

    public void send(JSONObject request, IReceived handler) {
        if (socket.getState() == WebSocketState.CONNECTING) {
            connectionQueue.add(new Pair<>(request, handler));
            handler.onException(true, new Exception("WebSocket is connecting! Requests queued."));
            return;
        } else if (socket.getState() != WebSocketState.OPEN) {
            handler.onException(false, new Exception("WebSocket not open! State: " + socket.getState().name()));
            return;
        }

        try {
            requests.put(request.getInt("id"), handler);
            socket.sendText(request.toString());
        } catch (JSONException ex) {
            handler.onException(false, ex);
        }
    }

    private void processQueue() {
        for (Pair<JSONObject, IReceived> pair : connectionQueue) {
            send(pair.first, pair.second);
        }
    }

    @Override
    public void onTextMessage(WebSocket websocket, String text) throws Exception {
        JSONObject response = new JSONObject(text);

        String method = response.optString("method");
        if (method != null && method.startsWith("aria2.on")) return;

        IReceived handler = requests.remove(response.getInt("id"));
        if (handler == null) return;
        if (response.isNull("error")) {
            // TODO: Passing invalid JSON?!
            handler.onResponse(response);
            return;
        }

        handler.onException(response.getJSONObject("error").getInt("code"), response.getJSONObject("error").getString("message"));
    }

    @Override
    public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
        Utils.UIToast(context, Utils.TOAST_MESSAGES.WS_OPENED);
    }

    @Override
    public void onStateChanged(WebSocket websocket, WebSocketState newState) throws Exception {
        if (newState.equals(WebSocketState.OPEN) && connectionQueue.size() > 0) processQueue();
    }

    @Override
    public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
        Utils.UIToast(context, Utils.TOAST_MESSAGES.WS_EXCEPTION, cause);
    }

    @Override
    public void handleCallbackError(WebSocket websocket, Throwable cause) throws Exception {
        Utils.UIToast(context, Utils.TOAST_MESSAGES.UNKNOWN_EXCEPTION, cause);
    }

    @Override
    public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
        Utils.UIToast(context, Utils.TOAST_MESSAGES.WS_CLOSED, "Closed by server: " + closedByServer + "\nServer frame: " + serverCloseFrame + "\nClient frame: " + clientCloseFrame);
    }

    public Activity getContext() {
        return context;
    }

    public interface IReceived {
        void onResponse(JSONObject response) throws JSONException;

        void onException(boolean queuing, Exception ex);
        void onException(int code, String reason);
    }
}
