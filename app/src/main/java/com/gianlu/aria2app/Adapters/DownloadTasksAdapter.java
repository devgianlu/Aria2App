package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

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

    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.direct_download_item, parent, false));
        }
    }
}
