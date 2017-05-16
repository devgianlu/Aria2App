package com.gianlu.aria2app.Activities.MoreAboutDownload.Peers;

import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.view.View;
import android.widget.TextView;

import com.gianlu.aria2app.NetIO.JTA2.Peer;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.SuperTextView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;

import java.util.List;

public class PeerBottomSheet extends BottomSheetBehavior.BottomSheetCallback {
    private final BottomSheetBehavior behavior;
    private final TextView title;
    private final SuperTextView downloadSpeed;
    private final SuperTextView uploadSpeed;
    private final LineChart chart;
    private final SuperTextView seeder;
    private final SuperTextView peerChoking;
    private final SuperTextView amChoking;
    private Peer currentPeer;

    private PeerBottomSheet(View sheet, final BottomSheetBehavior behavior) {
        this.behavior = behavior;
        this.behavior.setBottomSheetCallback(this);

        this.title = (TextView) sheet.findViewById(R.id.peersFragment_sheetTitle);
        this.downloadSpeed = (SuperTextView) sheet.findViewById(R.id.peersFragment_sheetDownloadSpeed);
        this.uploadSpeed = (SuperTextView) sheet.findViewById(R.id.peersFragment_sheetUploadSpeed);
        this.chart = (LineChart) sheet.findViewById(R.id.peersFragment_sheetChart);
        this.seeder = (SuperTextView) sheet.findViewById(R.id.peersFragment_sheetSeeder);
        this.peerChoking = (SuperTextView) sheet.findViewById(R.id.peersFragment_sheetPeerChoking);
        this.amChoking = (SuperTextView) sheet.findViewById(R.id.peersFragment_sheetAmChoking);

        sheet.findViewById(R.id.peersFragment_sheetClose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
    }

    public static PeerBottomSheet setup(View sheet) {
        BottomSheetBehavior behavior = BottomSheetBehavior.from(sheet);
        behavior.setPeekHeight(0);
        behavior.setHideable(true);
        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        return new PeerBottomSheet(sheet, behavior);
    }

    @Override
    public void onStateChanged(@NonNull View bottomSheet, int newState) {
        if (newState == BottomSheetBehavior.STATE_COLLAPSED) behavior.setPeekHeight(0);
    }

    @Override
    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
    }

    public boolean shouldUpdate() {
        return behavior.getState() == BottomSheetBehavior.STATE_EXPANDED;
    }

    public void expand(Peer peer) {
        currentPeer = peer;
        setupView(peer);
        updateView(peer);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void setupView(Peer peer) {
        if (peer == null) return;
        title.setText(peer.ip + ":" + peer.port);

        Utils.setupChart(chart, true);
    }

    private void updateView(Peer peer) {
        if (peer == null) return;
        currentPeer = peer;

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
    }

    public void update(List<Peer> peers) {
        if (currentPeer == null) return;
        update(Peer.find(peers, currentPeer.peerId));
    }

    public void update(Peer peer) {
        updateView(peer);
    }

    public void collapse() {
        currentPeer = null;
        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }
}
