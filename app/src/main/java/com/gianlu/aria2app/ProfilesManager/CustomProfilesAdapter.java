package com.gianlu.aria2app.ProfilesManager;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.View;

import com.gianlu.aria2app.ProfilesManager.Testers.HttpProfileTester;
import com.gianlu.aria2app.ProfilesManager.Testers.ITesting;
import com.gianlu.aria2app.ProfilesManager.Testers.NetProfileTester;
import com.gianlu.aria2app.ProfilesManager.Testers.WsProfileTester;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.Drawer.ProfilesAdapter;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomProfilesAdapter extends ProfilesAdapter<MultiProfile> implements ITesting {
    private final IEdit editListener;
    private final ExecutorService service = Executors.newCachedThreadPool();
    private final Handler handler;

    public CustomProfilesAdapter(Context context, List<MultiProfile> profiles, IAdapter<MultiProfile> listener, boolean black, @Nullable IEdit editListener) {
        super(context, profiles, R.drawable.ripple_effect_dark, R.color.colorAccent, black, listener);
        this.editListener = editListener;
        this.handler = new Handler(context.getMainLooper());
    }

    @Override
    public MultiProfile getItem(int pos) {
        return profiles.get(pos);
    }

    @Override
    public void onBindViewHolder(ProfilesAdapter.ViewHolder holder, int position) {
        final MultiProfile multi = getItem(position);
        final MultiProfile.UserProfile profile = multi.getProfile(context);

        holder.globalName.setVisibility(View.GONE);
        holder.name.setText(profile.getProfileName(context));
        holder.secondary.setText(profile.getFullServerAddress());

        if (profile.status.latency != -1) {
            holder.ping.setVisibility(View.VISIBLE);
            holder.ping.setText(String.format(Locale.getDefault(), "%s ms", profile.status.latency));
        } else {
            holder.ping.setVisibility(View.GONE);
        }

        if (profile.status.status == MultiProfile.Status.UNKNOWN) {
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

    private void notifyItemChanged(MultiProfile.UserProfile profile, MultiProfile.TestStatus status) {
        profile.setStatus(status);

        final int pos = indexOf(profile);
        if (pos != -1) {
            notifyItemChanged(pos);
        }
    }

    private int indexOf(MultiProfile.UserProfile match) {
        for (int i = 0; i < profiles.size(); i++)
            if (profiles.get(i).profiles.contains(match))
                return i;

        return -1;
    }

    @Override
    protected void runTest(int pos) {
        final MultiProfile.UserProfile profile = getItem(pos).getProfile(context);

        switch (profile.connectionMethod) {
            default:
            case HTTP:
                service.execute(new HttpProfileTester(context, profile, this));
                break;
            case WEBSOCKET:
                service.execute(new WsProfileTester(context, profile, this));
                break;
        }
    }

    @Override
    public void onUpdate(String message) {
    }

    @Override
    public void onConnectionResult(NetProfileTester tester, final MultiProfile.UserProfile profile, final long when, final MultiProfile.TestStatus status) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                notifyItemChanged(profile, status);
            }
        });
    }

    @Override
    public void onAria2Result(boolean successful, String message) {
    }

    @Override
    public void onEnd() {
    }

    public interface IEdit {
        void onEditProfile(MultiProfile profile);
    }
}
