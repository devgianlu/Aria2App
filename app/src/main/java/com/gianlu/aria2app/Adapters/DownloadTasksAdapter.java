package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gianlu.aria2app.Downloader.DownloadTask;
import com.gianlu.aria2app.Downloader.DownloaderService;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.SuperTextView;

import java.util.Locale;

public class DownloadTasksAdapter extends RecyclerView.Adapter<DownloadTasksAdapter.ViewHolder> {
    private final Context context;
    private final DownloaderService.DownloadTasks tasks;
    private final LayoutInflater inflater;

    public DownloadTasksAdapter(Context context, DownloaderService.DownloadTasks tasks) {
        this.context = context;
        this.tasks = tasks;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        DownloadTask task = tasks.get(position);

        holder.status.setText(task.status.getFormal(context));
        holder.status.setTextColor(task.status.getColor(context));
        holder.title.setText(task.task.getName());
        holder.uri.setText(task.task.uri.toString());

        if (task.length > 0) {
            float percentage = (float) task.downloaded / (float) task.length * 100;
            holder.progress.setIndeterminate(false);
            holder.progress.setProgress((int) percentage);

            holder.percentage.setText(String.format(Locale.getDefault(), "%.2f %%", percentage));
        } else {
            holder.progress.setIndeterminate(true);
            holder.percentage.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final SuperTextView status;
        final TextView title;
        final TextView uri;
        final ProgressBar progress;
        final TextView percentage;

        public ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.direct_download_item, parent, false));

            status = itemView.findViewById(R.id.directDownloadItem_status);
            status.setTypeface("fonts/Roboto-Bold.ttf");
            title = itemView.findViewById(R.id.directDownloadItem_title);
            uri = itemView.findViewById(R.id.directDownloadItem_uri);
            progress = itemView.findViewById(R.id.directDownloadItem_progress);
            percentage = itemView.findViewById(R.id.directDownloadItem_percentage);
        }
    }
}
