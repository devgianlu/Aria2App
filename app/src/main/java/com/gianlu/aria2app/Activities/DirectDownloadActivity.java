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

import java.util.Timer;
import java.util.TimerTask;

public class DirectDownloadActivity extends AppCompatActivity implements ServiceConnection, DownloadTasksAdapter.IAdapter {
    private Messenger downloaderMessenger;
    private RecyclerViewLayout layout;
    private TimerTask updater;

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

        updater = new TimerTask() {
            @Override
            public void run() {
                if (downloaderMessenger != null) DownloaderUtils.listDownloads(downloaderMessenger);
            }
        };

        new Timer().scheduleAtFixedRate(updater, 0, 1000);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        updater.cancel();
        updater = null;

        downloaderMessenger = null;

        Toaster.show(this, Utils.Messages.FAILED_LOADING, new Runnable() {
            @Override
            public void run() {
                onBackPressed();
            }
        });
    }

    @Override
    public void onResume(int id) {
        if (downloaderMessenger != null) DownloaderUtils.resumeDownload(downloaderMessenger, id);
    }

    @Override
    public void onPause(int id) {
        if (downloaderMessenger != null) DownloaderUtils.pauseDownload(downloaderMessenger, id);
    }

    @Override
    public void onRestart(int id) {
        if (downloaderMessenger != null) DownloaderUtils.restartDownload(downloaderMessenger, id);
    }

    @Override
    public void onRemove(int id) {
        if (downloaderMessenger != null) DownloaderUtils.removeDownload(downloaderMessenger, id);
    }

    private class InternalBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null) return;

            switch (intent.getAction()) {
                case DownloaderUtils.ACTION_LIST_DOWNLOADS:
                    DownloaderService.DownloadTasks downloads = (DownloaderService.DownloadTasks) intent.getSerializableExtra("downloads");
                    layout.loadListData(new DownloadTasksAdapter(DirectDownloadActivity.this, downloads, DirectDownloadActivity.this));
                    break;
            }
        }
    }
}
