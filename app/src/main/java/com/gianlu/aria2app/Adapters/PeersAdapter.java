package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.gianlu.aria2app.CountryFlags;
import com.gianlu.aria2app.NetIO.Aria2.Peer;
import com.gianlu.aria2app.NetIO.Geolocalization.GeoIP;
import com.gianlu.aria2app.NetIO.Geolocalization.IPDetails;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.Adapters.NotFilterable;
import com.gianlu.commonutils.Adapters.OrderedRecyclerViewAdapter;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.SuperTextView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

public class PeersAdapter extends OrderedRecyclerViewAdapter<PeersAdapter.ViewHolder, Peer, PeersAdapter.SortBy, NotFilterable> {
    private final Context context;
    private final Listener listener;
    private final LayoutInflater inflater;
    private final GeoIP geoIP;
    private final CountryFlags flags = CountryFlags.get();

    public PeersAdapter(Context context, @NonNull Listener listener) {
        super(new ArrayList<Peer>(), SortBy.DOWNLOAD_SPEED);
        this.inflater = LayoutInflater.from(context);
        this.context = context;
        this.listener = listener;
        this.geoIP = GeoIP.get();
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    protected void onUpdateViewHolder(@NonNull ViewHolder holder, int position, @NonNull Peer payload) {
        holder.downloadSpeed.setText(CommonUtils.speedFormatter(payload.downloadSpeed, false));
        holder.uploadSpeed.setText(CommonUtils.speedFormatter(payload.uploadSpeed, false));
    }

    @Override
    protected void onUpdateViewHolder(@NonNull ViewHolder holder, int position, @NonNull Object payload) {
        if (payload instanceof IPDetails)
            holder.flag.setImageDrawable(flags.loadFlag(context, ((IPDetails) payload).countryCode));
    }

    @Override
    public void onSetupViewHolder(@NonNull final ViewHolder holder, int position, @NonNull final Peer peer) {
        holder.address.setText(String.format(Locale.getDefault(), "%s:%d", peer.ip, peer.port));
        holder.downloadSpeed.setText(CommonUtils.speedFormatter(peer.downloadSpeed, false));
        holder.uploadSpeed.setText(CommonUtils.speedFormatter(peer.uploadSpeed, false));
        holder.flag.setImageResource(R.drawable.ic_list_country_unknown);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onPeerSelected(peer);
            }
        });

        geoIP.getIPDetails(peer.ip, new GeoIP.OnIpDetails() {
            @Override
            public void onDetails(@NonNull IPDetails details) {
                notifyItemChanged(holder.getAdapterPosition(), details);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                Logging.log(ex);
            }
        });
    }

    @Override
    protected boolean matchQuery(@NonNull Peer item, String query) {
        return true;
    }

    @Override
    protected void shouldUpdateItemCount(int count) {
        if (listener != null) listener.onItemCountUpdated(count);
    }

    @NonNull
    @Override
    public Comparator<Peer> getComparatorFor(SortBy sorting) {
        switch (sorting) {
            default:
            case DOWNLOAD_SPEED:
                return new Peer.DownloadSpeedComparator();
            case UPLOAD_SPEED:
                return new Peer.UploadSpeedComparator();
        }
    }

    public enum SortBy {
        DOWNLOAD_SPEED,
        UPLOAD_SPEED
    }

    public interface Listener {
        void onPeerSelected(@NonNull Peer peer);

        void onItemCountUpdated(int count);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final SuperTextView address;
        final SuperTextView downloadSpeed;
        final SuperTextView uploadSpeed;
        final ImageView flag;

        ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.item_peer, parent, false));
            address = itemView.findViewById(R.id.peerItem_address);
            downloadSpeed = itemView.findViewById(R.id.peerItem_downloadSpeed);
            uploadSpeed = itemView.findViewById(R.id.peerItem_uploadSpeed);
            flag = itemView.findViewById(R.id.peerItem_flag);
        }
    }
}
