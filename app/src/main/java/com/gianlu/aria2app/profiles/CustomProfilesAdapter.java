package com.gianlu.aria2app.profiles;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.aria2app.profiles.testers.NetTester;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.drawer.DrawerManager;
import com.gianlu.commonutils.drawer.ProfilesAdapter;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class CustomProfilesAdapter extends ProfilesAdapter<MultiProfile, CustomProfilesAdapter.ViewHolder> implements NetTester.ProfileTesterCallback {
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final LayoutInflater inflater;
    private final boolean forceWhite;

    public CustomProfilesAdapter(Context context, List<MultiProfile> profiles, @StyleRes int overrideStyle, DrawerManager.ProfilesDrawerListener<MultiProfile> listener) {
        super(context, profiles, listener);
        if (overrideStyle == 0) this.inflater = LayoutInflater.from(context);
        else this.inflater = LayoutInflater.from(new ContextThemeWrapper(context, overrideStyle));

        forceWhite = overrideStyle == R.style.ForceWhite;
    }

    @Override
    public MultiProfile getItem(int pos) {
        return profiles.get(pos);
    }

    @NonNull
    @Override
    public CustomProfilesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull MultiProfile multi) {
        MultiProfile.UserProfile profile = multi.getProfile(context);

        holder.name.setText(profile.getPrimaryText(context));
        holder.secondary.setText(profile.getSecondaryText(context));

        if (profile.isInAppDownloader()) {
            holder.loading.setVisibility(View.GONE);
            holder.status.setVisibility(View.VISIBLE);
            if (forceWhite) holder.status.setImageResource(R.drawable.ic_aria2_notification);
            else holder.status.setImageResource(R.drawable.ic_aria2android);
        } else {
            if (multi.status.status == MultiProfile.Status.UNKNOWN) {
                holder.loading.setVisibility(View.VISIBLE);
                holder.status.setVisibility(View.GONE);
            } else {
                holder.loading.setVisibility(View.GONE);
                holder.status.setVisibility(View.VISIBLE);

                switch (multi.status.status) {
                    case ONLINE:
                        holder.status.setImageResource(R.drawable.baseline_done_24);
                        break;
                    case OFFLINE:
                        holder.status.setImageResource(R.drawable.baseline_clear_24);
                        break;
                    case ERROR:
                        holder.status.setImageResource(R.drawable.baseline_error_24);
                        break;
                }
            }
        }

        if (multi.status.latency != -1) {
            holder.ping.setVisibility(View.VISIBLE);
            holder.ping.setText(String.format(Locale.getDefault(), "%s ms", multi.status.latency));
        } else {
            holder.ping.setVisibility(View.GONE);
        }
    }

    private int indexOf(String profileId) {
        for (int i = 0; i < profiles.size(); i++)
            if (Objects.equals(profiles.get(i).id, profileId))
                return i;

        return -1;
    }

    private void itemChanged(@NonNull String profileId, @NonNull MultiProfile.TestStatus status) {
        final int pos = indexOf(profileId);
        if (pos != -1) {
            MultiProfile profile = profiles.get(pos);
            profile.setStatus(status);
            post(() -> notifyItemChanged(pos));
        }
    }

    private void itemChanged(@NonNull String profileId, long ping) {
        final int pos = indexOf(profileId);
        if (pos != -1) {
            MultiProfile profile = profiles.get(pos);
            profile.updateStatusPing(ping);
            post(() -> notifyItemChanged(pos));
        }
    }

    @Override
    protected void runTest(int pos) {
        executorService.execute(new NetTester(context, getItem(pos).getProfile(context), true, this));
    }

    @Override
    public void statusUpdated(@NonNull String profileId, @NonNull MultiProfile.TestStatus status) {
        itemChanged(profileId, status);
    }

    @Override
    public void pingUpdated(@NonNull String profileId, long ping) {
        itemChanged(profileId, ping);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView secondary;
        final ProgressBar loading;
        final ImageView status;
        final TextView ping;

        public ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_profile, parent, false));

            name = itemView.findViewById(R.id.profileItem_name);
            secondary = itemView.findViewById(R.id.profileItem_secondary);
            loading = itemView.findViewById(R.id.profileItem_loading);
            status = itemView.findViewById(R.id.profileItem_status);
            ping = itemView.findViewById(R.id.profileItem_ping);
        }
    }
}
