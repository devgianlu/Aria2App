package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gianlu.aria2app.NetIO.Downloader.FetchDownloadWrapper;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.FontsManager;
import com.tonyodev.fetch2.Download;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public class DirectDownloadsAdapter extends RecyclerView.Adapter<DirectDownloadsAdapter.ViewHolder> {
    private final LayoutInflater inflater;
    private final List<FetchDownloadWrapper> downloads;
    private final Context context;

    public DirectDownloadsAdapter(Context context, List<Download> downloads) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.downloads = FetchDownloadWrapper.wrap(downloads);
    }

    private static void updateDownloadInfo(ViewHolder holder, FetchDownloadWrapper download) {
        long eta = download.getEta();
        if (eta == -1) {
            holder.remainingTime.setVisibility(View.GONE);
        } else {
            holder.remainingTime.setVisibility(View.VISIBLE);
            holder.remainingTime.setText(CommonUtils.timeFormatter(download.getEta()));
        }

        int progress = download.getProgress();
        if (progress == -1) {
            holder.progress.setIndeterminate(true);
            holder.percentage.setVisibility(View.GONE);
        } else {
            holder.progress.setProgress(progress);
            holder.percentage.setVisibility(View.VISIBLE);
            holder.percentage.setText(String.format(Locale.getDefault(), "%d%%", progress));
        }

        holder.speed.setText(CommonUtils.speedFormatter(download.getSpeed(), false));
        holder.downloaded.setText(CommonUtils.dimensionFormatter(download.getDownloaded(), false));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) { // TODO: Can improve performance
        FetchDownloadWrapper download = downloads.get(position);

        holder.title.setText(download.getName());
        holder.uri.setText(download.getDecodedUrl());
        holder.progress.setMax(100);

        updateDownloadInfo(holder, download);

        switch (download.getStatus()) {
            case QUEUED:
                holder.status.setText(R.string.pending);
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.downloadPending));

                holder.open.setVisibility(View.GONE);
                holder.start.setVisibility(View.GONE);
                holder.pause.setVisibility(View.GONE);
                holder.restart.setVisibility(View.GONE);
                holder.remove.setVisibility(View.VISIBLE);
                break;
            case DOWNLOADING:
                holder.status.setText(R.string.running);
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.downloadRunning));

                holder.open.setVisibility(View.GONE);
                holder.start.setVisibility(View.GONE);
                holder.pause.setVisibility(View.VISIBLE);
                holder.restart.setVisibility(View.GONE);
                holder.remove.setVisibility(View.VISIBLE);
                break;
            case PAUSED:
                holder.status.setText(R.string.paused);
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.downloadPaused));

                holder.open.setVisibility(View.GONE);
                holder.start.setVisibility(View.VISIBLE);
                holder.pause.setVisibility(View.GONE);
                holder.restart.setVisibility(View.GONE);
                holder.remove.setVisibility(View.VISIBLE);
                break;
            case COMPLETED:
                holder.status.setText(R.string.completed);
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.downloadCompleted));

                holder.open.setVisibility(View.VISIBLE);
                holder.start.setVisibility(View.GONE);
                holder.pause.setVisibility(View.GONE);
                holder.restart.setVisibility(View.GONE);
                holder.remove.setVisibility(View.VISIBLE);
                break;
            case FAILED:
                holder.status.setText(R.string.failed);
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.downloadFailed));

                holder.open.setVisibility(View.GONE);
                holder.start.setVisibility(View.GONE);
                holder.pause.setVisibility(View.GONE);
                holder.restart.setVisibility(View.VISIBLE);
                holder.remove.setVisibility(View.VISIBLE);
                break;
            case CANCELLED:
                holder.status.setText(R.string.cancelled);
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.downloadCancelled));

                holder.open.setVisibility(View.GONE);
                holder.start.setVisibility(View.GONE);
                holder.pause.setVisibility(View.GONE);
                holder.restart.setVisibility(View.GONE);
                holder.remove.setVisibility(View.VISIBLE);
                break;
            case REMOVED:
            case DELETED:
            case ADDED:
            case NONE:
                holder.status.setText(R.string.unknown);
                holder.status.setTextColor(Color.WHITE);
                break;
        }

        // TODO: Add button listeners
    }

    @Override
    public int getItemCount() {
        return downloads.size();
    }

    private int indexOf(@NotNull Download download) {
        for (int i = 0; i < downloads.size(); i++)
            if (downloads.get(i).is(download))
                return i;

        return -1;
    }

    @UiThread
    public void add(Download download) {
        downloads.add(FetchDownloadWrapper.wrap(download));
        notifyItemInserted(downloads.size() - 1);
    }

    @UiThread
    public void remove(Download download) {
        int index = indexOf(download);
        if (index != -1) {
            downloads.remove(index);
            notifyItemRemoved(index);
        }
    }

    @UiThread
    public void update(@NotNull Download download) {
        int index = indexOf(download);
        if (index != -1) {
            downloads.get(index).set(download);
            notifyItemChanged(index);
        }
    }

    @UiThread
    public void updateProgress(@NotNull Download download, long eta, long speed) {
        int index = indexOf(download);
        if (index != -1) {
            FetchDownloadWrapper wrapper = downloads.get(index);
            wrapper.set(download);
            wrapper.setEta(eta);
            wrapper.setSpeed(speed);
            notifyItemChanged(index);
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView status;
        final TextView title;
        final TextView uri;
        final ProgressBar progress;
        final TextView percentage;
        final ImageButton open;
        final ImageButton start;
        final ImageButton pause;
        final ImageButton restart;
        final ImageButton remove;
        final TextView downloaded;
        final TextView speed;
        final TextView remainingTime;

        public ViewHolder(@NonNull ViewGroup parent) {
            super(inflater.inflate(R.layout.item_direct_download, parent, false));

            status = itemView.findViewById(R.id.directDownloadItem_status);
            FontsManager.set(status, FontsManager.ROBOTO_BOLD);
            title = itemView.findViewById(R.id.directDownloadItem_title);
            uri = itemView.findViewById(R.id.directDownloadItem_uri);
            progress = itemView.findViewById(R.id.directDownloadItem_progress);
            percentage = itemView.findViewById(R.id.directDownloadItem_percentage);
            start = itemView.findViewById(R.id.directDownloadItem_start);
            pause = itemView.findViewById(R.id.directDownloadItem_pause);
            restart = itemView.findViewById(R.id.directDownloadItem_restart);
            remove = itemView.findViewById(R.id.directDownloadItem_remove);
            open = itemView.findViewById(R.id.directDownloadItem_open);
            downloaded = itemView.findViewById(R.id.directDownloadItem_downloaded);
            speed = itemView.findViewById(R.id.directDownloadItem_speed);
            remainingTime = itemView.findViewById(R.id.directDownloadItem_remainingTime);
        }
    }
}
