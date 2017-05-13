package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gianlu.aria2app.Activities.DirectDownload.DownloadSupervisor;
import com.gianlu.aria2app.MainActivity;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.model.FileDownloadStatus;

import java.util.List;
import java.util.Locale;

public class DirectDownloadsAdapter extends RecyclerView.Adapter<DirectDownloadsAdapter.ViewHolder> {
    private final Context context;
    private final List<BaseDownloadTask> objs;

    public DirectDownloadsAdapter(Context context, List<BaseDownloadTask> objs) {
        this.context = context;
        this.objs = objs;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.direct_download_item, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final BaseDownloadTask item = getItem(position);

        holder.name.setText(item.getFilename());

        float progress = (float) item.getLargeFileSoFarBytes() / (float) item.getLargeFileTotalBytes() * 100;
        holder.progressBar.setProgress((int) progress);
        holder.progressText.setText(String.format("%s %%", String.format(Locale.getDefault(), "%.2f", progress)));
        holder.speed.setText(CommonUtils.speedFormatter(item.getSpeed() * 1000));
        int speed = item.getSpeed() * 1000;
        if (speed != 0)
            holder.missingTime.setText(CommonUtils.timeFormatter((item.getLargeFileTotalBytes() - item.getLargeFileSoFarBytes()) / speed));

        switch (item.getStatus()) {
            case FileDownloadStatus.connected:
                holder.status.setText(R.string.downloading);
                break;
            case FileDownloadStatus.completed:
                holder.status.setText(R.string.completed);
                holder.speed.setText(CommonUtils.speedFormatter(0));
                break;
            case FileDownloadStatus.paused:
                holder.status.setText(R.string.paused);
                holder.speed.setText(CommonUtils.speedFormatter(0));
                break;
            case FileDownloadStatus.pending:
                holder.status.setText(R.string.pending);
                holder.speed.setText(CommonUtils.speedFormatter(0));
                break;
            case FileDownloadStatus.progress:
                holder.status.setText(R.string.downloading);
                break;
            case FileDownloadStatus.started:
                holder.status.setText(R.string.connecting);
                holder.speed.setText(CommonUtils.speedFormatter(0));
                break;
            case FileDownloadStatus.error:
                @SuppressWarnings("ThrowableResultOfMethodCallIgnored") String message = item.getErrorCause().getMessage();
                if (message == null) holder.status.setText(R.string.error);
                else holder.status.setText(context.getString(R.string.error_details, message));
                break;
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                context.startActivity(new Intent(context, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .putExtra("fromDirectDownload", true)
                        .putExtra("gid", (String) item.getTag(DownloadSupervisor.TAG_GID))
                        .putExtra("index", (int) item.getTag(DownloadSupervisor.TAG_INDEX)));
            }
        });

        // TODO: Menu
    }

    private BaseDownloadTask getItem(int pos) {
        return objs.get(pos);
    }

    @Override
    public int getItemCount() {
        return objs.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView status;
        public final TextView name;
        final ProgressBar progressBar;
        final TextView progressText;
        final TextView speed;
        final TextView missingTime;
        final ImageButton menu;

        public ViewHolder(View itemView) {
            super(itemView);

            status = (TextView) itemView.findViewById(R.id.directDownloadItem_status);
            name = (TextView) itemView.findViewById(R.id.directDownloadItem_name);
            progressBar = (ProgressBar) itemView.findViewById(R.id.directDownloadItem_progressBar);
            progressBar.setMax(100);
            progressText = (TextView) itemView.findViewById(R.id.directDownloadItem_progressText);
            speed = (TextView) itemView.findViewById(R.id.directDownloadItem_speed);
            missingTime = (TextView) itemView.findViewById(R.id.directDownloadItem_missingTime);
            menu = (ImageButton) itemView.findViewById(R.id.directDownloadItem_menu);
        }
    }
}
