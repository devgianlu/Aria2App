package com.gianlu.aria2app.Main.Profile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFrame;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfilesAdapter extends BaseAdapter {
    private Activity context;
    private List<ProfileItem> profiles;
    private IProfile handler;

    public ProfilesAdapter(Activity context, List<ProfileItem> profiles, IProfile handler) {
        this.context = context;
        this.profiles = profiles;
        this.handler = handler;
    }

    public void startProfilesTest(IFinished handler) {
        for (int i = 0; i < profiles.size(); i++) {
            if (i == profiles.size() - 1)
                runTest(i, handler);
            else
                runTest(i, null);
        }
    }

    @Override
    public int getCount() {
        return profiles.size();
    }

    public SingleModeProfileItem getItem(int position) {
        ProfileItem item = profiles.get(position);
        if (item instanceof SingleModeProfileItem) {
            return (SingleModeProfileItem) item;
        } else {
            return ((MultiModeProfileItem) item).getCurrentProfile(context);
        }
    }

    public boolean isItemSingleMode(int position) {
        return profiles.get(position).isSingleMode();
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public List<String> getItemsName() {
        List<String> names = new ArrayList<>();
        for (ProfileItem item : profiles) {
            names.add(item.getGlobalProfileName());
        }
        return names;
    }

    @SuppressLint("ViewHolder")
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        view = LayoutInflater.from(context).inflate(R.layout.material_drawer_profile_item, viewGroup, false);

        final SingleModeProfileItem profile = getItem(i);

        if (isItemSingleMode(i)) {
            view.findViewById(R.id.materialDrawer_profileGlobalName).setVisibility(View.GONE);
            view.findViewById(R.id.materialDrawer_profileName).setPadding(0, 0, 0, 0);
            view.findViewById(R.id.materialDrawer_profileAddress).setPadding(0, 0, 0, 0);
        } else {
            view.findViewById(R.id.materialDrawer_profileGlobalName).setVisibility(View.VISIBLE);
            view.findViewById(R.id.materialDrawer_profileName).setPadding(18, 0, 0, 0);
            view.findViewById(R.id.materialDrawer_profileAddress).setPadding(18, 0, 0, 0);
            ((TextView) view.findViewById(R.id.materialDrawer_profileGlobalName)).setText(profile.getGlobalProfileName());
        }

        ((TextView) view.findViewById(R.id.materialDrawer_profileName)).setText(profile.getProfileName());
        ((TextView) view.findViewById(R.id.materialDrawer_profileAddress)).setText(profile.getFullServerAddr());

        if (profile.getLatency() != -1) {
            view.findViewById(R.id.materialDrawer_profilePing).setVisibility(View.VISIBLE);
            ((TextView) view.findViewById(R.id.materialDrawer_profilePing)).setText(String.format(Locale.getDefault(), "%s ms", profile.getLatency()));
        } else {
            view.findViewById(R.id.materialDrawer_profilePing).setVisibility(View.GONE);
        }

        if (profile.getStatus() != null || profile.getStatus() == ProfileItem.STATUS.UNKNOWN) {
            view.findViewById(R.id.materialDrawer_profileProgressBar).setVisibility(View.GONE);
            view.findViewById(R.id.materialDrawer_profileStatus).setVisibility(View.VISIBLE);
        } else {
            view.findViewById(R.id.materialDrawer_profileProgressBar).setVisibility(View.VISIBLE);
            view.findViewById(R.id.materialDrawer_profileStatus).setVisibility(View.GONE);
        }

        switch (profile.getStatus()) {
            case ONLINE:
                ((ImageView) view.findViewById(R.id.materialDrawer_profileStatus)).setImageResource(R.drawable.ic_done_black_48dp);
                break;
            case OFFLINE:
                ((ImageView) view.findViewById(R.id.materialDrawer_profileStatus)).setImageResource(R.drawable.ic_clear_black_48dp);
                break;
            case ERROR:
                ((ImageView) view.findViewById(R.id.materialDrawer_profileStatus)).setImageResource(R.drawable.ic_error_black_48dp);
                break;
            case UNKNOWN:
                ((ImageView) view.findViewById(R.id.materialDrawer_profileStatus)).setImageResource(R.drawable.ic_help_black_48dp);
                break;
        }

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handler.onProfileSelected(profile);
            }
        });

        return view;
    }

    public void runTest(int pos, @Nullable IFinished handler) {
        SingleModeProfileItem profile = getItem(pos);

        try {
            WebSocket webSocket;
            if (profile.getAuthMethod().equals(JTA2.AUTH_METHOD.HTTP))
                webSocket = Utils.readyWebSocket(profile.isServerSSL(), profile.getFullServerAddr(), profile.getServerUsername(), profile.getServerPassword());
            else
                webSocket = Utils.readyWebSocket(profile.isServerSSL(), profile.getFullServerAddr());

            webSocket.addListener(new StatusWebSocketHandler(profile, handler))
                    .connectAsynchronously();
        } catch (IOException | NoSuchAlgorithmException ex) {
            profile.setStatus(ProfileItem.STATUS.ERROR);
            profile.setStatusMessage(ex.getMessage());

            notifyDataSetChanged();
            if (handler != null)
                handler.onFinished();
        }
    }

    public interface IProfile {
        void onProfileSelected(SingleModeProfileItem which);
    }

    public interface IFinished {
        void onFinished();
    }

    private class StatusWebSocketHandler extends WebSocketAdapter {
        private long startTime;
        private SingleModeProfileItem profile;
        @Nullable
        private IFinished handler;

        public StatusWebSocketHandler(SingleModeProfileItem profile, @Nullable IFinished handler) {
            this.profile = profile;
            this.handler = handler;
        }

        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
            profile.setStatus(ProfileItem.STATUS.ONLINE);
            profile.setStatusMessage("Online");

            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
            if (handler != null)
                handler.onFinished();

            startTime = System.currentTimeMillis();
            websocket.sendPing();
        }

        @Override
        public void onPongFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            profile.setLatency(System.currentTimeMillis() - startTime);

            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
            profile.setStatus(ProfileItem.STATUS.OFFLINE);
            profile.setStatusMessage(cause.getMessage());

            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
            if (handler != null)
                handler.onFinished();
        }

        @Override
        public void onUnexpectedError(WebSocket websocket, WebSocketException cause) throws Exception {
            Utils.UIToast(context, Utils.TOAST_MESSAGES.WS_EXCEPTION, cause);
            profile.setStatus(ProfileItem.STATUS.ERROR);
            profile.setStatusMessage(cause.getMessage());

            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
            if (handler != null)
                handler.onFinished();
        }

        @Override
        public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
            if (exception.getCause() instanceof ConnectException)
                profile.setStatus(ProfileItem.STATUS.OFFLINE);
            else if (exception.getCause() instanceof SocketTimeoutException)
                profile.setStatus(ProfileItem.STATUS.OFFLINE);
            else
                profile.setStatus(ProfileItem.STATUS.ERROR);

            profile.setStatusMessage(exception.getMessage());

            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
            if (handler != null)
                handler.onFinished();
        }

        @Override
        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
            if (closedByServer) {
                profile.setStatus(ProfileItem.STATUS.ERROR);
                profile.setStatusMessage(serverCloseFrame.getCloseReason());
            } else {
                profile.setStatus(ProfileItem.STATUS.OFFLINE);
                profile.setStatusMessage(clientCloseFrame.getCloseReason());
            }

            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
            if (handler != null)
                handler.onFinished();
        }
    }
}
