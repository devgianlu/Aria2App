package com.gianlu.aria2app.MoreAboutDownload.PeersFragment;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.gianlu.aria2app.R;
import com.github.mikephil.charting.charts.LineChart;

public class PeerViewHolder extends RecyclerView.ViewHolder {
    public TextView peerId;
    public TextView fullAddr;
    public TextView downloadSpeed;
    public TextView uploadSpeed;
    public LineChart chart;

    public PeerViewHolder(View itemView) {
        super(itemView);

        peerId = (TextView) itemView.findViewById(R.id.peerCardView_peerId);
        fullAddr = (TextView) itemView.findViewById(R.id.peerCardView_fullAddr);
        downloadSpeed = (TextView) itemView.findViewById(R.id.peerCardView_downloadSpeed);
        uploadSpeed = (TextView) itemView.findViewById(R.id.peerCardView_uploadSpeed);
        chart = (LineChart) itemView.findViewById(R.id.peerCardView_chart);
    }
}
