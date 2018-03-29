package com.gianlu.aria2app.ProfilesManager.Testers;

import android.content.Context;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.NetUtils;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
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
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

public class WebSocketTester extends NetTester implements WebSocketListener, Callable<Boolean> {
    private final AtomicBoolean returnValue = new AtomicBoolean();
    private long pingTime;

    WebSocketTester(Context context, MultiProfile.UserProfile profile, IPublish listener) {
        super(context, profile, listener);
    }

    public WebSocketTester(Context context, MultiProfile.UserProfile profile, IProfileTester profileListener) {
        super(context, profile, profileListener);
    }

    @Override
    public Boolean call() {
        try {
            WebSocket webSocket;
            if (profile.authMethod.equals(AbstractClient.AuthMethod.HTTP) && profile.serverUsername != null && profile.serverPassword != null)
                webSocket = NetUtils.readyWebSocket(profile);
            else
                webSocket = NetUtils.readyWebSocket(profile);

            webSocket.addListener(this).connectAsynchronously();

            synchronized (returnValue) {
                returnValue.wait();
                return returnValue.get();
            }
        } catch (IOException | NetUtils.InvalidUrlException | NoSuchAlgorithmException | CertificateException | KeyStoreException | KeyManagementException ex) {
            publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.ERROR, ex));
            return false;
        } catch (InterruptedException ignored) {
            return false;
        }
    }

    @Override
    public String describe() {
        return "WebSocket connection test";
    }

    @Override
    public void onStateChanged(WebSocket websocket, WebSocketState newState) {
        publishMessage("State changed to " + newState.name().toLowerCase(), android.R.color.tertiary_text_light);
    }

    @Override
    public void onConnected(WebSocket websocket, Map<String, List<String>> headers) {
        publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.ONLINE, null));

        pingTime = System.currentTimeMillis();
        websocket.sendPing();
    }

    @Override
    public void onPongFrame(WebSocket websocket, WebSocketFrame frame) {
        publishPing(profile, System.currentTimeMillis() - pingTime);
        notifyReturnValue(true);
    }

    private void notifyReturnValue(boolean value) {
        synchronized (returnValue) {
            returnValue.set(value);
            returnValue.notify();
        }
    }

    @Override
    public void onTextMessage(WebSocket websocket, String text) {
    }

    @Override
    public void onBinaryMessage(WebSocket websocket, byte[] binary) {
    }

    @Override
    public void onSendingFrame(WebSocket websocket, WebSocketFrame frame) {
    }

    @Override
    public void onFrameSent(WebSocket websocket, WebSocketFrame frame) {
    }

    @Override
    public void onFrameUnsent(WebSocket websocket, WebSocketFrame frame) {
    }

    @Override
    public void onThreadCreated(WebSocket websocket, ThreadType threadType, Thread thread) {
    }

    @Override
    public void onThreadStarted(WebSocket websocket, ThreadType threadType, Thread thread) {
    }

    @Override
    public void onThreadStopping(WebSocket websocket, ThreadType threadType, Thread thread) {
    }

    @Override
    public void onError(WebSocket websocket, WebSocketException cause) {
    }

    @Override
    public void onFrameError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) {
    }

    @Override
    public void onMessageError(WebSocket websocket, WebSocketException cause, List<WebSocketFrame> frames) {
    }

    @Override
    public void onMessageDecompressionError(WebSocket websocket, WebSocketException cause, byte[] compressed) {
    }

    @Override
    public void onTextMessageError(WebSocket websocket, WebSocketException cause, byte[] data) {
    }

    @Override
    public void onSendError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) {
    }

    @Override
    public void onUnexpectedError(WebSocket websocket, WebSocketException cause) {
        publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.ERROR, cause));
        notifyReturnValue(false);
    }

    @Override
    public void handleCallbackError(WebSocket websocket, Throwable cause) {
    }

    @Override
    public void onSendingHandshake(WebSocket websocket, String requestLine, List<String[]> headers) {
    }

    @Override
    public void onConnectError(WebSocket websocket, WebSocketException exception) {
        Throwable cause = exception.getCause();

        if (cause instanceof ConnectException || cause instanceof SocketTimeoutException)
            publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.OFFLINE, cause));
        else
            publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.ERROR, cause));

        notifyReturnValue(false);
    }

    @Override
    public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) {
        if (closedByServer)
            publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.ERROR, null));
        else
            publishResult(profile, new MultiProfile.TestStatus(MultiProfile.Status.OFFLINE, null));

        notifyReturnValue(false);
    }

    @Override
    public void onFrame(WebSocket websocket, WebSocketFrame frame) {
    }

    @Override
    public void onContinuationFrame(WebSocket websocket, WebSocketFrame frame) {
    }

    @Override
    public void onTextFrame(WebSocket websocket, WebSocketFrame frame) {
    }

    @Override
    public void onBinaryFrame(WebSocket websocket, WebSocketFrame frame) {
    }

    @Override
    public void onCloseFrame(WebSocket websocket, WebSocketFrame frame) {
    }

    @Override
    public void onPingFrame(WebSocket websocket, WebSocketFrame frame) {
    }
}
