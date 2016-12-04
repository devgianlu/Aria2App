package com.gianlu.aria2app.MoreAboutDownload.PeersFragment;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gianlu.aria2app.NetIO.JTA2.Peer;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;

import java.util.List;

class PeerCardAdapter extends RecyclerView.Adapter<PeerCardAdapter.ViewHolder> {
    private final Context context;
    private final List<Peer> objs;
    private final CardView noDataCardView;

    PeerCardAdapter(Context context, List<Peer> objs, CardView noDataCardView) {
        this.context = context;
        this.objs = objs;
        this.noDataCardView = noDataCardView;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.peer_cardview, parent, false));
    }

    void clear() {
        objs.clear();
        notifyDataSetChanged();
    }

    void onDisplayNoData(String message) {
        noDataCardView.setVisibility(View.VISIBLE);
        ((TextView) noDataCardView.findViewById(R.id.peersFragment_noDataLabel)).setText(context.getString(R.string.noPeersMessage, message));
    }

    void onUpdate(List<Peer> peers) {
        if (objs == null || peers == null) return;

        for (Peer newPeer : peers) {
            for (Peer listPeer : objs) {
                if (listPeer.peerId.equals(newPeer.peerId) && listPeer.getFullAddress().equals(newPeer.getFullAddress()))
                    notifyItemChanged(objs.indexOf(listPeer), newPeer);
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBindViewHolder(ViewHolder holder, int position, List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
            return;
        }

        if (payloads.get(0) instanceof Peer) {
            Peer peer = (Peer) payloads.get(0);

            holder.peerId.setText(peer.getPeerId());
            holder.fullAddr.setText(peer.getFullAddress());
            holder.uploadSpeed.setText(CommonUtils.speedFormatter(peer.uploadSpeed));
            holder.downloadSpeed.setText(CommonUtils.speedFormatter(peer.downloadSpeed));

            holder.detailsAmChoking.setText(Html.fromHtml(context.getString(R.string.amChoking, String.valueOf(peer.amChoking))));
            holder.detailsPeerChoking.setText(Html.fromHtml(context.getString(R.string.peerChoking, String.valueOf(peer.peerChoking))));
            holder.detailsSeeder.setText(Html.fromHtml(context.getString(R.string.seeder, String.valueOf(peer.seeder))));
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        if (objs.isEmpty())
            noDataCardView.setVisibility(View.VISIBLE);
        else
            noDataCardView.setVisibility(View.GONE);

        Peer peer = getItem(position);

        holder.peerId.setText(peer.getPeerId());
        holder.fullAddr.setText(peer.getFullAddress());
        holder.uploadSpeed.setText(CommonUtils.speedFormatter(peer.uploadSpeed));
        holder.downloadSpeed.setText(CommonUtils.speedFormatter(peer.downloadSpeed));

        holder.expand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CommonUtils.animateCollapsingArrowBellows(holder.expand, CommonUtils.isExpanded(holder.details));

                if (CommonUtils.isExpanded(holder.details))
                    CommonUtils.collapse(holder.details);
                else
                    CommonUtils.expand(holder.details);
            }
        });

        holder.detailsAmChoking.setText(Html.fromHtml(context.getString(R.string.amChoking, String.valueOf(peer.amChoking))));
        holder.detailsPeerChoking.setText(Html.fromHtml(context.getString(R.string.peerChoking, String.valueOf(peer.peerChoking))));
        holder.detailsSeeder.setText(Html.fromHtml(context.getString(R.string.seeder, String.valueOf(peer.seeder))));
    }

    @Override
    public int getItemCount() {
        if (objs == null)
            return 0;

        return objs.size();
    }

    private Peer getItem(int position) {
        return objs.get(position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView downloadSpeed;
        final TextView uploadSpeed;
        final TextView peerId;
        final TextView fullAddr;
        final ImageButton expand;
        final LinearLayout details;
        final TextView detailsAmChoking;
        final TextView detailsPeerChoking;
        final TextView detailsSeeder;

        ViewHolder(View itemView) {
            super(itemView);

            peerId = (TextView) itemView.findViewById(R.id.peerCardView_peerId);
            fullAddr = (TextView) itemView.findViewById(R.id.peerCardView_fullAddr);
            downloadSpeed = (TextView) itemView.findViewById(R.id.peerCardView_downloadSpeed);
            uploadSpeed = (TextView) itemView.findViewById(R.id.peerCardView_uploadSpeed);

            expand = (ImageButton) itemView.findViewById(R.id.peerCardView_expand);
            details = (LinearLayout) itemView.findViewById(R.id.peerCardView_details);
            detailsAmChoking = (TextView) itemView.findViewById(R.id.peerCardView_detailsAmChoking);
            detailsPeerChoking = (TextView) itemView.findViewById(R.id.peerCardView_detailsPeerChoking);
            detailsSeeder = (TextView) itemView.findViewById(R.id.peerCardView_detailsSeeder);
        }
    }
}
