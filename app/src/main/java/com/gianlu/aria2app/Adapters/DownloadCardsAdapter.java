package com.gianlu.aria2app.Adapters;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Messenger;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.aria2app.CustomDownloadInfo;
import com.gianlu.aria2app.DonutProgress;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.Download;
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithUpdate;
import com.gianlu.aria2app.PK;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Services.NotificationService;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.adapters.OrderedRecyclerViewAdapter;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.ui.Toaster;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

public class DownloadCardsAdapter extends OrderedRecyclerViewAdapter<DownloadCardsAdapter.ViewHolder, DownloadWithUpdate, DownloadCardsAdapter.SortBy, Download.Status> implements ServiceConnection, Aria2Helper.DownloadActionClick.Listener {
    private final Context context;
    private final Listener listener;
    private final LayoutInflater inflater;
    private final LocalReceiver receiver;
    private final LocalBroadcastManager broadcastManager;
    private final Map<String, NotificationService.Mode> notificationStates = new HashMap<>();
    private final Queue<String> pendingModeRequests = new LinkedList<>();
    private Messenger notificationMessenger;

    public DownloadCardsAdapter(Context context, List<DownloadWithUpdate> objs, Listener listener) {
        super(objs, SortBy.STATUS);
        this.context = context.getApplicationContext();
        this.listener = listener;
        this.inflater = LayoutInflater.from(context);
        this.broadcastManager = LocalBroadcastManager.getInstance(context);
        setHasStableIds(true);

        receiver = new LocalReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(NotificationService.EVENT_STOPPED);
        filter.addAction(NotificationService.EVENT_GET_MODE);
        broadcastManager.registerReceiver(receiver, filter);

        bindNotificationService();
    }

    private void bindNotificationService() {
        context.bindService(new Intent(context, NotificationService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void finalize() throws Throwable {
        if (broadcastManager != null) broadcastManager.unregisterReceiver(receiver);
        super.finalize();
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public long getItemId(int position) {
        return objs.get(position).hashCode();
    }

    @Override
    protected void onUpdateViewHolder(@NonNull ViewHolder holder, int position, @NonNull DownloadWithUpdate payload) {
        holder.update(payload);
    }

    @Override
    protected void onUpdateViewHolder(@NonNull ViewHolder holder, int position, @NonNull Object payload) {
        if (payload instanceof NotificationService.Mode) {
            holder.setupNotification(objs.get(position), (NotificationService.Mode) payload);
        }
    }

    @Override
    public void onSetupViewHolder(@NonNull ViewHolder holder, int position, final @NonNull DownloadWithUpdate item) {
        DownloadWithUpdate.SmallUpdate update = item.update();

        int colorAccent = ContextCompat.getColor(context, update.getColorAccent());
        Utils.setupChart(holder.detailsChart, true);
        holder.detailsChart.setNoDataTextColor(colorAccent);
        holder.donutProgress.setFinishedStrokeColor(colorAccent);

        holder.detailsGid.setHtml(R.string.gid, item.gid);
        holder.detailsTotalLength.setHtml(R.string.total_length, CommonUtils.dimensionFormatter(update.length, false));

        holder.itemView.setOnClickListener(view -> {
            if (CommonUtils.isExpanded(holder.details)) {
                CommonUtils.collapse(holder.details, null);
                CommonUtils.collapseTitle(holder.downloadName);
            } else {
                CommonUtils.expand(holder.details, null);
                CommonUtils.expandTitle(holder.downloadName);
            }
        });
        holder.more.setOnClickListener(view -> {
            if (listener != null) listener.onMoreClick(item);
        });

        holder.pause.setOnClickListener(new Aria2Helper.DownloadActionClick(item, Aria2Helper.WhatAction.PAUSE, this));
        holder.restart.setOnClickListener(new Aria2Helper.DownloadActionClick(item, Aria2Helper.WhatAction.RESTART, this));
        holder.start.setOnClickListener(new Aria2Helper.DownloadActionClick(item, Aria2Helper.WhatAction.RESUME, this));
        holder.stop.setOnClickListener(new Aria2Helper.DownloadActionClick(item, Aria2Helper.WhatAction.STOP, this));
        holder.remove.setOnClickListener(new Aria2Helper.DownloadActionClick(item, Aria2Helper.WhatAction.REMOVE, this));
        holder.moveUp.setOnClickListener(new Aria2Helper.DownloadActionClick(item, Aria2Helper.WhatAction.MOVE_UP, this));
        holder.moveDown.setOnClickListener(new Aria2Helper.DownloadActionClick(item, Aria2Helper.WhatAction.MOVE_DOWN, this));
        holder.customInfo.setDisplayInfo(CustomDownloadInfo.Info.toArray(Prefs.getSet(PK.A2_CUSTOM_INFO, new HashSet<>()), update.isTorrent()));
        holder.update(item);

        holder.toggleNotification.setOnClickListener(v -> {
            NotificationService.Mode mode = notificationStates.get(item.gid);
            if (mode != null) {
                NotificationService.Mode setMode;
                switch (mode) {
                    case NOTIFY_EXCLUSIVE:
                        setMode = NotificationService.Mode.NOT_NOTIFY_STANDARD;
                        break;
                    case NOTIFY_STANDARD:
                        setMode = NotificationService.Mode.NOT_NOTIFY_EXCLUSIVE;
                        break;
                    case NOT_NOTIFY_EXCLUSIVE:
                        setMode = NotificationService.Mode.NOTIFY_STANDARD;
                        break;
                    default:
                    case NOT_NOTIFY_STANDARD:
                        setMode = NotificationService.Mode.NOTIFY_EXCLUSIVE;
                        break;
                }

                NotificationService.setMode(context, item.gid, setMode);
            }
        });

        NotificationService.Mode mode = notificationStates.get(item.gid);
        if (mode != null)
            holder.setupNotification(item, mode);
        else if (notificationMessenger != null)
            NotificationService.getMode(notificationMessenger, item.gid);
        else if (!pendingModeRequests.contains(item.gid))
            pendingModeRequests.add(item.gid);
    }

    @Override
    protected boolean matchQuery(@NonNull DownloadWithUpdate item, @Nullable String query) {
        return (query == null
                || item.update().getName().toLowerCase().contains(query.toLowerCase())
                || item.gid.toLowerCase().contains(query.toLowerCase()))
                && !filters.contains(item.getFilterable());
    }

    @Override
    protected void shouldUpdateItemCount(int count) {
        if (listener != null) listener.onItemCountUpdated(count);
    }

    @Override
    @NonNull
    public Comparator<DownloadWithUpdate> getComparatorFor(SortBy sorting) {
        switch (sorting) {
            case NAME:
                return new DownloadWithUpdate.NameComparator();
            default:
            case STATUS:
                return new DownloadWithUpdate.StatusComparator();
            case PROGRESS:
                return new DownloadWithUpdate.ProgressComparator();
            case DOWNLOAD_SPEED:
                return new DownloadWithUpdate.DownloadSpeedComparator();
            case UPLOAD_SPEED:
                return new DownloadWithUpdate.UploadSpeedComparator();
            case COMPLETED_LENGTH:
                return new DownloadWithUpdate.CompletedLengthComparator();
            case LENGTH:
                return new DownloadWithUpdate.LengthComparator();
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        notificationMessenger = new Messenger(service);

        String gid;
        while ((gid = pendingModeRequests.poll()) != null)
            NotificationService.getMode(notificationMessenger, gid);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        notificationMessenger = null;
        bindNotificationService();
    }

    private int indexOf(String gid) {
        if (gid == null) return -1;
        for (int i = 0; i < getItemCount(); i++)
            if (Objects.equals(objs.get(i).gid, gid))
                return i;

        return -1;
    }

    public void activityDestroying(@NonNull Context context) {
        context.getApplicationContext().unbindService(this);
        if (broadcastManager != null) broadcastManager.unregisterReceiver(receiver);
    }

    @Override
    public void showDialog(@NonNull AlertDialog.Builder builder) {
        listener.showDialog(builder);
    }

    @Override
    public void showToast(@NonNull Toaster toaster) {
        toaster.show(context);
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

    public interface Listener {
        void onMoreClick(@NonNull DownloadWithUpdate item);

        void onItemCountUpdated(int count);

        void showDialog(@NonNull AlertDialog.Builder builder);
    }

    private class LocalReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, final Intent intent) {
            if (intent.getAction() == null) return;

            RecyclerView list = getList();
            if (list != null) {
                list.post(() -> {
                    if (Objects.equals(intent.getAction(), NotificationService.EVENT_GET_MODE)) {
                        String gid = intent.getStringExtra("gid");
                        NotificationService.Mode mode = (NotificationService.Mode) intent.getSerializableExtra("mode");
                        notificationStates.put(gid, mode);
                        int index = indexOf(gid);
                        if (index != -1) notifyItemChanged(index, mode);
                    } else if (Objects.equals(intent.getAction(), NotificationService.EVENT_STOPPED)) {
                        for (String gid : notificationStates.keySet())
                            notificationStates.put(gid, NotificationService.Mode.NOT_NOTIFY_STANDARD);
                        notifyItemRangeChanged(0, getItemCount(), NotificationService.Mode.NOT_NOTIFY_STANDARD);
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
            super(inflater.inflate(R.layout.item_download, parent, false));

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
            more = itemView.findViewById(R.id.downloadCard_actionMore);

            toggleNotification = itemView.findViewById(R.id.downloadCard_toggleNotification);
            toggleNotification.setVisibility(View.GONE);

            detailsChart = itemView.findViewById(R.id.downloadCard_detailsChart);
            detailsGid = itemView.findViewById(R.id.downloadCard_detailsGid);
            detailsTotalLength = itemView.findViewById(R.id.downloadCard_detailsTotalLength);
            detailsCompletedLength = itemView.findViewById(R.id.downloadCard_detailsCompletedLength);
            detailsUploadLength = itemView.findViewById(R.id.downloadCard_detailsUploadLength);
        }

        private void setupNotification(@NonNull DownloadWithUpdate download, @NonNull NotificationService.Mode mode) {
            DownloadWithUpdate.SmallUpdate last = download.update();
            if (last.status == Download.Status.ERROR || last.status == Download.Status.REMOVED || last.status == Download.Status.COMPLETE) {
                toggleNotification.setVisibility(View.GONE);
                return;
            }

            toggleNotification.setVisibility(View.VISIBLE);
            switch (mode) {
                case NOTIFY_EXCLUSIVE:
                    toggleNotification.setImageResource(R.drawable.baseline_notifications_active_24);
                    break;
                default:
                case NOTIFY_STANDARD:
                    toggleNotification.setImageResource(R.drawable.baseline_notifications_24);
                    break;
                case NOT_NOTIFY_EXCLUSIVE:
                    toggleNotification.setImageResource(R.drawable.baseline_notifications_off_24);
                    break;
                case NOT_NOTIFY_STANDARD:
                    toggleNotification.setImageResource(R.drawable.baseline_notifications_none_24);
                    break;
            }
        }

        private void setupActions(@NonNull DownloadWithUpdate download) {
            start.setVisibility(View.VISIBLE);
            stop.setVisibility(View.VISIBLE);
            restart.setVisibility(View.VISIBLE);
            pause.setVisibility(View.VISIBLE);
            remove.setVisibility(View.VISIBLE);
            moveUp.setVisibility(View.VISIBLE);
            moveDown.setVisibility(View.VISIBLE);
            toggleNotification.setVisibility(View.VISIBLE);
            more.setVisibility(View.VISIBLE);

            switch (download.update().status) {
                case ACTIVE:
                    restart.setVisibility(View.GONE);
                    start.setVisibility(View.GONE);
                    remove.setVisibility(View.GONE);
                    moveUp.setVisibility(View.GONE);
                    moveDown.setVisibility(View.GONE);
                    break;
                case PAUSED:
                    pause.setVisibility(View.GONE);
                    restart.setVisibility(View.GONE);
                    remove.setVisibility(View.GONE);
                    moveUp.setVisibility(View.GONE);
                    moveDown.setVisibility(View.GONE);
                    break;
                case WAITING:
                    pause.setVisibility(View.GONE);
                    restart.setVisibility(View.GONE);
                    stop.setVisibility(View.GONE);
                    start.setVisibility(View.GONE);
                    break;
                case ERROR:
                    more.setVisibility(View.INVISIBLE);
                case COMPLETE:
                case REMOVED:
                    if (download.update().isTorrent()) restart.setVisibility(View.GONE);
                    pause.setVisibility(View.GONE);
                    start.setVisibility(View.GONE);
                    stop.setVisibility(View.GONE);
                    moveUp.setVisibility(View.GONE);
                    moveDown.setVisibility(View.GONE);
                    toggleNotification.setVisibility(View.GONE);
                    break;
            }
        }

        public void update(@NonNull DownloadWithUpdate download) {
            DownloadWithUpdate.SmallUpdate last = download.update();
            if (last.status == Download.Status.ACTIVE) {
                detailsChart.setVisibility(View.VISIBLE);

                LineData data = detailsChart.getData();
                if (data == null) {
                    Utils.setupChart(detailsChart, true);
                    data = detailsChart.getData();
                }

                if (data != null) {
                    int pos = data.getEntryCount() / 2 + 1;
                    data.addEntry(new Entry(pos, last.downloadSpeed), Utils.CHART_DOWNLOAD_SET);
                    data.addEntry(new Entry(pos, last.uploadSpeed), Utils.CHART_UPLOAD_SET);
                    data.notifyDataChanged();
                    detailsChart.notifyDataSetChanged();

                    detailsChart.setVisibleXRangeMaximum(90);
                    detailsChart.moveViewToX(pos - 91);
                }
            } else {
                detailsChart.clear();
                detailsChart.setVisibility(View.GONE);
            }

            if (last.status == Download.Status.ERROR || last.status == Download.Status.REMOVED || last.status == Download.Status.COMPLETE)
                toggleNotification.setVisibility(View.GONE);
            else
                toggleNotification.setVisibility(View.VISIBLE);

            donutProgress.setProgress(last.getProgress());
            downloadName.setText(last.getName());
            if (last.status == Download.Status.ERROR) {
                if (last.errorMessage == null)
                    downloadStatus.setText(String.format(Locale.getDefault(), "%s #%d", last.status.getFormal(context, true), last.errorCode));
                else
                    downloadStatus.setText(String.format(Locale.getDefault(), "%s #%d: %s", last.status.getFormal(context, true), last.errorCode, last.errorMessage));
            } else {
                downloadStatus.setText(last.status.getFormal(context, true));
            }

            customInfo.update(last);

            detailsCompletedLength.setHtml(R.string.completed_length, CommonUtils.dimensionFormatter(last.completedLength, false));

            if (last.isTorrent()) {
                detailsUploadLength.setHtml(R.string.uploaded_length, CommonUtils.dimensionFormatter(last.uploadLength, false));
                detailsUploadLength.setVisibility(View.VISIBLE);
            } else {
                detailsUploadLength.setVisibility(View.GONE);
            }

            setupActions(download);

            CommonUtils.setRecyclerViewTopMargin(this);
        }
    }
}
