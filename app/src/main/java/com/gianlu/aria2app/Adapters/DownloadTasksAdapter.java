package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gianlu.aria2app.Downloader.DownloadTask;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.SuperTextView;

import java.util.List;
import java.util.Locale;

public class DownloadTasksAdapter extends RecyclerView.Adapter<DownloadTasksAdapter.ViewHolder> {
    private final Context context;
    private final List<DownloadTask> tasks;
    private final LayoutInflater inflater;
    private final IAdapter listener;

    public DownloadTasksAdapter(Context context, List<DownloadTask> tasks, IAdapter listener) {
        this.context = context;
        this.tasks = tasks;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
        setHasStableIds(true);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final DownloadTask task = tasks.get(position);

        holder.start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onResume(task.task.id);
            }
        });

        holder.pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onPause(task.task.id);
            }
        });

        holder.restart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onRestart(task.task.id);
            }
        });

        holder.remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onRemove(task.task.id);
            }
        });

        holder.open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onOpen(task.task.id);
            }
        });

        holder.title.setText(task.task.getName());
        holder.uri.setText(task.task.uri.toString());
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position, List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
        } else {
            tasks.set(position, (DownloadTask) payloads.get(0));
        }

        DownloadTask task = tasks.get(position);

        holder.status.setText(task.status.getFormal(context));
        holder.status.setTextColor(task.status.getColor(context));

        if (task.length > 0) {
            float percentage = (float) task.downloaded / (float) task.length * 100;
            holder.progress.setIndeterminate(false);
            holder.progress.setProgress((int) percentage);

            holder.percentage.setText(String.format(Locale.getDefault(), "%.2f %%", percentage));
        } else {
            holder.progress.setIndeterminate(true);
            holder.percentage.setVisibility(View.GONE);
        }

        switch (task.status) {
            case STARTED:
                holder.open.setVisibility(View.GONE);
                holder.start.setVisibility(View.GONE);
                holder.pause.setVisibility(View.GONE);
                holder.restart.setVisibility(View.GONE);
                holder.remove.setVisibility(View.VISIBLE);
                holder.progress.setVisibility(View.VISIBLE);
                holder.percentage.setVisibility(View.VISIBLE);
                break;
            case RUNNING:
                holder.open.setVisibility(View.GONE);
                holder.start.setVisibility(View.GONE);
                holder.pause.setVisibility(View.VISIBLE);
                holder.restart.setVisibility(View.GONE);
                holder.remove.setVisibility(View.VISIBLE);
                holder.progress.setVisibility(View.VISIBLE);
                holder.percentage.setVisibility(View.VISIBLE);
                break;
            case PAUSED:
                holder.open.setVisibility(View.GONE);
                holder.start.setVisibility(View.VISIBLE);
                holder.pause.setVisibility(View.GONE);
                holder.restart.setVisibility(View.GONE);
                holder.remove.setVisibility(View.VISIBLE);
                holder.progress.setVisibility(View.VISIBLE);
                holder.percentage.setVisibility(View.VISIBLE);
                break;
            case FAILED:
                holder.open.setVisibility(View.GONE);
                holder.start.setVisibility(View.GONE);
                holder.pause.setVisibility(View.GONE);
                holder.restart.setVisibility(View.VISIBLE);
                holder.remove.setVisibility(View.VISIBLE);
                holder.progress.setVisibility(View.GONE);
                holder.percentage.setVisibility(View.GONE);
                break;
            case COMPLETED:
                holder.open.setVisibility(View.VISIBLE);
                holder.start.setVisibility(View.GONE);
                holder.pause.setVisibility(View.GONE);
                holder.restart.setVisibility(View.GONE);
                holder.remove.setVisibility(View.VISIBLE);
                holder.progress.setVisibility(View.GONE);
                holder.percentage.setVisibility(View.GONE);
                break;
            case PENDING:
                holder.open.setVisibility(View.GONE);
                holder.start.setVisibility(View.GONE);
                holder.pause.setVisibility(View.GONE);
                holder.restart.setVisibility(View.GONE);
                holder.remove.setVisibility(View.VISIBLE);
                holder.progress.setVisibility(View.GONE);
                holder.percentage.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public long getItemId(int position) {
        return tasks.get(position).task.id;
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public void addItemAndNotifyItemInserted(DownloadTask task) {
        tasks.add(task);
        notifyItemInserted(tasks.size() - 1);
    }

    public void removeItemAndNotifyItemRemoved(int pos) {
        if (pos >= 0 && pos < tasks.size()) tasks.remove(pos);
        notifyItemRemoved(pos);
    }

    public interface IAdapter {
        void onResume(int id);

        void onPause(int id);

        void onRestart(int id);

        void onRemove(int id);

        void onOpen(int id);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final SuperTextView status;
        final TextView title;
        final TextView uri;
        final ProgressBar progress;
        final TextView percentage;
        final ImageButton open;
        final ImageButton start;
        final ImageButton pause;
        final ImageButton restart;
        final ImageButton remove;

        public ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.direct_download_item, parent, false));

            status = itemView.findViewById(R.id.directDownloadItem_status);
            status.setTypeface("fonts/Roboto-Bold.ttf");
            title = itemView.findViewById(R.id.directDownloadItem_title);
            uri = itemView.findViewById(R.id.directDownloadItem_uri);
            progress = itemView.findViewById(R.id.directDownloadItem_progress);
            percentage = itemView.findViewById(R.id.directDownloadItem_percentage);
            start = itemView.findViewById(R.id.directDownloadItem_start);
            pause = itemView.findViewById(R.id.directDownloadItem_pause);
            restart = itemView.findViewById(R.id.directDownloadItem_restart);
            remove = itemView.findViewById(R.id.directDownloadItem_remove);
            open = itemView.findViewById(R.id.directDownloadItem_open);
        }
    }
}
