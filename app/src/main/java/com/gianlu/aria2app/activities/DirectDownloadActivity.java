package com.gianlu.aria2app.activities;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.aria2app.R;
import com.gianlu.aria2app.adapters.DirectDownloadsAdapter;
import com.gianlu.aria2app.downloader.DdDownload;
import com.gianlu.aria2app.downloader.DirectDownloadHelper;
import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.commonutils.ui.Toaster;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DirectDownloadActivity extends ActivityWithDialog implements DirectDownloadHelper.Listener {
    private static final String TAG = DirectDownloadActivity.class.getSimpleName();
    private RecyclerMessageView rmv;
    private DirectDownloadHelper helper;
    private DirectDownloadsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rmv = new RecyclerMessageView(this);
        setContentView(rmv);
        setTitle(R.string.directDownload);

        rmv.linearLayoutManager(RecyclerView.VERTICAL, false);
        rmv.dividerDecoration(RecyclerView.VERTICAL);
        rmv.enableSwipeRefresh(() -> {
            if (helper != null) helper.reloadListener(DirectDownloadActivity.this);
        }, R.color.colorAccent, R.color.colorMetalink, R.color.colorTorrent);

        rmv.startLoading();

        try {
            helper = DirectDownloadHelper.get(this);
            helper.addListener(this);
        } catch (DirectDownloadHelper.DirectDownloadNotEnabledException ex) {
            rmv.showInfo(R.string.noDirectDownloads);
        } catch (DirectDownloadHelper.InitializationException ex) {
            Log.e(TAG, "Failed initializing Fetch.", ex);
            rmv.showError(R.string.failedLoading_reason, ex.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (helper != null) helper.removeListener(this);
    }

    @Override
    public void onDownloads(@NotNull List<DdDownload> downloads) {
        adapter = new DirectDownloadsAdapter(this, helper, downloads, new RestartListener());
        rmv.loadListData(adapter);
        countUpdated();
    }

    private void countUpdated() {
        if (adapter != null) {
            if (adapter.getItemCount() == 0) rmv.showInfo(R.string.noDirectDownloads);
            else rmv.showList();
        }
    }

    @Override
    public void onAdded(@NotNull DdDownload download) {
        if (adapter != null) adapter.add(download);
        countUpdated();
    }

    @Override
    public void onUpdated(@NonNull DdDownload download) {
        if (adapter != null) adapter.update(download);
    }

    @Override
    public void onProgress(@NotNull DdDownload download) {
        if (adapter != null) adapter.updateProgress(download);
    }

    @Override
    public void onRemoved(@NotNull DdDownload download) {
        if (adapter != null) adapter.remove(download);
        countUpdated();
    }

    private class RestartListener implements DirectDownloadHelper.StartListener {

        @Override
        public void onSuccess() {
            Toaster.with(DirectDownloadActivity.this).message(R.string.downloadRestarted).show();
        }

        @Override
        public void onFailed(@NonNull Throwable ex) {
            Log.w(TAG, "Failed restarting download.", ex);
            Toaster.with(DirectDownloadActivity.this).message(R.string.failedDownloadingFile).show();
        }
    }
}
