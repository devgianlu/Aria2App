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

public class DirectDownloadsAdapter extends RecyclerView.Adapter<DirectDownloadsAdapter.ViewHolder> {
    private final DownloadsManager manager;
    private final Context context;
    private final LayoutInflater inflater;

    public DirectDownloadsAdapter(Context context, DownloadsManager manager) {
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
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final DDDownload download = manager.getRunningDownloadAt(holder.getAdapterPosition());
        holder.name.setText(download.name);
        holder.update(download);

        holder.pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manager.pause(download);
            }
        });

        holder.resume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manager.resume(download);
            }
        });

        holder.stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manager.remove(download);
                notifyItemRemoved(holder.getAdapterPosition());
            }
        });

        holder.remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manager.remove(download);
                notifyItemRemoved(holder.getAdapterPosition());
            }
        });

        holder.restart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manager.restart(download);
                manager.remove(download);
                notifyItemRemoved(holder.getAdapterPosition());
            }
        });
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
        final ImageButton restart;

        public ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.direct_download_item, parent, false));

            status = itemView.findViewById(R.id.ddItem_status);
            name = itemView.findViewById(R.id.ddItem_name);
            progress = itemView.findViewById(R.id.ddItem_progress);
            progress.setMax(100);
            percentage = itemView.findViewById(R.id.ddItem_percentage);
            downloadSpeed = itemView.findViewById(R.id.ddItem_downloadSpeed);
            missingTime = itemView.findViewById(R.id.ddItem_missingTime);

            pause = itemView.findViewById(R.id.ddItem_pause);
            resume = itemView.findViewById(R.id.ddItem_resume);
            stop = itemView.findViewById(R.id.ddItem_stop);
            remove = itemView.findViewById(R.id.ddItem_remove);
            restart = itemView.findViewById(R.id.ddItem_restart);
        }

        private void updateStatus(DDDownload.Status status) {
            pause.setVisibility(View.VISIBLE);
            resume.setVisibility(View.VISIBLE);
            stop.setVisibility(View.VISIBLE);
            remove.setVisibility(View.VISIBLE);
            restart.setVisibility(View.VISIBLE);

            switch (status) {
                case COMPLETED:
                case ERROR:
                    pause.setVisibility(View.GONE);
                    resume.setVisibility(View.GONE);
                    stop.setVisibility(View.GONE);
                    break;
                case PAUSED:
                    pause.setVisibility(View.GONE);
                    remove.setVisibility(View.GONE);
                    restart.setVisibility(View.GONE);
                    break;
                case RUNNING:
                    resume.setVisibility(View.GONE);
                    remove.setVisibility(View.GONE);
                    restart.setVisibility(View.GONE);
                    break;
            }
        }

        public void update(DDDownload download) {
            progress.setProgress((int) download.getProgress());
            percentage.setText(String.format(Locale.getDefault(), "%.2f%%", download.getProgress()));
            downloadSpeed.setText(CommonUtils.speedFormatter(download.getDownloadSpeed(), false));
            missingTime.setText(CommonUtils.timeFormatter(download.getMissingTime()));

            if (download.status == DDDownload.Status.ERROR) {
                status.setTextColor(Color.RED);
                if (download.errorCause == null) status.setText(R.string.error);
                else
                    status.setText(context.getString(R.string.error_details, download.errorCause.getMessage()));
            } else {
                status.setText(download.status.toFormal(context));
                status.setTextColor(ContextCompat.getColor(context, android.R.color.secondary_text_light));
            }

            updateStatus(download.status);
        }
    }
}
