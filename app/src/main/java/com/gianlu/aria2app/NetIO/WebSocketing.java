package com.gianlu.aria2app.NetIO;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.NetIO.JTA2.AriaException;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketState;

import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketing extends AbstractClient {
    private static WebSocketing webSocketing;
    private static boolean locked = false;
    private final Map<Integer, WeakReference<IReceived>> requests = new ConcurrentHashMap<>();
    private IConnect connectionListener;
    private WebSocket socket;

    private WebSocketing(Context context) throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException, KeyManagementException, ProfilesManager.NoCurrentProfileException {
        super(context, ProfilesManager.get(context).getCurrent(context).getProfile(context));
        socket = NetUtils.readyWebSocket(profile).addListener(new Adapter()).connectAsynchronously();
    }

    private WebSocketing(Context context, @Nullable IConnect listener) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException, ProfilesManager.NoCurrentProfileException {
        this(context);
        this.connectionListener = listener;
    }

    private WebSocketing(Context context, MultiProfile.UserProfile profile, @Nullable IConnect listener) throws CertificateException, IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        super(context, profile);
        socket = NetUtils.readyWebSocket(profile.buildWebSocketUrl(), profile.hostnameVerifier, profile.serverUsername, profile.serverPassword, profile.certificate);
        socket.addListener(new Adapter()).connectAsynchronously();
        connectionListener = listener;
    }

    public static WebSocketing instantiate(Context context) throws InitializationException {
        if (webSocketing == null) {
            try {
                webSocketing = new WebSocketing(context);
            } catch (ProfilesManager.NoCurrentProfileException | IOException | NoSuchAlgorithmException | KeyStoreException | CertificateException | KeyManagementException ex) {
                throw new InitializationException(ex);
            }
        }

        return webSocketing;
    }

    public static void instantiate(Context context, MultiProfile.UserProfile profile, @NonNull IConnect listener) {
        try {
            unlock();
            webSocketing = new WebSocketing(context, profile, listener);
        } catch (CertificateException | NoSuchAlgorithmException | KeyManagementException | KeyStoreException | IOException ex) {
            listener.onFailedConnecting(ex);
        }
    }

    public static void clear() {
        locked = true;
        clearConnectivityListener();
        if (webSocketing != null) {
            webSocketing.clearInternal();
            webSocketing = null;
        }
    }

    private static void unlock() {
        locked = false;
        ErrorHandler.get().unlock();
    }

    @Override
    protected void clearInternal() {
        connectionListener = null;
        if (socket != null) socket.disconnect();
        if (requests != null) requests.clear();
    }

    @Override
    public void send(JSONObject request, IReceived listener) {
        if (locked) return;
        if (requests.size() > 10) requests.clear();
        if (socket.getState() != WebSocketState.OPEN) return;

        try {
            requests.put(request.getInt("id"), new WeakReference<>(listener));
            socket.sendText(request.toString());
        } catch (Exception ex) {
            listener.onException(ex);
        }
    }

    @Override
    public void connectivityChanged(@NonNull Context context, @NonNull MultiProfile.UserProfile profile) throws Exception {
        webSocketing = new WebSocketing(context, profile, null);
    }

    private class Adapter extends WebSocketAdapter {
        @Override
        public void onTextMessage(WebSocket websocket, String text) throws Exception {
            if (locked) return;

            JSONObject response = new JSONObject(text);

            String method = response.optString("method", null);
            if (method != null && method.startsWith("aria2.on")) return;

            WeakReference<IReceived> ref = requests.remove(response.getInt("id"));
            if (ref.get() == null) return;
            IReceived listener = ref.get();
            if (listener == null) return;
            if (response.isNull("error")) listener.onResponse(response);
            else listener.onException(new AriaException(response.getJSONObject("error")));
        }

        @Override
        public void handleCallbackError(WebSocket websocket, Throwable cause) throws Exception {
            if (locked) return;
            ErrorHandler.get().notifyException(cause, false);
        }

        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
            if (connectionListener != null) {
                connectionListener.onConnected(WebSocketing.this);
                connectionListener = null;
            }
        }

        @Override
        public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
            if (connectionListener != null) {
                connectionListener.onFailedConnecting(exception);
                connectionListener = null;
            }
        }
    }
}
