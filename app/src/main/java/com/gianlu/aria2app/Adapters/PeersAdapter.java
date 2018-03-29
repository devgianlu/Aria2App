package com.gianlu.aria2app.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.gianlu.aria2app.CountryFlags;
import com.gianlu.aria2app.NetIO.Aria2.Peer;
import com.gianlu.aria2app.NetIO.FreeGeoIP.FreeGeoIPApi;
import com.gianlu.aria2app.NetIO.FreeGeoIP.IPDetails;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.Adapters.NotFilterable;
import com.gianlu.commonutils.Adapters.OrderedRecyclerViewAdapter;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.SuperTextView;

import java.util.ArrayList;
import java.util.Comparator;

public class PeersAdapter extends OrderedRecyclerViewAdapter<PeersAdapter.ViewHolder, Peer, PeersAdapter.SortBy, NotFilterable> {
    private final Context context;
    private final IAdapter handler;
    private final LayoutInflater inflater;
    private final FreeGeoIPApi freeGeoIPApi;
    private final CountryFlags flags = CountryFlags.get();

    public PeersAdapter(Context context, IAdapter handler) {
        super(new ArrayList<Peer>(), SortBy.DOWNLOAD_SPEED);
        this.inflater = LayoutInflater.from(context);
        this.context = context;
        this.handler = handler;
        this.freeGeoIPApi = FreeGeoIPApi.get();
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    protected void onBindViewHolder(ViewHolder holder, int position, @NonNull Peer payload) {
        holder.downloadSpeed.setText(CommonUtils.speedFormatter(payload.downloadSpeed, false));
        holder.uploadSpeed.setText(CommonUtils.speedFormatter(payload.uploadSpeed, false));
    }

    @Override
    protected void onBindViewHolder(ViewHolder holder, int position, Object payload) {
        if (payload instanceof IPDetails)
            holder.flag.setImageDrawable(flags.loadFlag(context, ((IPDetails) payload).countryCode));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final Peer peer = objs.get(position);
        holder.address.setText(peer.ip + ":" + peer.port);
        holder.downloadSpeed.setText(CommonUtils.speedFormatter(peer.downloadSpeed, false));
        holder.uploadSpeed.setText(CommonUtils.speedFormatter(peer.uploadSpeed, false));
        holder.flag.setImageResource(R.drawable.ic_list_country_unknown);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (handler != null) handler.onPeerSelected(peer);
            }
        });

        freeGeoIPApi.getIPDetails(peer.ip, new FreeGeoIPApi.IIPDetails() {
            @Override
            public void onDetails(IPDetails details) {
                notifyItemChanged(holder.getAdapterPosition(), details);
            }

            @Override
            public void onException(Exception ex) {
                Logging.log(ex);
            }
        });
    }

    @Nullable
    @Override
    protected RecyclerView getRecyclerView() {
        if (handler != null) return handler.getRecyclerView();
        else return null;
    }

    @Override
    protected boolean matchQuery(Peer item, String query) {
        return true;
    }

    @Override
    protected void shouldUpdateItemCount(int count) {
        if (handler != null) handler.onItemCountUpdated(count);
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

    public interface IAdapter {
        void onPeerSelected(Peer peer);

        void onItemCountUpdated(int count);

        @Nullable
        RecyclerView getRecyclerView();
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
