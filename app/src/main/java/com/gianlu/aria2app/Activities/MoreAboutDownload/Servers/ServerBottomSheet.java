package com.gianlu.aria2app.Activities.MoreAboutDownload.Servers;

import android.support.annotation.NonNull;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gianlu.aria2app.NetIO.FreeGeoIP.FreeGeoIPApi;
import com.gianlu.aria2app.NetIO.FreeGeoIP.IPDetails;
import com.gianlu.aria2app.NetIO.FreeGeoIP.IPDetailsView;
import com.gianlu.aria2app.NetIO.JTA2.Server;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.NiceBaseBottomSheet;
import com.gianlu.commonutils.SuperTextView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;

import java.util.List;

public class ServerBottomSheet extends NiceBaseBottomSheet {
    private final FreeGeoIPApi freeGeoIPApi;
    private SuperTextView downloadSpeed;
    private LineChart chart;
    private SuperTextView currentUri;
    private SuperTextView uri;
    private IPDetailsView ipDetails;
    private Server currentServer = null;

    public ServerBottomSheet(ViewGroup parent) {
        super(parent, R.layout.sheet_header_server, R.layout.sheet_server, false);
        freeGeoIPApi = FreeGeoIPApi.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onUpdateViews(Object... payloads) {
        if (currentServer == null) return;

        Server server = Server.find((SparseArray<List<Server>>) payloads[0], currentServer);
        if (server != null) {
            updateHeaderViews(server);
            updateContentViews(server);
        }
    }

    @Override
    protected void cleanUp() {
        currentServer = null;
    }

    @Override
    protected void onCreateHeaderView(@NonNull ViewGroup parent, Object... payloads) {
        downloadSpeed = parent.findViewById(R.id.serverSheet_downloadSpeed);
        TextView title = parent.findViewById(R.id.serverSheet_title);
        Server server = (Server) payloads[0];

        parent.setBackgroundResource(R.color.colorAccent_light);
        title.setText(server.currentUri);
        updateHeaderViews(server);
    }

    private void updateHeaderViews(Server server) {
        downloadSpeed.setText(CommonUtils.speedFormatter(server.downloadSpeed, false));
    }

    private void updateContentViews(Server server) {
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
    protected void onCreateContentView(@NonNull ViewGroup parent, Object... payloads) {
        chart = parent.findViewById(R.id.serverSheet_chart);
        currentUri = parent.findViewById(R.id.serverSheet_currentUri);
        uri = parent.findViewById(R.id.serverSheet_uri);
        ipDetails = parent.findViewById(R.id.serverSheet_ipDetails);
        ipDetails.setVisibility(View.GONE);

        Server server = (Server) payloads[0];
        currentServer = server;

        Utils.setupChart(chart, true, R.color.colorPrimaryDark);
        freeGeoIPApi.getIPDetails(server.getCurrentUri().getHost(), new FreeGeoIPApi.IIPDetails() {
            @Override
            public void onDetails(IPDetails details) {
                ipDetails.setup(details);
                ipDetails.setVisibility(View.VISIBLE);
            }

            @Override
            public void onException(Exception ex) {
                Logging.logMe(ex);
                ipDetails.setVisibility(View.GONE);
            }
        });

        updateContentViews(server);
    }
}
