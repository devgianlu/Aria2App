package com.gianlu.aria2app.MoreAboutDownload.PeersFragment;

import android.content.Context;
import android.os.Build;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gianlu.aria2app.NetIO.JTA2.Peer;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class PeerCardAdapter extends RecyclerView.Adapter<PeerCardViewHolder> {
    private Context context;
    private List<Peer> objs;
    private boolean showCharts = false;
    private CardView noDataCardView;

    public PeerCardAdapter(Context context, List<Peer> objs, CardView noDataCardView) {
        this.context = context;
        this.objs = objs;
        this.noDataCardView = noDataCardView;
    }

    public static boolean isExpanded(View v) {
        return v.getVisibility() == View.VISIBLE;
    }

    public static void expand(final View v) {
        v.measure(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        final int targetHeight = v.getMeasuredHeight();

        v.getLayoutParams().height = 0;
        v.setVisibility(View.VISIBLE);
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1
                        ? RelativeLayout.LayoutParams.WRAP_CONTENT
                        : (int) (targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration((int) (targetHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    public static void collapse(final View v) {
        final int initialHeight = v.getMeasuredHeight();

        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    v.setVisibility(View.GONE);
                } else {
                    v.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration((int) (initialHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    @Override
    public PeerCardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new PeerCardViewHolder(LayoutInflater.from(context).inflate(R.layout.peer_cardview, parent, false));
    }

    public void clear() {
        objs.clear();
        notifyDataSetChanged();
    }

    public void onDisplayNoData(String message) {
        noDataCardView.setVisibility(View.VISIBLE);
        ((TextView) noDataCardView.findViewById(R.id.peersFragment_noDataLabel)).setText(context.getString(R.string.noPeersMessage, message));
    }

    public void onUpdate(List<Peer> peers, boolean showCharts) {
        this.showCharts = showCharts;
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
    public void onBindViewHolder(PeerCardViewHolder holder, int position, List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
            return;
        }

        if (payloads.get(0) instanceof Peer) {
            Peer peer = (Peer) payloads.get(0);

            if (showCharts) {
                holder.chart.setVisibility(View.VISIBLE);

                LineData data = holder.chart.getData();
                data.addXValue(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new java.util.Date()));
                data.addEntry(new Entry(peer.downloadSpeed, data.getDataSetByIndex(Utils.CHART_DOWNLOAD_SET).getEntryCount()), Utils.CHART_DOWNLOAD_SET);
                data.addEntry(new Entry(peer.uploadSpeed, data.getDataSetByIndex(Utils.CHART_UPLOAD_SET).getEntryCount()), Utils.CHART_UPLOAD_SET);

                holder.chart.notifyDataSetChanged();
                holder.chart.setVisibleXRangeMaximum(60);
                holder.chart.moveViewToX(data.getXValCount() - 61);
            } else {
                holder.chart.setVisibility(View.GONE);
            }

            holder.peerId.setText(peer.getPeerId());
            holder.fullAddr.setText(peer.getFullAddress());
            holder.uploadSpeed.setText(Utils.speedFormatter(peer.uploadSpeed));
            holder.downloadSpeed.setText(Utils.speedFormatter(peer.downloadSpeed));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                holder.detailsAmChoking.setText(Html.fromHtml(context.getString(R.string.amChoking, String.valueOf(peer.amChoking)), Html.FROM_HTML_MODE_COMPACT));
                holder.detailsPeerChoking.setText(Html.fromHtml(context.getString(R.string.peerChoking, String.valueOf(peer.peerChoking)), Html.FROM_HTML_MODE_COMPACT));
                holder.detailsSeeder.setText(Html.fromHtml(context.getString(R.string.seeder, String.valueOf(peer.seeder)), Html.FROM_HTML_MODE_COMPACT));
            } else {
                holder.detailsAmChoking.setText(Html.fromHtml(context.getString(R.string.amChoking, String.valueOf(peer.amChoking))));
                holder.detailsPeerChoking.setText(Html.fromHtml(context.getString(R.string.peerChoking, String.valueOf(peer.peerChoking))));
                holder.detailsSeeder.setText(Html.fromHtml(context.getString(R.string.seeder, String.valueOf(peer.seeder))));
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBindViewHolder(final PeerCardViewHolder holder, int position) {
        if (objs.isEmpty())
            noDataCardView.setVisibility(View.VISIBLE);
        else
            noDataCardView.setVisibility(View.GONE);

        Peer peer = getItem(position);

        holder.chart = Utils.setupPeerChart(holder.chart);
        holder.peerId.setText(peer.getPeerId());
        holder.fullAddr.setText(peer.getFullAddress());
        holder.uploadSpeed.setText(Utils.speedFormatter(peer.uploadSpeed));
        holder.downloadSpeed.setText(Utils.speedFormatter(peer.downloadSpeed));

        holder.header.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isExpanded(holder.details))
                    collapse(holder.details);
                else
                    expand(holder.details);
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            holder.detailsAmChoking.setText(Html.fromHtml(context.getString(R.string.amChoking, String.valueOf(peer.amChoking)), Html.FROM_HTML_MODE_COMPACT));
            holder.detailsPeerChoking.setText(Html.fromHtml(context.getString(R.string.peerChoking, String.valueOf(peer.peerChoking)), Html.FROM_HTML_MODE_COMPACT));
            holder.detailsSeeder.setText(Html.fromHtml(context.getString(R.string.seeder, String.valueOf(peer.seeder)), Html.FROM_HTML_MODE_COMPACT));
        } else {
            holder.detailsAmChoking.setText(Html.fromHtml(context.getString(R.string.amChoking, String.valueOf(peer.amChoking))));
            holder.detailsPeerChoking.setText(Html.fromHtml(context.getString(R.string.peerChoking, String.valueOf(peer.peerChoking))));
            holder.detailsSeeder.setText(Html.fromHtml(context.getString(R.string.seeder, String.valueOf(peer.seeder))));
        }

        if (showCharts)
            holder.chart.setVisibility(View.VISIBLE);
        else
            holder.chart.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return objs.size();
    }

    public Peer getItem(int position) {
        return objs.get(position);
    }
}
