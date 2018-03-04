package com.gianlu.aria2app.ProfilesManager;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.gianlu.aria2app.NetIO.NetUtils;
import com.gianlu.aria2app.ProfilesManager.Testers.HttpTester;
import com.gianlu.aria2app.ProfilesManager.Testers.NetTester;
import com.gianlu.aria2app.ProfilesManager.Testers.WebSocketTester;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.Drawer.ProfilesAdapter;
import com.gianlu.commonutils.Logging;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class CustomProfilesAdapter extends ProfilesAdapter<MultiProfile> implements NetTester.IProfileTester {
    private final IEdit editListener;
    private final ExecutorService service = Executors.newCachedThreadPool();
    private final Handler handler;

    public CustomProfilesAdapter(Context context, List<MultiProfile> profiles, IAdapter<MultiProfile> listener, boolean black, @Nullable IEdit editListener) {
        super(context, profiles, R.color.colorAccent, black, listener);
        this.editListener = editListener;
        this.handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public MultiProfile getItem(int pos) {
        return profiles.get(pos);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfilesAdapter.ViewHolder holder, int position) {
        final MultiProfile multi = getItem(position);
        final MultiProfile.UserProfile profile = multi.getProfile(context);

        holder.globalName.setVisibility(View.GONE);
        holder.name.setText(profile.getProfileName(context));

        try {
            holder.secondary.setText(profile.getFullServerAddress());
        } catch (NetUtils.InvalidUrlException ex) {
            Logging.log(ex);
            holder.secondary.setText(null);
        }

        if (multi.status.latency != -1) {
            holder.ping.setVisibility(View.VISIBLE);
            holder.ping.setText(String.format(Locale.getDefault(), "%s ms", multi.status.latency));
        } else {
            holder.ping.setVisibility(View.GONE);
        }

        if (multi.status.status == MultiProfile.Status.UNKNOWN) {
            holder.loading.setVisibility(View.VISIBLE);
            holder.status.setVisibility(View.GONE);
        } else {
            holder.loading.setVisibility(View.GONE);
            holder.status.setVisibility(View.VISIBLE);

            switch (multi.status.status) {
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
                if (listener != null) listener.onProfileSelected(multi);
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (editListener != null) editListener.onEditProfile(multi);
                return editListener != null;
            }
        });
    }

    private int indexOf(String profileId) {
        for (int i = 0; i < profiles.size(); i++)
            if (Objects.equals(profiles.get(i).id, profileId))
                return i;

        return -1;
    }

    private void notifyItemChanged(String profileId, MultiProfile.TestStatus status) {
        int pos = indexOf(profileId);
        if (pos != -1) {
            MultiProfile profile = profiles.get(pos);
            profile.setStatus(status);
            notifyItemChanged(pos);
        }
    }

    private void notifyItemChanged(String profileId, long ping) {
        int pos = indexOf(profileId);
        if (pos != -1) {
            MultiProfile profile = profiles.get(pos);
            profile.updateStatusPing(ping);
            notifyItemChanged(pos);
        }
    }

    @Override
    protected void runTest(int pos) {
        final MultiProfile.UserProfile profile = getItem(pos).getProfile(context);

        switch (profile.connectionMethod) {
            default:
            case HTTP:
                service.submit(new HttpTester(context, profile, this));
                break;
            case WEBSOCKET:
                service.submit(new WebSocketTester(context, profile, this));
                break;
        }
    }

    @Override
    public void statusUpdated(final String profileId, final MultiProfile.TestStatus status) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                notifyItemChanged(profileId, status);
            }
        });
    }

    @Override
    public void pingUpdated(final String profileId, final long ping) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                notifyItemChanged(profileId, ping);
            }
        });
    }

    public interface IEdit {
        void onEditProfile(MultiProfile profile);
    }
}
