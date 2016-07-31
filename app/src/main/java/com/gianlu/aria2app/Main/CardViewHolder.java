package com.gianlu.aria2app.Main;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gianlu.aria2app.R;
import com.github.lzyzsd.circleprogress.DonutProgress;
import com.github.mikephil.charting.charts.LineChart;

public class CardViewHolder extends RecyclerView.ViewHolder {
    public DonutProgress donutProgress;
    public TextView downloadName;
    public TextView downloadStatus;
    public TextView downloadSpeed;
    public TextView downloadMissingTime;
    public LinearLayout details;
    public LineChart detailsChart;
    public ImageButton detailsChartRefresh;
    public TextView detailsGid;
    public TextView detailsTotalLength;
    public TextView detailsCompletedLength;
    public TextView detailsUploadLength;
    public ImageButton expand;
    public Button more;
    public ImageButton menu;

    public CardViewHolder(View itemView) {
        super(itemView);

        donutProgress = (DonutProgress) itemView.findViewById(R.id.downloadCardView_donutProgress);
        downloadName = (TextView) itemView.findViewById(R.id.downloadCardView_name);
        downloadStatus = (TextView) itemView.findViewById(R.id.downloadCardView_status);
        downloadSpeed = (TextView) itemView.findViewById(R.id.downloadCardView_downloadSpeed);
        downloadMissingTime = (TextView) itemView.findViewById(R.id.downloadCardView_missingTime);
        details = (LinearLayout) itemView.findViewById(R.id.downloadCardView_details);
        expand = (ImageButton) itemView.findViewById(R.id.downloadCardView_expand);
        more = (Button) itemView.findViewById(R.id.downloadCardView_actionMore);
        menu = (ImageButton) itemView.findViewById(R.id.downloadCardView_actionMenu);

        detailsChart = (LineChart) itemView.findViewById(R.id.downloadCardViewDetails_chart);
        detailsChartRefresh = (ImageButton) itemView.findViewById(R.id.downloadCardViewDetails_chartRefresh);
        detailsGid = (TextView) itemView.findViewById(R.id.downloadCardViewDetails_gid);
        detailsTotalLength = (TextView) itemView.findViewById(R.id.downloadCardViewDetails_totalLength);
        detailsCompletedLength = (TextView) itemView.findViewById(R.id.downloadCardViewDetails_completedLength);
        detailsUploadLength = (TextView) itemView.findViewById(R.id.downloadCardViewDetails_uploadLength);
    }
}
