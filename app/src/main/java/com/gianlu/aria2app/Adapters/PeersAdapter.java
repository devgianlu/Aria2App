package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gianlu.aria2app.NetIO.JTA2.Peer;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Sorting.NotFilterable;
import com.gianlu.commonutils.Sorting.OrderedRecyclerViewAdapter;
import com.gianlu.commonutils.SuperTextView;

import java.util.Comparator;
import java.util.List;

public class PeersAdapter extends OrderedRecyclerViewAdapter<PeersAdapter.ViewHolder, Peer, PeersAdapter.SortBy, NotFilterable> {
    private final IAdapter listener;
    private final LayoutInflater inflater;

    public PeersAdapter(Context context, List<Peer> peers, IAdapter listener) {
        super(peers, SortBy.DOWNLOAD_SPEED);
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position, List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
        } else {
            Peer peer = (Peer) payloads.get(0);
            holder.downloadSpeed.setText(CommonUtils.speedFormatter(peer.downloadSpeed, false));
            holder.uploadSpeed.setText(CommonUtils.speedFormatter(peer.uploadSpeed, false));
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Peer peer = objs.get(position);
        holder.address.setText(peer.ip + ":" + peer.port);
        holder.downloadSpeed.setText(CommonUtils.speedFormatter(peer.downloadSpeed, false));
        holder.uploadSpeed.setText(CommonUtils.speedFormatter(peer.uploadSpeed, false));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onPeerSelected(peer);
            }
        });
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

    public void notifyItemsChanged(List<Peer> peers) {
        for (Peer peer : peers) notifyItemChanged(peer);
    }

    public enum SortBy {
        DOWNLOAD_SPEED,
        UPLOAD_SPEED
    }

    public interface IAdapter {
        void onPeerSelected(Peer peer);

        void onItemCountUpdated(int count);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final SuperTextView address;
        final SuperTextView downloadSpeed;
        final SuperTextView uploadSpeed;

        ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.peer_item, parent, false));
            address = (SuperTextView) itemView.findViewById(R.id.peerItem_address);
            downloadSpeed = (SuperTextView) itemView.findViewById(R.id.peerItem_downloadSpeed);
            uploadSpeed = (SuperTextView) itemView.findViewById(R.id.peerItem_uploadSpeed);
        }
    }
}
