package com.gianlu.aria2app.ProfilesManager;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.View;

import com.gianlu.aria2app.NetIO.HTTPing;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.NetUtils;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.Drawer.ProfilesAdapter;
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
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomProfilesAdapter extends ProfilesAdapter<UserProfile> {
    private final IEdit editListener;
    private final ExecutorService service = Executors.newCachedThreadPool();
    private final Handler handler;

    public CustomProfilesAdapter(Context context, List<UserProfile> profiles, IAdapter<UserProfile> listener, boolean black, @Nullable IEdit editListener) {
        super(context, profiles, R.drawable.ripple_effect_dark, R.color.colorAccent, black, listener);
        this.editListener = editListener;
        this.handler = new Handler(context.getMainLooper());
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

        if (profile.status.latency != -1) {
            holder.ping.setVisibility(View.VISIBLE);
            holder.ping.setText(String.format(Locale.getDefault(), "%s ms", profile.status.latency));
        } else {
            holder.ping.setVisibility(View.GONE);
        }

        if (profile.status.status == BaseProfile.Status.UNKNOWN) {
            holder.loading.setVisibility(View.VISIBLE);
            holder.status.setVisibility(View.GONE);
        } else {
            holder.loading.setVisibility(View.GONE);
            holder.status.setVisibility(View.VISIBLE);

            switch (profile.status.status) {
                case ONLINE:
                    holder.status.setImageResource(black ? R.drawable.ic_done_black_48dp : R.drawable.ic_done_white_48dp);
                    break;
                case OFFLINE:
                    holder.status.setImageResource(black ? R.drawable.ic_clear_black_48dp : R.drawable.ic_clear_white_48dp);
                    break;
                case ERROR:
                    holder.status.setImageResource(black ? R.drawable.ic_error_black_48dp : R.drawable.ic_error_white_48dp);
                    break;
            }
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) listener.onProfileSelected(profile);
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (editListener != null) editListener.onEditProfile(profile);
                return editListener != null;
            }
        });
    }

    private void notifyItemChanged(UserProfile profile, BaseProfile.TestStatus status) {
        profile.setStatus(status);

        final int pos = indexOf(profile);
        if (pos != -1) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    notifyItemChanged(pos);
                }
            });
        }
    }

    private int indexOf(UserProfile match) {
        for (int i = 0; i < profiles.size(); i++) {
            UserProfile profile = profiles.get(i);
            if (Objects.equals(profile.serverAddr, match.serverAddr)
                    && Objects.equals(profile.serverPort, match.serverPort)
                    && Objects.equals(profile.authMethod, match.authMethod))
                return i;
        }

        return -1;
    }

    @Override
    protected void runTest(int pos) {
        final UserProfile profile = getItem(pos);

        switch (profile.connectionMethod) {
            case HTTP:
                service.execute(new HttpProfileTester(profile));
                break;
            default:
            case WEBSOCKET:
                service.execute(new WsProfileTester(profile));
                break;
        }
    }

    public interface IEdit {
        void onEditProfile(UserProfile profile);
    }

    private class HttpProfileTester implements Runnable {
        private final UserProfile profile;

        public HttpProfileTester(UserProfile profile) {
            this.profile = profile;
        }

        @Override
        public void run() {
            try {
                HttpURLConnection conn;
                if (profile.authMethod.equals(JTA2.AuthMethod.HTTP) && profile.serverUsername != null && profile.serverPassword != null)
                    conn = HTTPing.readyHttpConnection(profile.buildHttpUrl(), profile.serverUsername, profile.serverPassword, NetUtils.readyCertificate(context, profile));
                else
                    conn = HTTPing.readyHttpConnection(profile.buildHttpUrl(), NetUtils.readyCertificate(context, profile));

                long start = System.currentTimeMillis();
                conn.connect();

                if (conn.getResponseCode() == 400) {
                    notifyItemChanged(profile, new BaseProfile.TestStatus(BaseProfile.Status.ONLINE, System.currentTimeMillis() - start));
                } else {
                    notifyItemChanged(profile, new BaseProfile.TestStatus(BaseProfile.Status.OFFLINE));
                }
            } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException ex) {
                notifyItemChanged(profile, new BaseProfile.TestStatus(BaseProfile.Status.ERROR));
            }
        }
    }

    private class WsProfileTester extends WebSocketAdapter implements Runnable {
        private final UserProfile profile;
        private long pingTime;

        public WsProfileTester(UserProfile profile) {
            this.profile = profile;
        }

        @Override
        public void run() {
            try {
                WebSocket webSocket;
                if (profile.authMethod.equals(JTA2.AuthMethod.HTTP) && profile.serverUsername != null && profile.serverPassword != null)
                    webSocket = NetUtils.readyWebSocket(profile.buildWebSocketUrl(), profile.serverUsername, profile.serverPassword, NetUtils.readyCertificate(context, profile));
                else
                    webSocket = NetUtils.readyWebSocket(profile.buildWebSocketUrl(), NetUtils.readyCertificate(context, profile));

                webSocket.addListener(this).connectAsynchronously();
            } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException | KeyManagementException ex) {
                notifyItemChanged(profile, new BaseProfile.TestStatus(BaseProfile.Status.ERROR));
            }
        }

        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
            notifyItemChanged(profile, new BaseProfile.TestStatus(BaseProfile.Status.ONLINE));

            pingTime = System.currentTimeMillis();
            websocket.sendPing();
        }

        @Override
        public void onPongFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            notifyItemChanged(profile, new BaseProfile.TestStatus(BaseProfile.Status.ONLINE, System.currentTimeMillis() - pingTime));
        }

        @Override
        public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
            notifyItemChanged(profile, new BaseProfile.TestStatus(BaseProfile.Status.OFFLINE));
        }

        @Override
        public void onUnexpectedError(WebSocket websocket, WebSocketException cause) throws Exception {
            notifyItemChanged(profile, new BaseProfile.TestStatus(BaseProfile.Status.ERROR));
        }

        @Override
        public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
            if (exception.getCause() instanceof ConnectException)
                notifyItemChanged(profile, new BaseProfile.TestStatus(BaseProfile.Status.OFFLINE));
            else if (exception.getCause() instanceof SocketTimeoutException)
                notifyItemChanged(profile, new BaseProfile.TestStatus(BaseProfile.Status.OFFLINE));
            else
                notifyItemChanged(profile, new BaseProfile.TestStatus(BaseProfile.Status.ERROR));
        }

        @Override
        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
            if (closedByServer)
                notifyItemChanged(profile, new BaseProfile.TestStatus(BaseProfile.Status.ERROR));
            else notifyItemChanged(profile, new BaseProfile.TestStatus(BaseProfile.Status.OFFLINE));
        }
    }
}
