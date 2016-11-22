package com.gianlu.aria2app;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

public class DirectDownloadActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direct_download);
        setTitle(R.string.directDownload);

        SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) findViewById(R.id.directDownload_swipeLayout);
        RecyclerView list = (RecyclerView) findViewById(R.id.directDownload_list);
        list.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
    }
}
