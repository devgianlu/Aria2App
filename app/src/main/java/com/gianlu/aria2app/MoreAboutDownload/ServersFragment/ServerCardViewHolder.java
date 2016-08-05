package com.gianlu.aria2app.MoreAboutDownload.ServersFragment;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.gianlu.aria2app.R;
import com.github.mikephil.charting.charts.LineChart;

public class ServerCardViewHolder extends RecyclerView.ViewHolder {
    public TextView currentUri;
    public TextView uri;
    public TextView downloadSpeed;
    public LineChart chart;

    public ServerCardViewHolder(View itemView) {
        super(itemView);

        currentUri = (TextView) itemView.findViewById(R.id.serverCardView_currentUri);
        uri = (TextView) itemView.findViewById(R.id.serverCardView_uri);
        downloadSpeed = (TextView) itemView.findViewById(R.id.serverCardView_downloadSpeed);
        chart = (LineChart) itemView.findViewById(R.id.serverCardView_chart);
    }
}
