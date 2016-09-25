package com.gianlu.aria2app.MoreAboutDownload.PeersFragment;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gianlu.aria2app.R;
import com.github.mikephil.charting.charts.LineChart;

class PeerCardViewHolder extends RecyclerView.ViewHolder {
    public final TextView downloadSpeed;
    public final TextView uploadSpeed;
    final RelativeLayout header;
    final TextView peerId;
    final TextView fullAddr;
    final LinearLayout details;
    final TextView detailsAmChoking;
    final TextView detailsPeerChoking;
    final TextView detailsSeeder;
    LineChart chart;

    PeerCardViewHolder(View itemView) {
        super(itemView);

        header = (RelativeLayout) itemView.findViewById(R.id.peerCardView_header);
        peerId = (TextView) itemView.findViewById(R.id.peerCardView_peerId);
        fullAddr = (TextView) itemView.findViewById(R.id.peerCardView_fullAddr);
        downloadSpeed = (TextView) itemView.findViewById(R.id.peerCardView_downloadSpeed);
        uploadSpeed = (TextView) itemView.findViewById(R.id.peerCardView_uploadSpeed);
        chart = (LineChart) itemView.findViewById(R.id.peerCardView_chart);

        details = (LinearLayout) itemView.findViewById(R.id.peerCardView_details);
        detailsAmChoking = (TextView) itemView.findViewById(R.id.peerCardView_detailsAmChoking);
        detailsPeerChoking = (TextView) itemView.findViewById(R.id.peerCardView_detailsPeerChoking);
        detailsSeeder = (TextView) itemView.findViewById(R.id.peerCardView_detailsSeeder);

    }
}
