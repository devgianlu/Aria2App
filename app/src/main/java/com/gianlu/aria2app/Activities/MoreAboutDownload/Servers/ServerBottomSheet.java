package com.gianlu.aria2app.Activities.MoreAboutDownload.Servers;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.util.SparseArray;
import android.view.View;

import com.gianlu.aria2app.NetIO.JTA2.Server;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.BaseBottomSheet;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.SuperTextView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;

import java.util.List;

public class ServerBottomSheet extends BaseBottomSheet<Server> {
    private SuperTextView downloadSpeed;
    private LineChart chart;
    private SuperTextView currentUri;
    private SuperTextView uri;

    public ServerBottomSheet(View parent, @LayoutRes int layoutRes) {
        super(parent, layoutRes, false);
    }

    @Override
    protected int getRippleDark() {
        return R.drawable.ripple_effect_dark;
    }

    @Override
    public void bindViews() {
        downloadSpeed = (SuperTextView) content.findViewById(R.id.serverSheet_downloadSpeed);
        chart = (LineChart) content.findViewById(R.id.serverSheet_chart);
        currentUri = (SuperTextView) content.findViewById(R.id.serverSheet_currentUri);
        uri = (SuperTextView) content.findViewById(R.id.serverSheet_uri);
    }

    @Override
    protected void setupView(@NonNull Server server) {
        title.setText(server.currentUri);
        Utils.setupChart(chart, true);
    }

    @Override
    protected void updateView(@NonNull Server server) {
        downloadSpeed.setText(CommonUtils.speedFormatter(server.downloadSpeed, false));

        LineData data = chart.getLineData();
        if (data != null) {
            int pos = data.getEntryCount() + 1;
            data.addEntry(new Entry(pos, server.downloadSpeed), Utils.CHART_DOWNLOAD_SET);
            data.notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.setVisibleXRangeMaximum(60);
            chart.moveViewToX(data.getEntryCount());
        }

        currentUri.setHtml(R.string.currentUri, server.currentUri);
        uri.setHtml(R.string.uri, server.uri);
    }

    public void update(SparseArray<List<Server>> servers) {
        if (current == null) return;
        update(Server.find(servers, current));
    }
}
