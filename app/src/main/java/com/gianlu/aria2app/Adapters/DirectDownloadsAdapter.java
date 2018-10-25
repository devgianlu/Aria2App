package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gianlu.aria2app.R;
import com.gianlu.commonutils.SuperTextView;
import com.tonyodev.fetch2.Download;

import java.util.List;

public class DirectDownloadsAdapter extends RecyclerView.Adapter<DirectDownloadsAdapter.ViewHolder> {
    private final LayoutInflater inflater;
    private final List<Download> downloads;

    public DirectDownloadsAdapter(Context context, List<Download> downloads) {
        this.inflater = LayoutInflater.from(context);
        this.downloads = downloads;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Download download = downloads.get(position);

        holder.title.setText(download.getFile());
        holder.uri.setText(download.getUrl());
    }

    @Override
    public int getItemCount() {
        return downloads.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final SuperTextView status;
        final TextView title;
        final TextView uri;


        public ViewHolder(@NonNull ViewGroup parent) {
            super(inflater.inflate(R.layout.item_direct_download, parent, false));

            status = itemView.findViewById(R.id.directDownloadItem_status);
            title = itemView.findViewById(R.id.directDownloadItem_title);
            uri = itemView.findViewById(R.id.directDownloadItem_uri);
        }
    }
}
