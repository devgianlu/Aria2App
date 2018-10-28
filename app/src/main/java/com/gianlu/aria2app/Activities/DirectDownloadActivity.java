package com.gianlu.aria2app.Activities;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;

import com.gianlu.aria2app.Adapters.DirectDownloadsAdapter;
import com.gianlu.aria2app.NetIO.Downloader.FetchHelper;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.commonutils.SuppressingLinearLayoutManager;
import com.gianlu.commonutils.Toaster;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2core.DownloadBlock;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DirectDownloadActivity extends ActivityWithDialog implements FetchHelper.FetchEventListener {
    private RecyclerViewLayout layout;
    private FetchHelper helper;
    private DirectDownloadsAdapter adapter;

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
                helper.reloadListener(DirectDownloadActivity.this);
            }
        }, R.color.colorAccent, R.color.colorMetalink, R.color.colorTorrent);

        layout.startLoading();

        helper = FetchHelper.get(this);
        helper.addListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        helper.removeListener(this);
    }

    @Override
    public void onDownloads(List<Download> downloads) {
        adapter = new DirectDownloadsAdapter(this, downloads, new RestartListener());
        layout.loadListData(adapter);
        countUpdated();
    }

    private void countUpdated() {
        if (adapter != null) {
            if (adapter.getItemCount() == 0) layout.showInfo(R.string.noDirectDownloads);
            else layout.showList();
        }
    }

    @Override
    public void onAdded(@NotNull Download download) {
        if (adapter != null) adapter.add(download);
    }

    @Override
    public void onCancelled(@NotNull Download download) {
        if (adapter != null) adapter.update(download);
    }

    @Override
    public void onCompleted(@NotNull Download download) {
        if (adapter != null) adapter.update(download);
    }

    @Override
    public void onDeleted(@NotNull Download download) {
        onRemoved(download);
    }

    @Override
    public void onDownloadBlockUpdated(@NotNull Download download, @NotNull DownloadBlock downloadBlock, int total) {
    }

    @Override
    public void onError(@NotNull Download download, @NotNull Error error, @Nullable Throwable throwable) {
        if (adapter != null) adapter.update(download);
    }

    @Override
    public void onPaused(@NotNull Download download) {
        if (adapter != null) adapter.update(download);
    }

    @Override
    public void onProgress(@NotNull Download download, long eta, long speed) {
        if (adapter != null) adapter.updateProgress(download, eta, speed);
    }

    @Override
    public void onQueued(@NotNull Download download, boolean waitingOnNetwork) {
        if (adapter != null) adapter.update(download);
    }

    @Override
    public void onRemoved(@NotNull Download download) {
        if (adapter != null) adapter.remove(download);
    }

    @Override
    public void onResumed(@NotNull Download download) {
        if (adapter != null) adapter.update(download);
    }

    @Override
    public void onStarted(@NotNull Download download, @NotNull List<? extends DownloadBlock> list, int total) {
        if (adapter != null) adapter.update(download);
    }

    @Override
    public void onWaitingNetwork(@NotNull Download download) {
        if (adapter != null) adapter.update(download);
    }

    private class RestartListener implements FetchHelper.StartListener {

        @Override
        public void onSuccess() {
            Toaster.with(DirectDownloadActivity.this).message(R.string.downloadRestarted).show();
        }

        @Override
        public void onFailed(Throwable ex) {
            Toaster.with(DirectDownloadActivity.this).message(R.string.failedDownloadingFile).ex(ex).show();
        }
    }
}
