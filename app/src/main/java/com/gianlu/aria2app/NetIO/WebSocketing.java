package com.gianlu.aria2app.NetIO;

import android.app.Activity;
import android.util.ArrayMap;

import com.gianlu.aria2app.MainActivity;
import com.gianlu.aria2app.Utils;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFrame;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

public class WebSocketing extends WebSocketAdapter {
    private static WebSocketing webSocketing;
    private WebSocket socket;
    private Activity context;

    private Map<Integer, IReceived> requests = new ArrayMap<>();

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
        try {
            requests.put(request.getInt("id"), handler);
            socket.sendText(request.toString());
        } catch (JSONException ex) {
            handler.onException(ex);
        }
    }

    @Override
    public void onTextMessage(WebSocket websocket, String text) throws Exception {
        JSONObject response = new JSONObject(text);

        IReceived handler = requests.remove(response.getInt("id"));
        if (handler == null) return;
        if (response.isNull("error")) {
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
    public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
        Utils.UIToast(context, Utils.TOAST_MESSAGES.WS_EXCEPTION, exception);
    }

    @Override
    public void handleCallbackError(WebSocket websocket, Throwable cause) throws Exception {
        if (cause instanceof JSONException) {
            // TODO: Bla bla bla
        }
    }

    @Override
    public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
        Utils.UIToast(context, Utils.TOAST_MESSAGES.WS_CLOSED, cause);
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

        void onException(Exception ex);

        void onException(int code, String reason);
    }
}
