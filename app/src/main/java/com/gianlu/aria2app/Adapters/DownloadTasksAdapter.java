package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gianlu.aria2app.Downloader.DownloadTask;
import com.gianlu.aria2app.Downloader.DownloaderService;
import com.gianlu.aria2app.R;

public class DownloadTasksAdapter extends RecyclerView.Adapter<DownloadTasksAdapter.ViewHolder> {
    private final DownloaderService.DownloadTasks tasks;
    private final LayoutInflater inflater;

    public DownloadTasksAdapter(Context context, DownloaderService.DownloadTasks tasks) {
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

        holder.status.setText(task.status.name());
        holder.title.setText(task.task.getName());
        holder.uri.setText(task.task.uri.toString());
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView status;
        final TextView title;
        final TextView uri;

        public ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.direct_download_item, parent, false));

            status = itemView.findViewById(R.id.directDownloadItem_status);
            title = itemView.findViewById(R.id.directDownloadItem_title);
            uri = itemView.findViewById(R.id.directDownloadItem_uri);
        }
    }
}
