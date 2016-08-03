package com.gianlu.aria2app.MoreAboutDownload.ServersFragment;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gianlu.aria2app.R;
import com.github.mikephil.charting.charts.LineChart;

public class ServerViewHolder extends RecyclerView.ViewHolder {
    public RelativeLayout header;
    public TextView downloadSpeed;
    public LineChart chart;

    public LinearLayout details;

    public ServerViewHolder(View itemView) {
        super(itemView);

        header = (RelativeLayout) itemView.findViewById(R.id.serverCardView_header);
        downloadSpeed = (TextView) itemView.findViewById(R.id.serverCardView_downloadSpeed);
        chart = (LineChart) itemView.findViewById(R.id.serverCardView_chart);

        details = (LinearLayout) itemView.findViewById(R.id.serverCardView_details);
    }
}
