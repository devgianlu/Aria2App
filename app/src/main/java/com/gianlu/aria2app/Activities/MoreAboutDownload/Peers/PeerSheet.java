package com.gianlu.aria2app.Activities.MoreAboutDownload.Peers;

import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gianlu.aria2app.NetIO.Aria2.Peer;
import com.gianlu.aria2app.NetIO.Aria2.Peers;
import com.gianlu.aria2app.NetIO.Geolocalization.GeoIP;
import com.gianlu.aria2app.NetIO.Geolocalization.IPDetails;
import com.gianlu.aria2app.NetIO.Geolocalization.IPDetailsView;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.BottomSheet.ThemedModalBottomSheet;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.SuperTextView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;

import java.util.Locale;

public class PeerSheet extends ThemedModalBottomSheet<Peer, Peers> {
    private final GeoIP ipApi = GeoIP.get();
    private SuperTextView downloadSpeed;
    private SuperTextView uploadSpeed;
    private LineChart chart;
    private SuperTextView seeder;
    private SuperTextView peerChoking;
    private SuperTextView amChoking;
    private IPDetailsView ipDetails;
    private Peer currentPeer;

    @NonNull
    public static PeerSheet get() {
        return new PeerSheet();
    }

    private void update(@NonNull Peer peer) {
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
        downloadSpeed.setText(CommonUtils.speedFormatter(peer.downloadSpeed, false));
        uploadSpeed.setText(CommonUtils.speedFormatter(peer.uploadSpeed, false));
    }

    @Override
    protected void onRequestedUpdate(@NonNull Peers payload) {
        Peer updatedPeer = payload.find(currentPeer);
        if (updatedPeer != null) {
            currentPeer = updatedPeer;
            update(updatedPeer);
        }
    }

    @Override
    protected boolean onCreateHeader(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @NonNull Peer payload) {
        inflater.inflate(R.layout.sheet_header_peer, parent, true);
        parent.setBackgroundResource(R.color.colorTorrent);
        currentPeer = payload;

        TextView title = parent.findViewById(R.id.peerSheet_title);
        title.setText(String.format(Locale.getDefault(), "%s:%d", payload.ip, payload.port));

        downloadSpeed = parent.findViewById(R.id.peerSheet_downloadSpeed);
        uploadSpeed = parent.findViewById(R.id.peerSheet_uploadSpeed);

        return true;
    }

    @Override
    protected void onCreateBody(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @NonNull Peer payload) {
        inflater.inflate(R.layout.sheet_peer, parent, true);

        chart = parent.findViewById(R.id.peerSheet_chart);
        seeder = parent.findViewById(R.id.peerSheet_seeder);
        peerChoking = parent.findViewById(R.id.peerSheet_peerChoking);
        amChoking = parent.findViewById(R.id.peerSheet_amChoking);
        ipDetails = parent.findViewById(R.id.peerSheet_ipDetails);
        ipDetails.setVisibility(View.GONE);

        Utils.setupChart(chart, true);
        update(payload);

        ipApi.getIPDetails(payload.ip, new GeoIP.OnIpDetails() {
            @Override
            public void onDetails(@NonNull IPDetails details) {
                ipDetails.setup(details);
                ipDetails.setVisibility(View.VISIBLE);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                Logging.log(ex);
                ipDetails.setVisibility(View.GONE);
            }
        });

        isLoading(false);
    }

    @Override
    protected void onCustomizeToolbar(@NonNull Toolbar toolbar, @NonNull Peer payload) {
        toolbar.setBackgroundResource(R.color.colorTorrent_pressed);
        toolbar.setTitle(payload.ip + ":" + payload.port);
    }

    @Override
    protected boolean onCustomizeAction(@NonNull FloatingActionButton action, @NonNull Peer payload) {
        return false;
    }

    @Override
    protected int getCustomTheme(@NonNull Peer payload) {
        return R.style.AppTheme_NoActionBar_Torrent;
    }
}
