package com.gianlu.aria2app.Activities.MoreAboutDownload;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.gianlu.aria2app.NetIO.OnRefresh;
import com.gianlu.aria2app.NetIO.UpdaterFragment;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.MaterialColors;
import com.gianlu.commonutils.NiceBaseBottomSheet;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.commonutils.SuppressingLinearLayoutManager;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;
import java.util.List;

public abstract class PeersServersFragment<A extends RecyclerView.Adapter<?>, S extends NiceBaseBottomSheet> extends UpdaterFragment implements OnBackPressed {
    protected TopCountriesView topCountries;
    protected RecyclerViewLayout recyclerViewLayout;
    protected S sheet;
    protected A adapter;
    private PieChart topDownloadChart;
    private PieChart topUploadChart;

    private void setupChart(PieChart chart, @StringRes int noDataRes) {
        if (getContext() == null) return;

        chart.clear();

        chart.setDescription(null);
        chart.setNoDataText(getString(noDataRes));
        chart.setNoDataTextColor(ContextCompat.getColor(getContext(), android.R.color.tertiary_text_light));
        chart.setBackgroundColor(Color.alpha(0));
        chart.setTouchEnabled(false);
        chart.setDrawHoleEnabled(false);
        chart.setDrawCenterText(false);
        chart.getLegend().setEnabled(false);

        chart.setData(new PieData(initDataSet()));
        chart.invalidate();
    }

    private PieDataSet initDataSet() {
        PieDataSet set = new PieDataSet(new ArrayList<PieEntry>(), null);
        set.setDrawValues(false);
        set.setColors(MaterialColors.getShuffledInstance().getColorsRes(), getContext());
        set.setValueTextSize(16);
        return set;
    }

    protected void reloadTopCountriesCharts() {
        List<PieEntry> downloadEntries = topCountries.getDownloadPieEntries();
        if (downloadEntries.isEmpty()) {
            topDownloadChart.clear();
        } else {
            if (topDownloadChart.getData() == null)
                setupChart(topDownloadChart, R.string.noDownloadActivity);

            PieDataSet downloadSet = (PieDataSet) topDownloadChart.getData().getDataSet();

            downloadSet.setValues(downloadEntries);
            downloadSet.calcMinMax();
            topDownloadChart.notifyDataSetChanged();
            topDownloadChart.invalidate();
        }

        List<PieEntry> uploadEntries = topCountries.getUploadPieEntries();
        if (uploadEntries.isEmpty()) {
            topUploadChart.clear();
        } else {
            if (topUploadChart.getData() == null)
                setupChart(topUploadChart, R.string.noUploadActivity);

            PieDataSet uploadSet = (PieDataSet) topUploadChart.getData().getDataSet();

            uploadSet.setValues(uploadEntries);
            uploadSet.calcMinMax();
            topUploadChart.notifyDataSetChanged();
            topUploadChart.invalidate();
        }
    }

    protected abstract A getAdapter(@NonNull Context context);

    protected abstract S getSheet(@NonNull CoordinatorLayout layout);

    @Override
    public final boolean canGoBack(int code) {
        if (code == CODE_CLOSE_SHEET) {
            if (sheet != null) sheet.collapse();
            return true;
        }

        if (sheet != null && sheet.isExpanded()) {
            sheet.collapse();
            return false;
        } else {
            return true;
        }
    }

    @Override
    public final void onBackPressed() {
        stopUpdater();
    }

    @Nullable
    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getContext() == null) return null;
        CoordinatorLayout layout = (CoordinatorLayout) inflater.inflate(R.layout.fragment_peers_and_servers, container, false);
        topCountries = layout.findViewById(R.id.peersServersFragment_topCountries);
        recyclerViewLayout = layout.findViewById(R.id.peersServersFragment_recyclerViewLayout);
        recyclerViewLayout.enableSwipeRefresh(R.color.colorAccent, R.color.colorMetalink, R.color.colorTorrent);
        recyclerViewLayout.setLayoutManager(new SuppressingLinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        adapter = getAdapter(getContext());
        recyclerViewLayout.loadListData(adapter);
        recyclerViewLayout.startLoading();

        sheet = getSheet(layout);
        recyclerViewLayout.setRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh(new OnRefresh() {
                    @Override
                    public void refreshed() {
                        adapter = getAdapter(getContext());
                        recyclerViewLayout.loadListData(adapter);
                        recyclerViewLayout.startLoading();
                    }
                });
            }
        });

        final ImageButton toggleTopCountries = layout.findViewById(R.id.peersServersFragment_toggleTopCountries);
        final LinearLayout topCountriesDetails = layout.findViewById(R.id.peersServersFragment_topCountriesDetails);
        toggleTopCountries.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CommonUtils.handleCollapseClick(toggleTopCountries, topCountriesDetails);
            }
        });

        topDownloadChart = layout.findViewById(R.id.peersServersFragment_topDownloadChart);
        setupChart(topDownloadChart, R.string.noDownloadActivity);
        topUploadChart = layout.findViewById(R.id.peersServersFragment_topUploadChart);
        setupChart(topUploadChart, R.string.noUploadActivity);

        return layout;
    }
}
