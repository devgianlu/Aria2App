package com.gianlu.aria2app.Main;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gianlu.aria2app.R;
import com.github.lzyzsd.circleprogress.DonutProgress;
import com.github.mikephil.charting.charts.LineChart;

public class CardViewHolder extends RecyclerView.ViewHolder {
    public RelativeLayout header;
    public DonutProgress donutProgress;
    public TextView downloadName;
    public TextView downloadStatus;
    public LinearLayout details;
    public LineChart detailsChart;
    public TextView detailsGid;
    public TextView detailsTotalLength;
    public TextView detailsCompletedLength;
    public TextView detailsUploadLength;
    public Button more;
    public ImageButton menu;

    public CardViewHolder(View itemView) {
        super(itemView);

        header = (RelativeLayout) itemView.findViewById(R.id.downloadCardView_header);
        donutProgress = (DonutProgress) itemView.findViewById(R.id.downloadCardView_donutProgress);
        downloadName = (TextView) itemView.findViewById(R.id.downloadCardView_name);
        downloadStatus = (TextView) itemView.findViewById(R.id.downloadCardView_status);
        details = (LinearLayout) itemView.findViewById(R.id.downloadCardView_details);
        more = (Button) itemView.findViewById(R.id.downloadCardView_actionMore);
        menu = (ImageButton) itemView.findViewById(R.id.downloadCardView_actionMenu);

        detailsChart = (LineChart) itemView.findViewById(R.id.downloadCardViewDetails_chart);
        detailsGid = (TextView) itemView.findViewById(R.id.downloadCardViewDetails_gid);
        detailsTotalLength = (TextView) itemView.findViewById(R.id.downloadCardViewDetails_totalLength);
        detailsCompletedLength = (TextView) itemView.findViewById(R.id.downloadCardViewDetails_completedLength);
        detailsUploadLength = (TextView) itemView.findViewById(R.id.downloadCardViewDetails_uploadLength);
    }
}
