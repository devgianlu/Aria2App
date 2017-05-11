package com.gianlu.aria2app.ProfilesManager;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.View;

import com.gianlu.aria2app.NetIO.HTTPing;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.WebSocketing;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.Drawer.ProfilesAdapter;
import com.gianlu.commonutils.Logging;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFrame;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CustomProfilesAdapter extends ProfilesAdapter<UserProfile> {
    public CustomProfilesAdapter(Context context, List<UserProfile> profiles, IAdapter<UserProfile> listener) {
        super(context, profiles, R.drawable.ripple_effect_dark, R.color.colorAccent, listener);
    }

    @Override
    public UserProfile getItem(int pos) {
        return profiles.get(pos);
    }

    @Override
    public void onBindViewHolder(ProfilesAdapter.ViewHolder holder, int position) {
        final UserProfile profile = getItem(position);

        holder.globalName.setVisibility(View.GONE);
        holder.name.setText(profile.getProfileName());
        holder.secondary.setText(profile.getFullServerAddress());

        if (profile.latency != -1) {
            holder.ping.setVisibility(View.VISIBLE);
            holder.ping.setText(String.format(Locale.getDefault(), "%s ms", profile.latency));
        } else {
            holder.ping.setVisibility(View.GONE);
        }

        if (profile.status == BaseProfile.Status.UNKNOWN) {
            holder.loading.setVisibility(View.VISIBLE);
            holder.status.setVisibility(View.GONE);
        } else {
            holder.loading.setVisibility(View.GONE);
            holder.status.setVisibility(View.VISIBLE);

            switch (profile.status) {
                case ONLINE:
                    holder.status.setImageResource(R.drawable.ic_done_black_48dp);
                    break;
                case OFFLINE:
                    holder.status.setImageResource(R.drawable.ic_clear_black_48dp);
                    break;
                case ERROR:
                    holder.status.setImageResource(R.drawable.ic_error_black_48dp);
                    break;
            }
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) listener.onProfileSelected(profile);
            }
        });
    }

    @Override
    protected void runTest(int pos, final IFinished handler) {
        final UserProfile profile = getItem(pos);

        switch (profile.connectionMethod) {
            case HTTP:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            HttpURLConnection conn;
                            if (profile.authMethod.equals(JTA2.AuthMethod.HTTP))
                                conn = HTTPing.readyHttpConnection(profile.buildHttpUrl(), profile.serverUsername, profile.serverPassword, Utils.readyCertificate(context, profile));
                            else
                                conn = HTTPing.readyHttpConnection(profile.buildHttpUrl(), Utils.readyCertificate(context, profile));

                            long start = System.currentTimeMillis();
                            conn.connect();

                            if (conn.getResponseCode() == 400) {
                                profile.setStatus(BaseProfile.Status.ONLINE);
                                profile.setLatency(System.currentTimeMillis() - start);

                                new Handler(context.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        notifyDataSetChanged();
                                    }
                                });

                                if (handler != null) handler.onFinished();
                            } else {
                                profile.setStatus(BaseProfile.Status.OFFLINE);

                                new Handler(context.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        notifyDataSetChanged();
                                    }
                                });

                                if (handler != null) handler.onFinished();
                            }
                        } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException ex) {
                            profile.setStatus(BaseProfile.Status.ERROR);

                            new Handler(context.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    notifyDataSetChanged();
                                }
                            });

                            if (handler != null) handler.onFinished();

                            Logging.logMe(context, ex);
                        }
                    }
                }).start();
                break;
            case WEBSOCKET:
                try {
                    WebSocket webSocket;
                    if (profile.authMethod.equals(JTA2.AuthMethod.HTTP))
                        webSocket = WebSocketing.readyWebSocket(profile.buildWebSocketUrl(), profile.serverUsername, profile.serverPassword, Utils.readyCertificate(context, profile));
                    else
                        webSocket = WebSocketing.readyWebSocket(profile.buildWebSocketUrl(), Utils.readyCertificate(context, profile));

                    webSocket.addListener(new StatusWebSocketHandler(profile, handler)).connectAsynchronously();
                } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException | KeyManagementException ex) {
                    profile.setStatus(BaseProfile.Status.ERROR);

                    new Handler(context.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            notifyDataSetChanged();
                        }
                    });

                    if (handler != null) handler.onFinished();

                    Logging.logMe(context, ex);
                }
                break;
        }
    }

    private class StatusWebSocketHandler extends WebSocketAdapter {
        private final UserProfile profile;
        private final IFinished handler;
        private long startTime;

        StatusWebSocketHandler(UserProfile profile, @Nullable IFinished handler) {
            this.profile = profile;
            this.handler = handler;
        }

        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
            profile.setStatus(BaseProfile.Status.ONLINE);

            new Handler(context.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });

            if (handler != null) handler.onFinished();

            startTime = System.currentTimeMillis();
            websocket.sendPing();
        }

        @Override
        public void onPongFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            profile.setLatency(System.currentTimeMillis() - startTime);

            new Handler(context.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
            profile.setStatus(BaseProfile.Status.OFFLINE);

            new Handler(context.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });

            if (handler != null) handler.onFinished();
        }

        @Override
        public void onUnexpectedError(WebSocket websocket, WebSocketException cause) throws Exception {
            profile.setStatus(BaseProfile.Status.ERROR);

            new Handler(context.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });

            if (handler != null) handler.onFinished();
        }

        @Override
        public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
            if (exception.getCause() instanceof ConnectException)
                profile.setStatus(BaseProfile.Status.OFFLINE);
            else if (exception.getCause() instanceof SocketTimeoutException)
                profile.setStatus(BaseProfile.Status.OFFLINE);
            else
                profile.setStatus(BaseProfile.Status.ERROR);

            new Handler(context.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });

            if (handler != null) handler.onFinished();
        }

        @Override
        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
            if (closedByServer) profile.setStatus(BaseProfile.Status.ERROR);
            else profile.setStatus(BaseProfile.Status.OFFLINE);

            new Handler(context.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });

            if (handler != null) handler.onFinished();
        }
    }
}
