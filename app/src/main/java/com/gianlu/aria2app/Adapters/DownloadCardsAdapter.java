package com.gianlu.aria2app.Adapters;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.gianlu.aria2app.CustomDownloadInfo;
import com.gianlu.aria2app.DonutProgress;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.PKeys;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Services.NotificationService;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.Adapters.OrderedRecyclerViewAdapter;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.commonutils.SuperTextView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class DownloadCardsAdapter extends OrderedRecyclerViewAdapter<DownloadCardsAdapter.ViewHolder, Download, DownloadCardsAdapter.SortBy, Download.Status> implements ServiceConnection {
    private final Context context;
    private final IAdapter listener;
    private final LayoutInflater inflater;
    private final LocalReceiver receiver;
    private final LocalBroadcastManager broadcastManager;
    private Messenger notificationMessenger;

    public DownloadCardsAdapter(Context context, List<Download> objs, IAdapter listener) {
        super(objs, SortBy.STATUS);
        this.context = context;
        this.listener = listener;
        this.inflater = LayoutInflater.from(context);
        this.broadcastManager = LocalBroadcastManager.getInstance(context);
        setHasStableIds(true);

        receiver = new LocalReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(NotificationService.ACTION_STOPPED);
        filter.addAction(NotificationService.ACTION_IS_NOTIFICABLE);
        broadcastManager.registerReceiver(receiver, filter);

        context.bindService(new Intent(context, NotificationService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void finalize() throws Throwable {
        if (broadcastManager != null) broadcastManager.unregisterReceiver(receiver);
        super.finalize();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public long getItemId(int position) {
        return objs.get(position).gid.hashCode();
    }

    private void setupActions(ViewHolder holder, Download download) {
        holder.start.setVisibility(View.VISIBLE);
        holder.stop.setVisibility(View.VISIBLE);
        holder.restart.setVisibility(View.VISIBLE);
        holder.pause.setVisibility(View.VISIBLE);
        holder.remove.setVisibility(View.VISIBLE);
        holder.moveUp.setVisibility(View.VISIBLE);
        holder.moveDown.setVisibility(View.VISIBLE);
        holder.toggleNotification.setVisibility(View.VISIBLE);
        holder.more.setVisibility(View.VISIBLE);

        switch (download.status) {
            case ACTIVE:
                holder.restart.setVisibility(View.GONE);
                holder.start.setVisibility(View.GONE);
                holder.remove.setVisibility(View.GONE);
                holder.moveUp.setVisibility(View.GONE);
                holder.moveDown.setVisibility(View.GONE);
                break;
            case PAUSED:
                holder.pause.setVisibility(View.GONE);
                holder.restart.setVisibility(View.GONE);
                holder.remove.setVisibility(View.GONE);
                holder.moveUp.setVisibility(View.GONE);
                holder.moveDown.setVisibility(View.GONE);
                break;
            case WAITING:
                holder.pause.setVisibility(View.GONE);
                holder.restart.setVisibility(View.GONE);
                holder.stop.setVisibility(View.GONE);
                holder.start.setVisibility(View.GONE);
                break;
            case ERROR:
                holder.more.setVisibility(View.INVISIBLE);
            case COMPLETE:
            case REMOVED:
                if (download.isTorrent()) holder.restart.setVisibility(View.GONE);
                holder.pause.setVisibility(View.GONE);
                holder.start.setVisibility(View.GONE);
                holder.stop.setVisibility(View.GONE);
                holder.moveUp.setVisibility(View.GONE);
                holder.moveDown.setVisibility(View.GONE);
                holder.toggleNotification.setVisibility(View.GONE);
                break;
            case UNKNOWN:
                holder.more.setVisibility(View.GONE);
                holder.pause.setVisibility(View.GONE);
                holder.start.setVisibility(View.GONE);
                holder.stop.setVisibility(View.GONE);
                holder.restart.setVisibility(View.GONE);
                holder.remove.setVisibility(View.GONE);
                holder.moveUp.setVisibility(View.GONE);
                holder.moveDown.setVisibility(View.GONE);
                holder.toggleNotification.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    protected void onBindViewHolder(ViewHolder holder, int position, @NonNull Download payload) {
        holder.update(payload);
        CommonUtils.setRecyclerViewTopMargin(context, holder);
    }

    @Override
    protected void onBindViewHolder(ViewHolder holder, int position, Object payload) {
        if (payload instanceof Boolean) { // Notification toggle
            if ((Boolean) payload)
                holder.toggleNotification.setImageResource(R.drawable.ic_notifications_active_black_48dp);
            else
                holder.toggleNotification.setImageResource(R.drawable.ic_notifications_none_black_48dp);
        }
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final Download item = objs.get(position);

        final int color;
        if (item.isTorrent())
            color = ContextCompat.getColor(context, R.color.colorTorrent_pressed);
        else color = ContextCompat.getColor(context, R.color.colorAccent);

        Utils.setupChart(holder.detailsChart, true, R.color.colorPrimaryDark);
        holder.detailsChart.setNoDataTextColor(color);
        holder.donutProgress.setFinishedStrokeColor(color);

        holder.detailsGid.setHtml(R.string.gid, item.gid);
        holder.detailsTotalLength.setHtml(R.string.total_length, CommonUtils.dimensionFormatter(item.length, false));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CommonUtils.isExpanded(holder.details)) {
                    CommonUtils.collapse(holder.details, null);
                    CommonUtils.collapseTitle(holder.downloadName);
                } else {
                    CommonUtils.expand(holder.details, null);
                    CommonUtils.expandTitle(holder.downloadName);
                }
            }
        });

        holder.more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) listener.onMoreClick(item);
            }
        });
        holder.pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onMenuItemSelected(item, JTA2.DownloadActions.PAUSE);
            }
        });
        holder.restart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null)
                    listener.onMenuItemSelected(item, JTA2.DownloadActions.RESTART);
            }
        });
        holder.start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null)
                    listener.onMenuItemSelected(item, JTA2.DownloadActions.RESUME);
            }
        });
        holder.stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null)
                    listener.onMenuItemSelected(item, JTA2.DownloadActions.REMOVE);
            }
        });
        holder.remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null)
                    listener.onMenuItemSelected(item, JTA2.DownloadActions.REMOVE);
            }
        });
        holder.moveUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null)
                    listener.onMenuItemSelected(item, JTA2.DownloadActions.MOVE_UP);
            }
        });
        holder.moveDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null)
                    listener.onMenuItemSelected(item, JTA2.DownloadActions.MOVE_DOWN);
            }
        });
        holder.toggleNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotificationService.toggleNotification(context, broadcastManager, item.gid, new NotificationService.OnIsNotificable() {
                    @Override
                    public void onResult(final boolean notificable) {
                        holder.toggleNotification.post(new Runnable() {
                            @Override
                            public void run() {
                                notifyItemChanged(holder.getAdapterPosition(), notificable);
                            }
                        });
                    }
                });
            }
        });

        if (notificationMessenger != null) {
            try {
                notificationMessenger.send(Message.obtain(null, NotificationService.MESSENGER_IS_NOTIFICABLE, item.gid));
            } catch (RemoteException ex) {
                Logging.log(ex);
            }
        }

        holder.customInfo.setDisplayInfo(CustomDownloadInfo.Info.toArray(Prefs.getSet(context, PKeys.A2_CUSTOM_INFO, new HashSet<String>()), item.isTorrent()));
        setupActions(holder, item);
        holder.update(item);
        CommonUtils.setRecyclerViewTopMargin(context, holder);
    }

    @Nullable
    @Override
    protected RecyclerView getRecyclerView() {
        if (listener != null) return listener.getRecyclerView();
        else return null;
    }

    @Override
    protected boolean matchQuery(Download item, @Nullable String query) {
        return (query == null
                || item.getName().toLowerCase().contains(query.toLowerCase())
                || item.gid.toLowerCase().contains(query.toLowerCase()))
                && !filters.contains(item.getFilterable());
    }

    @Override
    protected void shouldUpdateItemCount(int count) {
        if (listener != null) listener.onItemCountUpdated(count);
    }

    @Override
    @NonNull
    public Comparator<Download> getComparatorFor(SortBy sorting) {
        switch (sorting) {
            case NAME:
                return new Download.NameComparator();
            default:
            case STATUS:
                return new Download.StatusComparator();
            case PROGRESS:
                return new Download.ProgressComparator();
            case DOWNLOAD_SPEED:
                return new Download.DownloadSpeedComparator();
            case UPLOAD_SPEED:
                return new Download.UploadSpeedComparator();
            case COMPLETED_LENGTH:
                return new Download.CompletedLengthComparator();
            case LENGTH:
                return new Download.LengthComparator();
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        notificationMessenger = new Messenger(service);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        notificationMessenger = null;
    }

    private int indexOf(String gid) {
        if (gid == null) return -1;
        for (int i = 0; i < getItemCount(); i++)
            if (Objects.equals(objs.get(i).gid, gid))
                return i;

        return -1;
    }

    public void activityDestroying(Context context) {
        context.unbindService(this);
        if (broadcastManager != null) broadcastManager.unregisterReceiver(receiver);
    }

    public enum SortBy {
        NAME,
        STATUS,
        PROGRESS,
        DOWNLOAD_SPEED,
        UPLOAD_SPEED,
        COMPLETED_LENGTH,
        LENGTH
    }

    public interface IAdapter {
        void onMoreClick(Download item);

        void onItemCountUpdated(int count);

        void onMenuItemSelected(Download download, JTA2.DownloadActions action);

        @Nullable
        RecyclerView getRecyclerView();
    }

    private class LocalReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, final Intent intent) {
            if (intent.getAction() == null) return;

            RecyclerView recyclerView = listener != null ? listener.getRecyclerView() : null;
            if (recyclerView != null) {
                recyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        switch (intent.getAction()) {
                            case NotificationService.ACTION_IS_NOTIFICABLE:
                                int index = indexOf(intent.getStringExtra("gid"));
                                if (index != -1)
                                    notifyItemChanged(index, intent.getBooleanExtra("notificable", false));
                                break;
                            case NotificationService.ACTION_STOPPED:
                                for (int i = 0; i < getItemCount(); i++)
                                    notifyItemChanged(i, false);
                                break;
                        }
                    }
                });
            }
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final LinearLayout details;
        public final Button more;
        final DonutProgress donutProgress;
        final SuperTextView downloadName;
        final SuperTextView downloadStatus;
        final CustomDownloadInfo customInfo;
        final SuperTextView detailsGid;
        final SuperTextView detailsTotalLength;
        final SuperTextView detailsCompletedLength;
        final SuperTextView detailsUploadLength;
        final ImageButton pause;
        final ImageButton start;
        final ImageButton stop;
        final ImageButton restart;
        final ImageButton remove;
        final ImageButton moveUp;
        final ImageButton moveDown;
        final ImageButton toggleNotification;
        final LineChart detailsChart;

        ViewHolder(ViewGroup parent) {
            super(inflater.inflate(R.layout.card_download, parent, false));

            donutProgress = itemView.findViewById(R.id.downloadCard_donutProgress);
            downloadName = itemView.findViewById(R.id.downloadCard_name);
            downloadStatus = itemView.findViewById(R.id.downloadCard_status);
            customInfo = itemView.findViewById(R.id.downloadCard_customInfo);
            details = itemView.findViewById(R.id.downloadCard_details);
            pause = itemView.findViewById(R.id.downloadCard_pause);
            start = itemView.findViewById(R.id.downloadCard_start);
            stop = itemView.findViewById(R.id.downloadCard_stop);
            restart = itemView.findViewById(R.id.downloadCard_restart);
            remove = itemView.findViewById(R.id.downloadCard_remove);
            moveUp = itemView.findViewById(R.id.downloadCard_moveUp);
            moveDown = itemView.findViewById(R.id.downloadCard_moveDown);
            toggleNotification = itemView.findViewById(R.id.downloadCard_toggleNotification);
            more = itemView.findViewById(R.id.downloadCard_actionMore);

            detailsChart = itemView.findViewById(R.id.downloadCard_detailsChart);
            detailsGid = itemView.findViewById(R.id.downloadCard_detailsGid);
            detailsTotalLength = itemView.findViewById(R.id.downloadCard_detailsTotalLength);
            detailsCompletedLength = itemView.findViewById(R.id.downloadCard_detailsCompletedLength);
            detailsUploadLength = itemView.findViewById(R.id.downloadCard_detailsUploadLength);
        }

        public void update(Download item) {
            if (item.status == Download.Status.ACTIVE) {
                LineData data = detailsChart.getData();
                if (data == null) {
                    Utils.setupChart(detailsChart, true, R.color.colorPrimaryDark);
                    data = detailsChart.getData();
                }

                if (data != null) {
                    int pos = data.getEntryCount() / 2 + 1;
                    data.addEntry(new Entry(pos, item.downloadSpeed), Utils.CHART_DOWNLOAD_SET);
                    data.addEntry(new Entry(pos, item.uploadSpeed), Utils.CHART_UPLOAD_SET);
                    data.notifyDataChanged();
                    detailsChart.notifyDataSetChanged();

                    detailsChart.setVisibleXRangeMaximum(90);
                    detailsChart.moveViewToX(pos - 91);
                }
            } else {
                detailsChart.clear();
                detailsChart.setNoDataText(context.getString(R.string.downloadIs, item.status.getFormal(context, false)));
            }

            donutProgress.setProgress(item.getProgress());
            downloadName.setText(item.getName());
            if (item.status == Download.Status.ERROR)
                downloadStatus.setText(String.format(Locale.getDefault(), "%s #%d: %s", item.status.getFormal(context, true), item.errorCode, item.errorMessage));
            else
                downloadStatus.setText(item.status.getFormal(context, true));

            customInfo.update(item);

            detailsCompletedLength.setHtml(R.string.completed_length, CommonUtils.dimensionFormatter(item.completedLength, false));
            detailsUploadLength.setHtml(R.string.uploaded_length, CommonUtils.dimensionFormatter(item.uploadLength, false));

            setupActions(this, item);
        }
    }
}
