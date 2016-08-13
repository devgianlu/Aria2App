package com.gianlu.aria2app.MoreAboutDownload.InfoFragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gianlu.aria2app.MoreAboutDownload.CommonFragment;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.github.mikephil.charting.charts.LineChart;

public class InfoPagerFragment extends CommonFragment {
    private UpdateUI updateUI;
    private UpdateUI.IDownloadStatusObserver pendingObserver;
    private ViewHolder holder;

    public static InfoPagerFragment newInstance(String title, String gid) {
        InfoPagerFragment fragment = new InfoPagerFragment();

        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("gid", gid);

        fragment.setArguments(args);
        return fragment;
    }

    public InfoPagerFragment setStatusObserver(UpdateUI.IDownloadStatusObserver observer) {
        if (updateUI == null)
            pendingObserver = observer;
        else
            updateUI.setStatusObserver(observer);

        return this;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        updateUI = new UpdateUI(getActivity(), getArguments().getString("gid"), holder);
        if (pendingObserver != null) updateUI.setStatusObserver(pendingObserver);
        new Thread(updateUI).start();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        holder = new ViewHolder(inflater.inflate(R.layout.info_fragment, container, false));

        holder.chartRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.chart = Utils.setupChart(holder.chart, false);
            }
        });

        holder.chart = Utils.setupChart(holder.chart, false);

        return holder.rootView;
    }

    @Override
    public void stopUpdater() {
        UpdateUI.stop(updateUI);
    }

    public class ViewHolder {
        public View rootView;
        public LineChart chart;
        public ImageButton chartRefresh;
        public TextView gid;
        public TextView totalLength;
        public TextView completedLength;
        public TextView uploadLength;
        public TextView pieceLength;
        public TextView numPieces;
        public TextView connections;
        public TextView directory;
        public TextView verifiedLength;
        public TextView verifyIntegrityPending;

        public LinearLayout bitTorrentOnly;
        public TextView btMode;
        public TextView btSeeders;
        public TextView btSeeder;
        public TextView btComment;
        public TextView btCreationDate;
        public TextView btInfoHash;
        public LinearLayout btAnnounceList;

        public ViewHolder(View rootView) {
            this.rootView = rootView;

            chart = (LineChart) rootView.findViewById(R.id.infoFragment_chart);
            chartRefresh = (ImageButton) rootView.findViewById(R.id.infoFragment_chartRefresh);
            gid = (TextView) rootView.findViewById(R.id.infoFragment_gid);
            totalLength = (TextView) rootView.findViewById(R.id.infoFragment_totalLength);
            completedLength = (TextView) rootView.findViewById(R.id.infoFragment_completedLength);
            uploadLength = (TextView) rootView.findViewById(R.id.infoFragment_uploadLength);
            pieceLength = (TextView) rootView.findViewById(R.id.infoFragment_pieceLength);
            numPieces = (TextView) rootView.findViewById(R.id.infoFragment_numPieces);
            connections = (TextView) rootView.findViewById(R.id.infoFragment_connections);
            directory = (TextView) rootView.findViewById(R.id.infoFragment_directory);
            verifiedLength = (TextView) rootView.findViewById(R.id.infoFragment_verifiedLength);
            verifyIntegrityPending = (TextView) rootView.findViewById(R.id.infoFragment_verifyIntegrityPending);

            bitTorrentOnly = (LinearLayout) rootView.findViewById(R.id.infoFragment_bitTorrentOnly);
            btMode = (TextView) rootView.findViewById(R.id.infoFragment_btMode);
            btSeeders = (TextView) rootView.findViewById(R.id.infoFragment_btSeeders);
            btSeeder = (TextView) rootView.findViewById(R.id.infoFragment_btSeeder);
            btComment = (TextView) rootView.findViewById(R.id.infoFragment_btComment);
            btCreationDate = (TextView) rootView.findViewById(R.id.infoFragment_btCreationDate);
            btInfoHash = (TextView) rootView.findViewById(R.id.infoFragment_btInfoHash);
            btAnnounceList = (LinearLayout) rootView.findViewById(R.id.infoFragment_btAnnounceList);
        }
    }
}
