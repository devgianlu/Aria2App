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
import com.gianlu.aria2app.Downloader.DownloadStartConfig;
import com.gianlu.aria2app.Downloader.DownloadTask;
import com.gianlu.aria2app.Downloader.DownloaderUtils;
import com.gianlu.aria2app.GeneralFileProvider;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.commonutils.Toaster;

import java.util.List;

public class DirectDownloadActivity extends AppCompatActivity implements ServiceConnection, DownloadTasksAdapter.IAdapter {
    private Messenger downloaderMessenger;
    private RecyclerViewLayout layout;
    private DownloadTasksAdapter adapter;
    private InternalBroadcastReceiver receiver;

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

        layout.showMessage(R.string.noDirectDownloads, false);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        downloaderMessenger = new Messenger(service);
        DownloaderUtils.listDownloads(downloaderMessenger);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (receiver == null) {
            receiver = new InternalBroadcastReceiver();
            DownloaderUtils.registerReceiver(this, receiver, true);
        }

        DownloaderUtils.bindService(this, this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        DownloaderUtils.unbindService(this, this);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        DownloaderUtils.unregisterReceiver(this, receiver);
        receiver = null;

        downloaderMessenger = null;
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

    @Override
    public void onOpen(int id) {
        if (downloaderMessenger != null) DownloaderUtils.getDownload(downloaderMessenger, id);
    }

    private class InternalBroadcastReceiver extends BroadcastReceiver {

        @Override
        @SuppressWarnings("unchecked")
        public void onReceive(Context context, final Intent intent) {
            if (intent.getAction() == null) return;

            switch (intent.getAction()) {
                case DownloaderUtils.ACTION_COUNT_CHANGED:
                    if (intent.getIntExtra("count", 0) <= 0)
                        layout.showMessage(R.string.noDirectDownloads, false);
                    break;
                case DownloaderUtils.ACTION_LIST_DOWNLOADS:
                    List<DownloadTask> downloads = (List<DownloadTask>) intent.getSerializableExtra("downloads");
                    if (downloads.isEmpty()) {
                        layout.showMessage(R.string.noDirectDownloads, false);
                    } else {
                        adapter = new DownloadTasksAdapter(DirectDownloadActivity.this, downloads, DirectDownloadActivity.this);
                        layout.loadListData(adapter);
                    }
                    break;
                case DownloaderUtils.ACTION_ITEM_INSERTED:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (adapter != null)
                                adapter.addItemAndNotifyItemInserted((DownloadTask) intent.getSerializableExtra("item"));
                        }
                    });
                    break;
                case DownloaderUtils.ACTION_ITEM_REMOVED:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (adapter != null)
                                adapter.removeItemAndNotifyItemRemoved(intent.getIntExtra("pos", -1));
                        }
                    });
                    break;
                case DownloaderUtils.ACTION_ITEM_CHANGED:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (adapter != null)
                                adapter.notifyItemChanged(intent.getIntExtra("pos", -1), intent.getSerializableExtra("item"));
                        }
                    });
                    break;
                case DownloaderUtils.ACTION_FAILED_RESTARTING:
                case DownloaderUtils.ACTION_FAILED_RESUMING:
                    Exception ex = (Exception) intent.getSerializableExtra("ex");
                    if (ex instanceof DownloadStartConfig.CannotCreateStartConfigException)
                        ((DownloadStartConfig.CannotCreateStartConfigException) ex).showAppropriateToast(DirectDownloadActivity.this);
                    else
                        Toaster.show(DirectDownloadActivity.this, Utils.Messages.FAILED_DOWNLOAD_FILE, ex);
                    break;
                case DownloaderUtils.ACTION_GET_DOWNLOAD:
                    DownloadTask task = (DownloadTask) intent.getSerializableExtra("task");
                    if (task == null)
                        Toaster.show(DirectDownloadActivity.this, Utils.Messages.FAILED_OPENING_DOWNLOAD);
                    else
                        startActivity(new Intent(Intent.ACTION_VIEW, GeneralFileProvider.getUriForFile(DirectDownloadActivity.this, "com.gianlu.aria2app", task.task.destFile)).setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
                    break;
            }
        }
    }
}
