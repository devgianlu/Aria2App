package com.gianlu.aria2app.Activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;

import com.gianlu.aria2app.Adapters.DownloadTasksAdapter;
import com.gianlu.aria2app.Downloader.DownloaderService;
import com.gianlu.aria2app.Downloader.DownloaderUtils;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.commonutils.Toaster;

public class DirectDownloadActivity extends AppCompatActivity implements ServiceConnection {
    private Messenger downloaderMessenger;
    private RecyclerViewLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        layout = new RecyclerViewLayout(this);
        setContentView(layout);
        setTitle(R.string.directDownload);

        layout.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        layout.getList().addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        layout.enableSwipeRefresh(R.color.colorAccent, R.color.colorMetalink, R.color.colorTorrent);
        layout.setRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (downloaderMessenger != null) DownloaderUtils.listDownloads(downloaderMessenger);
            }
        });

        DownloaderUtils.registerReceiver(this, new InternalBroadcastReceiver());
        DownloaderUtils.bindService(this, this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        downloaderMessenger = new Messenger(service);
        DownloaderUtils.listDownloads(downloaderMessenger);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        downloaderMessenger = null;

        Toaster.show(this, Utils.Messages.FAILED_LOADING, new Runnable() {
            @Override
            public void run() {
                onBackPressed();
            }
        });
    }

    private class InternalBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null) return;

            switch (intent.getAction()) {
                case DownloaderUtils.ACTION_LIST_DOWNLOADS:
                    DownloaderService.DownloadTasks downloads = (DownloaderService.DownloadTasks) intent.getSerializableExtra("downloads");
                    layout.loadListData(new DownloadTasksAdapter(DirectDownloadActivity.this, downloads));
                    break;
            }
        }
    }
}
