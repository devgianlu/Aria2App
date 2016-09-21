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

class CardViewHolder extends RecyclerView.ViewHolder {
    DonutProgress donutProgress;
    TextView downloadName;
    TextView downloadStatus;
    TextView downloadSpeed;
    TextView downloadMissingTime;
    LinearLayout details;
    LineChart detailsChart;
    ImageButton detailsChartRefresh;
    TextView detailsGid;
    TextView detailsTotalLength;
    TextView detailsCompletedLength;
    TextView detailsUploadLength;
    ImageButton expand;
    Button more;
    ImageButton menu;

    CardViewHolder(View itemView) {
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
