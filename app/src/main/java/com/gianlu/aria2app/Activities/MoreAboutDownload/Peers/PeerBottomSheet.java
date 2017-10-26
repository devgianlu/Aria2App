package com.gianlu.aria2app.Activities.MoreAboutDownload.Peers;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.view.View;

import com.gianlu.aria2app.NetIO.FreeGeoIP.IPDetailsView;
import com.gianlu.aria2app.NetIO.JTA2.Peer;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.BaseBottomSheet;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.SuperTextView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;

import java.util.List;

public class PeerBottomSheet extends BaseBottomSheet<Peer> {
    private SuperTextView downloadSpeed;
    private SuperTextView uploadSpeed;
    private LineChart chart;
    private SuperTextView seeder;
    private SuperTextView peerChoking;
    private SuperTextView amChoking;
    private boolean hasIpDetails = false;
    private IPDetailsView ipDetails;

    public PeerBottomSheet(View sheet) {
        super(sheet, R.layout.peer_sheet, false);
    }

    @Override
    public void bindViews() {
        downloadSpeed = content.findViewById(R.id.peerSheet_downloadSpeed);
        uploadSpeed = content.findViewById(R.id.peerSheet_uploadSpeed);
        chart = content.findViewById(R.id.peerSheet_chart);
        seeder = content.findViewById(R.id.peerSheet_seeder);
        peerChoking = content.findViewById(R.id.peerSheet_peerChoking);
        amChoking = content.findViewById(R.id.peerSheet_amChoking);
        ipDetails = content.findViewById(R.id.peerSheet_ipDetails);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void setupView(@NonNull Peer peer) {
        title.setText(peer.ip + ":" + peer.port);
        Utils.setupChart(chart, true, R.color.colorPrimaryDark);

        if (peer.ipDetails != null) {
            ipDetails.setVisibility(View.VISIBLE);
            ipDetails.setup(peer.ipDetails);
            hasIpDetails = true;
        } else {
            hasIpDetails = false;
            ipDetails.setVisibility(View.GONE);
        }
    }

    @Override
    protected void updateView(@NonNull Peer peer) {
        downloadSpeed.setText(CommonUtils.speedFormatter(peer.downloadSpeed, false));
        uploadSpeed.setText(CommonUtils.speedFormatter(peer.uploadSpeed, false));

        LineData data = chart.getLineData();
        if (data != null) {
            int pos = data.getEntryCount() + 1;
            data.addEntry(new Entry(pos, peer.downloadSpeed), Utils.CHART_DOWNLOAD_SET);
            data.addEntry(new Entry(pos, peer.uploadSpeed), Utils.CHART_UPLOAD_SET);
            data.notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.setVisibleXRangeMaximum(60);
            chart.moveViewToX(data.getEntryCount());
        }

        seeder.setHtml(R.string.seeder, String.valueOf(peer.seeder));
        peerChoking.setHtml(R.string.peerChoking, String.valueOf(peer.peerChoking));
        amChoking.setHtml(R.string.amChoking, String.valueOf(peer.amChoking));

        if (!hasIpDetails && peer.ipDetails != null) {
            ipDetails.setVisibility(View.VISIBLE);
            ipDetails.setup(peer.ipDetails);
            hasIpDetails = true;
        }
    }

    public void update(List<Peer> peers) {
        if (current == null) return;
        update(Peer.find(peers, current));
    }
}
