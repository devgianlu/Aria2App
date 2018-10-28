package com.gianlu.aria2app.Adapters;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gianlu.aria2app.NetIO.Downloader.FetchDownloadWrapper;
import com.gianlu.aria2app.NetIO.Downloader.FetchHelper;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.FontsManager;
import com.gianlu.commonutils.Toaster;
import com.tonyodev.fetch2.Download;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public class DirectDownloadsAdapter extends RecyclerView.Adapter<DirectDownloadsAdapter.ViewHolder> {
    private final LayoutInflater inflater;
    private final List<FetchDownloadWrapper> downloads;
    private final Context context;
    private final FetchHelper helper;
    private final FetchHelper.StartListener restartListener;

    public DirectDownloadsAdapter(Context context, List<Download> downloads, FetchHelper.StartListener restartListener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.downloads = FetchDownloadWrapper.wrap(downloads);
        this.helper = FetchHelper.get(context);
        this.restartListener = restartListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    private void updateViewHolder(@NonNull ViewHolder holder, int pos, @Nullable FetchDownloadWrapper.ProgressBundle bundle) {
        FetchDownloadWrapper download = downloads.get(pos);

        long eta = bundle == null ? download.getEta() : bundle.eta;
        if (eta == -1) {
            holder.remainingTime.setVisibility(View.GONE);
        } else {
            holder.remainingTime.setVisibility(View.VISIBLE);
            holder.remainingTime.setText(CommonUtils.timeFormatter(eta / 1000));
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

        long speed = bundle == null ? download.getSpeed() : bundle.speed;
        holder.speed.setText(CommonUtils.speedFormatter(speed, false));
        holder.downloaded.setText(CommonUtils.dimensionFormatter(download.getDownloaded(), false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
            return;
        }

        FetchDownloadWrapper.ProgressBundle bundle = (FetchDownloadWrapper.ProgressBundle) payloads.get(0);
        updateViewHolder(holder, position, bundle);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final FetchDownloadWrapper download = downloads.get(position);

        holder.title.setText(download.getName());
        holder.uri.setText(download.getDecodedUrl());
        holder.progress.setMax(100);

        updateViewHolder(holder, position, null);

        switch (download.getStatus()) {
            case QUEUED:
                holder.status.setText(R.string.pending);
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.downloadPending));

                holder.remainingTime.setVisibility(View.GONE);
                holder.speed.setVisibility(View.GONE);

                holder.open.setVisibility(View.GONE);
                holder.start.setVisibility(View.GONE);
                holder.pause.setVisibility(View.GONE);
                holder.restart.setVisibility(View.GONE);
                holder.remove.setVisibility(View.VISIBLE);
                break;
            case DOWNLOADING:
                holder.status.setText(R.string.running);
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.downloadRunning));

                holder.remainingTime.setVisibility(View.VISIBLE);
                holder.speed.setVisibility(View.VISIBLE);

                holder.open.setVisibility(View.GONE);
                holder.start.setVisibility(View.GONE);
                holder.pause.setVisibility(View.VISIBLE);
                holder.restart.setVisibility(View.GONE);
                holder.remove.setVisibility(View.VISIBLE);
                break;
            case PAUSED:
                holder.status.setText(R.string.paused);
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.downloadPaused));

                holder.remainingTime.setVisibility(View.GONE);
                holder.speed.setVisibility(View.GONE);

                holder.open.setVisibility(View.GONE);
                holder.start.setVisibility(View.VISIBLE);
                holder.pause.setVisibility(View.GONE);
                holder.restart.setVisibility(View.GONE);
                holder.remove.setVisibility(View.VISIBLE);
                break;
            case COMPLETED:
                holder.status.setText(R.string.completed);
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.downloadCompleted));

                holder.remainingTime.setVisibility(View.GONE);
                holder.speed.setVisibility(View.GONE);

                holder.open.setVisibility(View.VISIBLE);
                holder.start.setVisibility(View.GONE);
                holder.pause.setVisibility(View.GONE);
                holder.restart.setVisibility(View.GONE);
                holder.remove.setVisibility(View.VISIBLE);
                break;
            case FAILED:
                holder.status.setText(R.string.failed);
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.downloadFailed));

                holder.remainingTime.setVisibility(View.GONE);
                holder.speed.setVisibility(View.GONE);

                holder.open.setVisibility(View.GONE);
                holder.start.setVisibility(View.GONE);
                holder.pause.setVisibility(View.GONE);
                holder.restart.setVisibility(View.VISIBLE);
                holder.remove.setVisibility(View.VISIBLE);
                break;
            case CANCELLED:
                holder.status.setText(R.string.cancelled);
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.downloadCancelled));

                holder.remainingTime.setVisibility(View.GONE);
                holder.speed.setVisibility(View.GONE);

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

        holder.open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFile(download);
            }
        });
        holder.start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                helper.resume(download);
            }
        });
        holder.pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                helper.pause(download);
            }
        });
        holder.restart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                helper.restart(download, restartListener);
            }
        });
        holder.remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                helper.remove(download);
            }
        });
    }

    private void openFile(FetchDownloadWrapper download) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, FileProvider.getUriForFile(context, "com.gianlu.aria2app", download.getFile()))
                    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
        } catch (ActivityNotFoundException ex) {
            Toaster.with(context).message(R.string.failedOpeningDownload).ex(ex).show();
        }
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
            notifyItemChanged(index, wrapper.progress(eta, speed));
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
