package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gianlu.aria2app.NetIO.DownloadsManager.DDDownload;
import com.gianlu.aria2app.NetIO.DownloadsManager.DownloadsManager;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;

import java.util.List;
import java.util.Locale;

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

        // TODO: Setup actions
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
        final TextView downloadSpeed;
        final TextView missingTime;
        final ImageButton pause;
        final ImageButton resume;
        final ImageButton stop;
        final ImageButton remove;

        public ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.direct_download_item, parent, false));

            status = (TextView) itemView.findViewById(R.id.ddItem_status);
            name = (TextView) itemView.findViewById(R.id.ddItem_name);
            progress = (ProgressBar) itemView.findViewById(R.id.ddItem_progress);
            progress.setMax(100);
            percentage = (TextView) itemView.findViewById(R.id.ddItem_percentage);
            downloadSpeed = (TextView) itemView.findViewById(R.id.ddItem_downloadSpeed);
            missingTime = (TextView) itemView.findViewById(R.id.ddItem_missingTime);

            pause = (ImageButton) itemView.findViewById(R.id.ddItem_pause);
            resume = (ImageButton) itemView.findViewById(R.id.ddItem_resume);
            stop = (ImageButton) itemView.findViewById(R.id.ddItem_stop);
            remove = (ImageButton) itemView.findViewById(R.id.ddItem_remove);
        }

        private void updateStatus(DDDownload.Status status) {
            pause.setVisibility(View.VISIBLE);
            resume.setVisibility(View.VISIBLE);
            stop.setVisibility(View.VISIBLE);
            remove.setVisibility(View.VISIBLE);

            switch (status) {
                case COMPLETED:
                    pause.setVisibility(View.GONE);
                    resume.setVisibility(View.GONE);
                    stop.setVisibility(View.GONE);
                    break;
                case ERROR:
                    pause.setVisibility(View.GONE);
                    resume.setVisibility(View.GONE);
                    stop.setVisibility(View.GONE);
                    break;
                case PAUSED:
                    pause.setVisibility(View.GONE);
                    remove.setVisibility(View.GONE);
                    break;
                case RUNNING:
                    resume.setVisibility(View.GONE);
                    remove.setVisibility(View.GONE);
                    break;
            }
        }

        public void update(DDDownload download) {
            progress.setProgress((int) download.getProgress());
            percentage.setText(String.format(Locale.getDefault(), "%.2f%%", download.getProgress()));
            downloadSpeed.setText(CommonUtils.speedFormatter(download.downloadSpeed, false));
            missingTime.setText(CommonUtils.timeFormatter(download.getMissingTime()));

            if (download.status == DDDownload.Status.ERROR) {
                status.setText(context.getString(R.string.error_details, download.errorCause.getMessage()));
                status.setTextColor(Color.RED);
            } else {
                status.setText(download.status.toFormal(context));
                status.setTextColor(ContextCompat.getColor(context, android.R.color.secondary_text_light));
            }

            updateStatus(download.status);
        }
    }
}
