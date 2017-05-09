package com.gianlu.aria2app.Activities;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;

import com.gianlu.aria2app.Activities.DirectDownload.DownloadSupervisor;
import com.gianlu.aria2app.Adapters.DirectDownloadsAdapter;
import com.gianlu.aria2app.R;

public class DirectDownloadActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direct_download);
        setTitle(R.string.directDownload);

        final SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) findViewById(R.id.directDownload_swipeLayout);
        final LinearLayout noItems = (LinearLayout) findViewById(R.id.directDownload_noItems);
        final RecyclerView list = (RecyclerView) findViewById(R.id.directDownload_list);
        list.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        final DirectDownloadsAdapter adapter = new DirectDownloadsAdapter(this, DownloadSupervisor.getInstance().getDownloads());
        list.setAdapter(adapter);

        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                adapter.notifyDataSetChanged();
                swipeLayout.setRefreshing(false);
            }
        });

        DownloadSupervisor.getInstance().attachListener(new DownloadSupervisor.IListener() {
            @Override
            public Context onUpdateAdapter(final int count) {
                DirectDownloadActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();

                        if (count == 0) {
                            swipeLayout.setVisibility(View.GONE);
                            noItems.setVisibility(View.VISIBLE);
                        } else {
                            swipeLayout.setVisibility(View.VISIBLE);
                            noItems.setVisibility(View.GONE);
                        }
                    }
                });

                return DirectDownloadActivity.this;
            }
        });
    }
}
