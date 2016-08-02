package com.gianlu.aria2app.MoreAboutDownload.PeersFragment;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.gianlu.aria2app.NetIO.JTA2.Peer;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;

import java.util.List;

public class PeerCardAdapter extends RecyclerView.Adapter<PeerViewHolder> {
    private Context context;
    private List<Peer> objs;

    public PeerCardAdapter(Context context, List<Peer> objs) {
        this.context = context;
        this.objs = objs;
    }

    @Override
    public PeerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new PeerViewHolder(LayoutInflater.from(context).inflate(R.layout.peer_cardview, parent, false));
    }

    public void onUpdate(List<Peer> peers) {

        for (Peer newPeer : peers) {
            for (Peer listPeer : objs) {
                if (listPeer.peerId.equals(newPeer.peerId) && listPeer.getFullAddress().equals(newPeer.getFullAddress()))
                    notifyItemChanged(objs.indexOf(listPeer), newPeer);
            }
        }
    }

    @Override
    public void onBindViewHolder(PeerViewHolder holder, int position, List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
            return;
        }

        if (payloads.get(0) instanceof Peer) {
            Peer peer = (Peer) payloads.get(0);
            holder.peerId.setText(peer.getPeerId());
            holder.fullAddr.setText(peer.getFullAddress());

            holder.uploadSpeed.setText(Utils.speedFormatter(peer.uploadSpeed));
            holder.downloadSpeed.setText(Utils.speedFormatter(peer.downloadSpeed));
        }
    }

    @Override
    public void onBindViewHolder(PeerViewHolder holder, int position) {
        Peer peer = getItem(position);

        holder.peerId.setText(peer.getPeerId());
        holder.fullAddr.setText(peer.getFullAddress());

        holder.uploadSpeed.setText(Utils.speedFormatter(peer.uploadSpeed));
        holder.downloadSpeed.setText(Utils.speedFormatter(peer.downloadSpeed));
    }

    @Override
    public int getItemCount() {
        return objs.size();
    }

    public Peer getItem(int position) {
        return objs.get(position);
    }
}
