package com.gianlu.aria2app.ProfilesManager.Testers;

import android.content.Context;

import com.gianlu.aria2app.NetIO.IConnect;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.NetUtils;
import com.gianlu.aria2app.NetIO.WebSocketing;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.commonutils.Logging;
import com.neovisionaries.ws.client.ThreadType;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketListener;
import com.neovisionaries.ws.client.WebSocketState;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Map;

public class WsProfileTester extends NetProfileTester implements WebSocketListener {
    private long pingTime;

    public WsProfileTester(Context context, MultiProfile.UserProfile profile, ITesting listener) {
        super(context, profile, listener);
    }

    @Override
    public void getClient(IConnect listener) {
        try {
            new WebSocketing(context, profile, listener);
        } catch (CertificateException | KeyManagementException | NoSuchAlgorithmException | KeyStoreException | IOException ex) {
            listener.onFailedConnecting(ex);
            Logging.logMe(context, ex);
        }
    }

    @Override
    public void run() {
        publishUpdate("Started connection test...");

        try {
            WebSocket webSocket;
            if (profile.authMethod.equals(JTA2.AuthMethod.HTTP) && profile.serverUsername != null && profile.serverPassword != null)
                webSocket = NetUtils.readyWebSocket(profile.buildWebSocketUrl(), profile.serverUsername, profile.serverPassword, NetUtils.readyCertificate(context, profile));
            else
                webSocket = NetUtils.readyWebSocket(profile.buildWebSocketUrl(), NetUtils.readyCertificate(context, profile));

            webSocket.addListener(this).connectAsynchronously();
        } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException | KeyManagementException ex) {
            publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.ERROR));
            publishUpdate(ex.getMessage());
        }
    }

    @Override
    public void onStateChanged(WebSocket websocket, WebSocketState newState) throws Exception {
        publishUpdate("State changed to " + newState.name().toLowerCase() + ".");
    }

    @Override
    public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
        publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.ONLINE));

        pingTime = System.currentTimeMillis();
        websocket.sendPing();

        publishUpdate("Sent ping.");
    }

    @Override
    public void onPongFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
        publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.ONLINE, System.currentTimeMillis() - pingTime));
        publishUpdate("Ping successful after " + (System.currentTimeMillis() - pingTime) + "ms.");
    }

    @Override
    public void onTextMessage(WebSocket websocket, String text) throws Exception {
    }

    @Override
    public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception {
    }

    @Override
    public void onSendingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
    }

    @Override
    public void onFrameSent(WebSocket websocket, WebSocketFrame frame) throws Exception {
    }

    @Override
    public void onFrameUnsent(WebSocket websocket, WebSocketFrame frame) throws Exception {
    }

    @Override
    public void onThreadCreated(WebSocket websocket, ThreadType threadType, Thread thread) throws Exception {
    }

    @Override
    public void onThreadStarted(WebSocket websocket, ThreadType threadType, Thread thread) throws Exception {
    }

    @Override
    public void onThreadStopping(WebSocket websocket, ThreadType threadType, Thread thread) throws Exception {
    }

    @Override
    public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
    }

    @Override
    public void onFrameError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {
    }

    @Override
    public void onMessageError(WebSocket websocket, WebSocketException cause, List<WebSocketFrame> frames) throws Exception {
    }

    @Override
    public void onMessageDecompressionError(WebSocket websocket, WebSocketException cause, byte[] compressed) throws Exception {
    }

    @Override
    public void onTextMessageError(WebSocket websocket, WebSocketException cause, byte[] data) throws Exception {
    }

    @Override
    public void onSendError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {
    }

    @Override
    public void onUnexpectedError(WebSocket websocket, WebSocketException cause) throws Exception {
        publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.ERROR));
        publishUpdate(cause.getMessage());
    }

    @Override
    public void handleCallbackError(WebSocket websocket, Throwable cause) throws Exception {
    }

    @Override
    public void onSendingHandshake(WebSocket websocket, String requestLine, List<String[]> headers) throws Exception {
    }

    @Override
    public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
        if (exception.getCause() instanceof ConnectException)
            publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.OFFLINE));
        else if (exception.getCause() instanceof SocketTimeoutException)
            publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.OFFLINE));
        else
            publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.ERROR));

        publishUpdate(exception.getMessage());
    }

    @Override
    public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
        if (closedByServer)
            publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.ERROR));
        else publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.OFFLINE));
        publishUpdate("Connection closed by " + (closedByServer ? "server" : "client" + "."));
    }

    @Override
    public void onFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
    }

    @Override
    public void onContinuationFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
    }

    @Override
    public void onTextFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
    }

    @Override
    public void onBinaryFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
    }

    @Override
    public void onCloseFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
    }

    @Override
    public void onPingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
    }
}
