package com.gianlu.aria2app.MoreAboutDownload.PeersFragment;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.gianlu.aria2app.DownloadsListing.Charting;
import com.gianlu.aria2app.NetIO.JTA2.Peer;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

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
        if (objs == null || peers == null) return;

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

            LineData data = holder.chart.getData();
            data.addXValue(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new java.util.Date()));
            data.addEntry(new Entry(peer.downloadSpeed, data.getDataSetByIndex(Charting.DOWNLOAD_SET).getEntryCount()), Charting.DOWNLOAD_SET);
            data.addEntry(new Entry(peer.uploadSpeed, data.getDataSetByIndex(Charting.UPLOAD_SET).getEntryCount()), Charting.UPLOAD_SET);

            holder.chart.notifyDataSetChanged();
            holder.chart.setVisibleXRangeMaximum(60);
            holder.chart.moveViewToX(data.getXValCount() - 61);

            holder.peerId.setText(peer.getPeerId());
            holder.fullAddr.setText(peer.getFullAddress());
            holder.uploadSpeed.setText(Utils.speedFormatter(peer.uploadSpeed));
            holder.downloadSpeed.setText(Utils.speedFormatter(peer.downloadSpeed));
        }
    }

    @Override
    public void onBindViewHolder(PeerViewHolder holder, int position) {
        Peer peer = getItem(position);

        holder.chart = Charting.setupPeerChart(holder.chart);

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
