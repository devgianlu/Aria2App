package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gianlu.aria2app.NetIO.JTA2.Peer;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.SuperTextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// TODO: Sorting
public class PeersAdapter extends RecyclerView.Adapter<PeersAdapter.ViewHolder> {
    private final List<Peer> peers;
    private final IAdapter listener;
    private final LayoutInflater inflater;

    public PeersAdapter(Context context, List<Peer> peers, IAdapter listener) {
        this.peers = peers;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;

        if (listener != null) listener.onItemCountUpdated(peers.size());
    }

    // FIXME: Mhh
    private static List<Peer> findMissingItems(List<Peer> oldItems, List<Peer> newItems) {
        List<Peer> missingItems = new ArrayList<>();
        for (Peer oldItem : oldItems)
            if (!newItems.contains(oldItem))
                missingItems.add(oldItem);
        return missingItems;
    }

    public void sort(SortBy sortBy) {
        switch (sortBy) {
            default:
            case DOWNLOAD_SPEED:
                Collections.sort(peers, new Peer.DownloadSpeedComparator());
                break;
            case UPLOAD_SPEED:
                Collections.sort(peers, new Peer.UploadSpeedComparator());
                break;
        }

        notifyDataSetChanged();
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
        final Peer peer = peers.get(position);
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
    public int getItemCount() {
        return peers.size();
    }

    private int indexOf(String peerId) {
        for (int i = 0; i < peers.size(); i++)
            if (Objects.equals(peers.get(i).peerId, peerId))
                return i;

        return -1;
    }

    public void notifyItemChanged(Peer payload) {
        int pos = indexOf(payload.peerId);
        if (pos == -1) {
            peers.add(payload);
            super.notifyItemInserted(peers.size() - 1);
            if (listener != null) listener.onItemCountUpdated(peers.size());
        } else {
            peers.set(pos, payload);
            super.notifyItemChanged(pos, payload);
        }
    }

    public void notifyItemsChanged(List<Peer> peers) {
        for (Peer peer : findMissingItems(this.peers, peers)) notifyItemRemoved(peer);
        for (Peer peer : peers) notifyItemChanged(peer);
    }

    private void notifyItemRemoved(Peer peer) {
        int pos = peers.indexOf(peer);
        if (pos != -1) {
            peers.remove(pos);
            notifyItemRemoved(pos);
        }
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
