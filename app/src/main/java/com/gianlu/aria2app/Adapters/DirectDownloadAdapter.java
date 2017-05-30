package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gianlu.aria2app.NetIO.DownloadsManager.DDDownload;
import com.gianlu.aria2app.NetIO.DownloadsManager.DownloadsManager;
import com.gianlu.aria2app.R;

import java.util.List;
import java.util.Locale;

// TODO: Better layout
public class DirectDownloadAdapter extends RecyclerView.Adapter<DirectDownloadAdapter.ViewHolder> {
    private final DownloadsManager manager;
    private final Context context;
    private final LayoutInflater inflater;

    public DirectDownloadAdapter(Context context, DownloadsManager manager) {
        this.inflater = LayoutInflater.from(context);
        this.context = context;
        this.manager = manager;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    public int refresh() {
        for (int i = 0; i < getItemCount(); i++)
            super.notifyItemChanged(i, manager.getRunningDownloadAt(i));

        return getItemCount();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position, List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
        } else {
            DDDownload payload = (DDDownload) payloads.get(0);
            holder.update(payload);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        DDDownload download = manager.getRunningDownloadAt(position);
        holder.name.setText(download.name);
        holder.update(download);
    }

    @Override
    public int getItemCount() {
        return manager.getRunningDownloadsCount();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final TextView status;
        final TextView name;
        final ProgressBar progress;
        final TextView percentage;

        public ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.direct_download_item, parent, false));

            status = (TextView) itemView.findViewById(R.id.ddItem_status);
            name = (TextView) itemView.findViewById(R.id.ddItem_name);
            progress = (ProgressBar) itemView.findViewById(R.id.ddItem_progress);
            progress.setMax(1000);
            percentage = (TextView) itemView.findViewById(R.id.ddItem_percentage);
        }

        public void update(DDDownload download) {
            progress.setProgress((int) download.getProgress());
            percentage.setText(String.format(Locale.getDefault(), "%.2f%%", download.getProgress()));

            if (download.status == DDDownload.Status.ERROR) {
                status.setText(context.getString(R.string.error_details, download.errorCause.getMessage()));
                status.setTextColor(Color.RED);
            } else {
                status.setText(download.status.toFormal(context));
                status.setTextColor(ContextCompat.getColor(context, android.R.color.secondary_text_light));
            }
        }
    }
}
