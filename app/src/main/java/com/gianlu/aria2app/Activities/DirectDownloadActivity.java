package com.gianlu.aria2app.Activities;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;

import com.gianlu.aria2app.R;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.commonutils.SuppressingLinearLayoutManager;

public class DirectDownloadActivity extends ActivityWithDialog {
    private RecyclerViewLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        layout = new RecyclerViewLayout(this);
        setContentView(layout);
        setTitle(R.string.directDownload);

        layout.setLayoutManager(new SuppressingLinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        layout.getList().addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        layout.enableSwipeRefresh(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // TODO
            }
        }, R.color.colorAccent, R.color.colorMetalink, R.color.colorTorrent);

        layout.showInfo(R.string.noDirectDownloads);
    }
}
