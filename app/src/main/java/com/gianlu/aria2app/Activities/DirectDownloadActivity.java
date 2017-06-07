package com.gianlu.aria2app.Activities;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;

import com.gianlu.aria2app.Activities.DirectDownload.UpdateUI;
import com.gianlu.aria2app.Adapters.DirectDownloadsAdapter;
import com.gianlu.aria2app.NetIO.BaseUpdater;
import com.gianlu.aria2app.NetIO.DownloadsManager.DownloadsManager;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.MessageLayout;

public class DirectDownloadActivity extends AppCompatActivity implements UpdateUI.IUI {
    private DirectDownloadsAdapter adapter;
    private UpdateUI updater;
    private FrameLayout layout;
    private RecyclerView list;
    private SwipeRefreshLayout swipeRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direct_download);
        setTitle(R.string.directDownload);

        layout = (FrameLayout) findViewById(R.id.directDownload_container);
        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.directDownload_swipeRefresh);
        swipeRefresh.setColorSchemeResources(R.color.colorAccent, R.color.colorMetalink, R.color.colorTorrent);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updater.stopThread(new BaseUpdater.IThread() {
                    @Override
                    public void onStopped() {
                        adapter = new DirectDownloadsAdapter(DirectDownloadActivity.this, DownloadsManager.get(DirectDownloadActivity.this));
                        list.setAdapter(adapter);
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });

        list = (RecyclerView) findViewById(R.id.directDownload_list);
        list.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        list.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        adapter = new DirectDownloadsAdapter(this, DownloadsManager.get(this));
        list.setAdapter(adapter);

        updater = new UpdateUI(this, this);
        updater.start();
    }

    @Override
    public void onBackPressed() {
        if (updater != null) updater.stopThread(null);
        super.onBackPressed();
    }

    @Override
    public void onRefresh() {
        if (adapter != null) {
            if (adapter.refresh() == 0) {
                MessageLayout.show(layout, R.string.directDownload_noItems, R.drawable.ic_info_black_24dp);
                list.setVisibility(View.GONE);
            } else {
                MessageLayout.hide(layout);
                list.setVisibility(View.VISIBLE);
            }
        }
    }
}
