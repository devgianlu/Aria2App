package com.gianlu.aria2app.Activities.MoreAboutDownload.Servers;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.gianlu.aria2app.NetIO.Aria2.Server;
import com.gianlu.aria2app.NetIO.Aria2.SparseServers;
import com.gianlu.aria2app.NetIO.Geolocalization.GeoIP;
import com.gianlu.aria2app.NetIO.Geolocalization.IPDetails;
import com.gianlu.aria2app.NetIO.Geolocalization.IPDetailsView;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.bottomsheet.ModalBottomSheetHeaderView;
import com.gianlu.commonutils.bottomsheet.ThemedModalBottomSheet;
import com.gianlu.commonutils.misc.SuperTextView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ServerSheet extends ThemedModalBottomSheet<Server, SparseServers> {
    private final GeoIP ipApi = GeoIP.get();
    private Server currentServer;
    private SuperTextView downloadSpeed;
    private LineChart chart;
    private SuperTextView currentUri;
    private SuperTextView uri;
    private IPDetailsView ipDetails;

    @NonNull
    public static ServerSheet get() {
        return new ServerSheet();
    }

    @Override
    protected int getCustomTheme(@NonNull Server payload) {
        return R.style.AppTheme_NoActionBar_URI;
    }

    @Override
    protected void onReceivedUpdate(@NonNull SparseServers payload) {
        Server updatedServer = payload.find(currentServer);
        if (updatedServer != null) {
            currentServer = updatedServer;
            update(updatedServer);
        }
    }

    private void update(@NonNull Server server) {
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

    @Override
    protected void onCreateHeader(@NonNull LayoutInflater inflater, @NonNull ModalBottomSheetHeaderView parent, @NonNull Server server) {
        inflater.inflate(R.layout.sheet_header_server, parent, true);
        parent.setBackgroundColorRes(R.color.colorSecondary_light);

        downloadSpeed = parent.findViewById(R.id.serverSheet_downloadSpeed);

        TextView title = parent.findViewById(R.id.serverSheet_title);
        title.setText(server.getShortUri());
    }

    @Override
    protected void onCreateBody(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @NonNull Server server) {
        inflater.inflate(R.layout.sheet_server, parent, true);
        chart = parent.findViewById(R.id.serverSheet_chart);
        currentUri = parent.findViewById(R.id.serverSheet_currentUri);
        uri = parent.findViewById(R.id.serverSheet_uri);
        ipDetails = parent.findViewById(R.id.serverSheet_ipDetails);
        ipDetails.setVisibility(View.GONE);

        currentServer = server;

        Utils.setupChart(chart, true);
        update(server);

        String host = server.uri.getHost();
        if (host != null) {
            ipApi.getIPDetails(host, getActivity(), new GeoIP.OnIpDetails() {
                @Override
                public void onDetails(@NonNull IPDetails details) {
                    ipDetails.setup(details);
                    ipDetails.setVisibility(View.VISIBLE);
                }

                @Override
                public void onException(@NonNull Exception ex) {
                    ipDetails.setVisibility(View.GONE);
                }
            });
        }

        isLoading(false);
    }

    @Override
    protected boolean onCustomizeAction(@NonNull FloatingActionButton action, @NonNull Server payload) {
        return false;
    }
}
