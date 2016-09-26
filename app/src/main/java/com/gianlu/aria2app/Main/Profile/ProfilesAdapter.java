package com.gianlu.aria2app.Main.Profile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
    private final Activity context;
    private final List<ProfileItem> profiles;
    private final IProfile handler;
    private final View.OnClickListener statusClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String message = (String) v.getTag();
            if (message != null)
                Utils.UIToast(context, message);
        }
    };

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

    private boolean isItemSingleMode(int position) {
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
        ViewHolder holder = new ViewHolder(LayoutInflater.from(context).inflate(R.layout.material_drawer_profile_item, viewGroup, false));
        final SingleModeProfileItem profile = getItem(i);

        if (isItemSingleMode(i)) {
            holder.globalName.setVisibility(View.GONE);
            holder.name.setPadding(0, 0, 0, 0);
            holder.address.setPadding(0, 0, 0, 0);
        } else {
            holder.globalName.setVisibility(View.VISIBLE);
            holder.name.setPadding(18, 0, 0, 0);
            holder.address.setPadding(18, 0, 0, 0);
            holder.globalName.setText(profile.getGlobalProfileName());
        }

        holder.name.setText(profile.getProfileName());
        holder.address.setText(profile.getFullServerAddr());

        if (profile.getLatency() != -1) {
            holder.ping.setVisibility(View.VISIBLE);
            holder.ping.setText(String.format(Locale.getDefault(), "%s ms", profile.getLatency()));
        } else {
            holder.ping.setVisibility(View.GONE);
        }

        if (profile.getStatus() == ProfileItem.STATUS.UNKNOWN) {
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.status.setVisibility(View.GONE);
        } else {
            holder.progressBar.setVisibility(View.GONE);
            holder.status.setVisibility(View.VISIBLE);
            holder.status.setOnClickListener(statusClick);
            holder.status.setTag(profile.getStatusMessage());

            switch (profile.getStatus()) {
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

        holder.rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handler.onProfileSelected(profile);
            }
        });

        return holder.rootView;
    }

    private void runTest(int pos, @Nullable IFinished handler) {
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

    private class ViewHolder {
        final View rootView;
        final TextView globalName;
        final TextView name;
        final TextView address;
        final TextView ping;
        final ProgressBar progressBar;
        final ImageView status;

        public ViewHolder(View rootView) {
            this.rootView = rootView;

            globalName = (TextView) rootView.findViewById(R.id.materialDrawer_profileGlobalName);
            name = (TextView) rootView.findViewById(R.id.materialDrawer_profileName);
            address = (TextView) rootView.findViewById(R.id.materialDrawer_profileAddress);
            ping = (TextView) rootView.findViewById(R.id.materialDrawer_profilePing);
            progressBar = (ProgressBar) rootView.findViewById(R.id.materialDrawer_profileProgressBar);
            status = (ImageView) rootView.findViewById(R.id.materialDrawer_profileStatus);
        }
    }

    private class StatusWebSocketHandler extends WebSocketAdapter {
        private final SingleModeProfileItem profile;
        @Nullable
        private final IFinished handler;
        private long startTime;

        StatusWebSocketHandler(SingleModeProfileItem profile, @Nullable IFinished handler) {
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
