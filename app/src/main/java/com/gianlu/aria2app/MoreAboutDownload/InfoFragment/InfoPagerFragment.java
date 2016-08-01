package com.gianlu.aria2app.MoreAboutDownload.InfoFragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.gianlu.aria2app.DownloadsListing.Charting;
import com.gianlu.aria2app.R;
import com.github.mikephil.charting.charts.LineChart;

public class InfoPagerFragment extends Fragment {
    private UpdateUI updateUI;
    private ViewHolder holder;

    public static InfoPagerFragment newInstance(String title, String gid) {
        InfoPagerFragment fragment = new InfoPagerFragment();

        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("gid", gid);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        updateUI = new UpdateUI(getActivity(), getArguments().getString("gid"), holder);
        new Thread(updateUI).start();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        holder = new ViewHolder(inflater.inflate(R.layout.info_fragment, container, false));
        holder.rootView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // TODO: Not working at all
            }
        });
        holder.chartRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.chart = Charting.setupChart(holder.chart);
            }
        });

        holder.chart = Charting.setupChart(holder.chart);

        return holder.rootView;
    }

    public class ViewHolder {
        public SwipeRefreshLayout rootView;
        public LineChart chart;
        public ImageButton chartRefresh;
        public TextView gid;
        public TextView totalLength;
        public TextView completedLength;
        public TextView uploadLength;

        public ViewHolder(View rootView) {
            this.rootView = (SwipeRefreshLayout) rootView;

            chart = (LineChart) rootView.findViewById(R.id.infoFragment_chart);
            chartRefresh = (ImageButton) rootView.findViewById(R.id.infoFragment_chartRefresh);
            gid = (TextView) rootView.findViewById(R.id.infoFragment_gid);
            totalLength = (TextView) rootView.findViewById(R.id.infoFragment_totalLength);
            completedLength = (TextView) rootView.findViewById(R.id.infoFragment_completedLength);
            uploadLength = (TextView) rootView.findViewById(R.id.infoFragment_uploadLength);
        }
    }
}
