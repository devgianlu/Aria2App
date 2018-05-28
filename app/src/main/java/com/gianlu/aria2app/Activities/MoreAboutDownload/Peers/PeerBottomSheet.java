package com.gianlu.aria2app.Activities.MoreAboutDownload.Peers;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gianlu.aria2app.NetIO.Aria2.Peer;
import com.gianlu.aria2app.NetIO.Aria2.Peers;
import com.gianlu.aria2app.NetIO.FreeGeoIP.FreeGeoIPApi;
import com.gianlu.aria2app.NetIO.FreeGeoIP.IPDetails;
import com.gianlu.aria2app.NetIO.FreeGeoIP.IPDetailsView;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.BottomSheet.NiceBaseBottomSheet;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.SuperTextView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;

public class PeerBottomSheet extends NiceBaseBottomSheet {
    private final FreeGeoIPApi freeGeoIPApi;
    private SuperTextView downloadSpeed;
    private SuperTextView uploadSpeed;
    private LineChart chart;
    private SuperTextView seeder;
    private SuperTextView peerChoking;
    private SuperTextView amChoking;
    private IPDetailsView ipDetails;
    private Peer currentPeer = null;

    PeerBottomSheet(ViewGroup parent) {
        super(parent, R.layout.sheet_header_peer, R.layout.sheet_peer, false);
        freeGeoIPApi = FreeGeoIPApi.get();
    }

    @Override
    protected void onUpdateViews(Object... payloads) {
        if (currentPeer == null) return;
        Peer peer = Peer.find((Peers) payloads[0], currentPeer);
        updateContentViews(peer);
        updateHeaderViews(peer);
    }

    private void updateHeaderViews(Peer peer) {
        downloadSpeed.setText(CommonUtils.speedFormatter(peer.downloadSpeed, false));
        uploadSpeed.setText(CommonUtils.speedFormatter(peer.uploadSpeed, false));
    }

    private void updateContentViews(Peer peer) {
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

    @Override
    protected void cleanUp() {
        currentPeer = null;
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreateHeaderView(@NonNull ViewGroup parent, Object... payloads) {
        TextView title = parent.findViewById(R.id.peerSheet_title);
        downloadSpeed = parent.findViewById(R.id.peerSheet_downloadSpeed);
        uploadSpeed = parent.findViewById(R.id.peerSheet_uploadSpeed);

        parent.setBackgroundResource(R.color.colorTorrent);

        Peer peer = (Peer) payloads[0];
        currentPeer = peer;

        title.setText(peer.ip + ":" + peer.port);
        updateHeaderViews(peer);
    }

    @Override
    protected void onCreateContentView(@NonNull ViewGroup parent, Object... payloads) {
        chart = parent.findViewById(R.id.peerSheet_chart);
        seeder = parent.findViewById(R.id.peerSheet_seeder);
        peerChoking = parent.findViewById(R.id.peerSheet_peerChoking);
        amChoking = parent.findViewById(R.id.peerSheet_amChoking);
        ipDetails = parent.findViewById(R.id.peerSheet_ipDetails);
        ipDetails.setVisibility(View.GONE);

        Peer peer = (Peer) payloads[0];

        Utils.setupChart(chart, true, R.color.colorPrimaryDark);
        freeGeoIPApi.getIPDetails(peer.ip, new FreeGeoIPApi.OnIpDetails() {
            @Override
            public void onDetails(IPDetails details) {
                ipDetails.setup(details);
                ipDetails.setVisibility(View.VISIBLE);
            }

            @Override
            public void onException(Exception ex) {
                Logging.log(ex);
                ipDetails.setVisibility(View.GONE);
            }
        });

        updateContentViews(peer);
    }
}
