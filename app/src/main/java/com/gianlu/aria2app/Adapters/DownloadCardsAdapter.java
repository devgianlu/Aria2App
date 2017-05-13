package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.gianlu.aria2app.DonutProgress;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.SuperTextView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

// TODO: Rearrange layout of the card
// FIXME: Sorting
public class DownloadCardsAdapter extends RecyclerView.Adapter<DownloadCardsAdapter.DownloadViewHolder> {
    private final Context context;
    private final List<Download> originalObjs;
    private final List<Download> objs;
    private final IAdapter handler;
    private final List<Download.Status> filters;
    private final LayoutInflater inflater;

    public DownloadCardsAdapter(Context context, List<Download> objs, IAdapter handler) {
        this.context = context;
        this.originalObjs = objs;
        this.objs = new ArrayList<>(objs);
        this.handler = handler;
        this.filters = new ArrayList<>();
        this.inflater = LayoutInflater.from(context);

        Collections.sort(this.objs, new StatusComparator());
    }

    public void sortBy(SortBy sorting) {
        switch (sorting) {
            case STATUS:
                Collections.sort(objs, new StatusComparator());
                break;
            case PROGRESS:
                Collections.sort(objs, new ProgressComparator());
                break;
            case DOWNLOAD_SPEED:
                Collections.sort(objs, new DownloadSpeedComparator());
                break;
            case UPLOAD_SPEED:
                Collections.sort(objs, new UploadSpeedComparator());
                break;
            case COMPLETED_LENGTH:
                Collections.sort(objs, new CompletedLengthComparator());
                break;
            case LENGTH:
                Collections.sort(objs, new LengthComparator());
                break;
        }

        notifyDataSetChanged();
    }

    private void processFilters() {
        objs.clear();

        for (Download obj : originalObjs)
            if (!filters.contains(obj.status))
                objs.add(obj);

        if (handler != null) handler.onItemCountUpdated(objs.size());
        notifyDataSetChanged();
    }

    public void addFilter(Download.Status status) {
        filters.add(status);
        processFilters();
    }

    public void removeFilter(Download.Status status) {
        filters.remove(status);
        processFilters();
    }

    private int indexOf(String gid) {
        for (int i = 0; i < objs.size(); i++)
            if (Objects.equals(objs.get(i).gid, gid))
                return i;

        return -1;
    }

    private int originalIndexOf(String gid) {
        for (int i = 0; i < originalObjs.size(); i++)
            if (Objects.equals(originalObjs.get(i).gid, gid))
                return i;

        return -1;
    }

    public void notifyItemChanged(Download payload) {
        int pos = indexOf(payload.gid);
        if (pos == -1 && originalIndexOf(payload.gid) == -1) {
            originalObjs.add(payload);
            processFilters();
            super.notifyItemInserted(objs.size() - 1);
            if (handler != null) handler.onItemCountUpdated(objs.size());
            return;
        }

        super.notifyItemChanged(pos, payload);
    }

    @Override
    public DownloadCardsAdapter.DownloadViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DownloadViewHolder(inflater.inflate(R.layout.download_card, parent, false));
    }

    @Override
    public void onBindViewHolder(DownloadCardsAdapter.DownloadViewHolder holder, int position, List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
        } else {
            Download item = (Download) payloads.get(0);

            if (item.status == Download.Status.ACTIVE) {
                holder.detailsChartRefresh.setEnabled(true);

                LineData data = holder.detailsChart.getData();
                if (data == null) {
                    Utils.setupChart(holder.detailsChart, true);
                    data = holder.detailsChart.getData();
                }

                if (data != null) {
                    int pos = data.getEntryCount() / 2 + 1;
                    data.addEntry(new Entry(pos, item.downloadSpeed), Utils.CHART_DOWNLOAD_SET);
                    data.addEntry(new Entry(pos, item.uploadSpeed), Utils.CHART_UPLOAD_SET);
                    data.notifyDataChanged();
                    holder.detailsChart.notifyDataSetChanged();

                    holder.detailsChart.setVisibleXRangeMaximum(90);
                    holder.detailsChart.moveViewToX(pos - 91);
                }
            } else {
                holder.detailsChartRefresh.setEnabled(false);

                holder.detailsChart.clear();
                holder.detailsChart.setNoDataText(context.getString(R.string.downloadIs, item.status.getFormal(context, false)));
            }

            holder.donutProgress.setProgress(item.getProgress());
            holder.downloadName.setText(item.getName());
            if (item.status == Download.Status.ERROR)
                holder.downloadStatus.setText(String.format(Locale.getDefault(), "%s #%d: %s", item.status.getFormal(context, true), item.errorCode, item.errorMessage));
            else
                holder.downloadStatus.setText(item.status.getFormal(context, true));
            holder.downloadSpeed.setText(CommonUtils.speedFormatter(item.downloadSpeed));
            holder.downloadMissingTime.setText(CommonUtils.timeFormatter(item.getMissingTime()));

            holder.detailsCompletedLength.setHtml(R.string.completed_length, CommonUtils.dimensionFormatter(item.completedLength));
            holder.detailsUploadLength.setHtml(R.string.uploaded_length, CommonUtils.dimensionFormatter(item.uploadLength));

            if (item.status == Download.Status.UNKNOWN || item.status == Download.Status.ERROR)
                holder.more.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onBindViewHolder(final DownloadCardsAdapter.DownloadViewHolder holder, int position) {
        final Download item = objs.get(position);

        final int color;
        if (item.isBitTorrent)
            color = ContextCompat.getColor(context, R.color.colorTorrent_pressed);
        else color = ContextCompat.getColor(context, R.color.colorAccent);

        Utils.setupChart(holder.detailsChart, true);
        holder.donutProgress.setFinishedStrokeColor(color);

        holder.detailsGid.setHtml(R.string.gid, item.gid);
        holder.detailsTotalLength.setHtml(R.string.total_length, CommonUtils.dimensionFormatter(item.length));

        holder.expand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CommonUtils.animateCollapsingArrowBellows((ImageButton) view, CommonUtils.isExpanded(holder.details));

                if (CommonUtils.isExpanded(holder.details)) {
                    CommonUtils.collapse(holder.details);
                    CommonUtils.collapseTitle(holder.downloadName);
                } else {
                    CommonUtils.expand(holder.details);
                    CommonUtils.expandTitle(holder.downloadName);
                }
            }
        });
        holder.detailsChartRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.setupChart(holder.detailsChart, true);
            }
        });
        holder.more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handler.onMoreClick(item);
            }
        });
        holder.menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popupMenu = new PopupMenu(context, holder.menu, Gravity.BOTTOM);
                popupMenu.inflate(R.menu.download_cardview);
                Menu menu = popupMenu.getMenu();

                switch (item.status) {
                    case ACTIVE:
                        menu.removeItem(R.id.downloadCardViewMenu_resume);
                        menu.removeItem(R.id.downloadCardViewMenu_restart);
                        menu.removeItem(R.id.downloadCardViewMenu_moveUp);
                        menu.removeItem(R.id.downloadCardViewMenu_moveDown);
                        break;
                    case WAITING:
                        menu.removeItem(R.id.downloadCardViewMenu_pause);
                        menu.removeItem(R.id.downloadCardViewMenu_resume);
                        menu.removeItem(R.id.downloadCardViewMenu_restart);
                        break;
                    case PAUSED:
                        menu.removeItem(R.id.downloadCardViewMenu_pause);
                        menu.removeItem(R.id.downloadCardViewMenu_restart);
                        menu.removeItem(R.id.downloadCardViewMenu_moveUp);
                        menu.removeItem(R.id.downloadCardViewMenu_moveDown);
                        break;
                    case COMPLETE:
                        menu.removeItem(R.id.downloadCardViewMenu_pause);
                        menu.removeItem(R.id.downloadCardViewMenu_resume);
                        menu.removeItem(R.id.downloadCardViewMenu_restart);
                        menu.removeItem(R.id.downloadCardViewMenu_moveUp);
                        menu.removeItem(R.id.downloadCardViewMenu_moveDown);
                        break;
                    case ERROR:
                        if (item.isBitTorrent)
                            menu.removeItem(R.id.downloadCardViewMenu_restart);
                        menu.removeItem(R.id.downloadCardViewMenu_pause);
                        menu.removeItem(R.id.downloadCardViewMenu_resume);
                        menu.removeItem(R.id.downloadCardViewMenu_moveUp);
                        menu.removeItem(R.id.downloadCardViewMenu_moveDown);
                        break;
                    case REMOVED:
                        if (item.isBitTorrent)
                            menu.removeItem(R.id.downloadCardViewMenu_restart);
                        menu.removeItem(R.id.downloadCardViewMenu_pause);
                        menu.removeItem(R.id.downloadCardViewMenu_resume);
                        menu.removeItem(R.id.downloadCardViewMenu_moveUp);
                        menu.removeItem(R.id.downloadCardViewMenu_moveDown);
                        break;
                }

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.downloadCardViewMenu_remove:
                                handler.onMenuItemSelected(item, JTA2.DownloadActions.REMOVE);
                                break;
                            case R.id.downloadCardViewMenu_restart:
                                handler.onMenuItemSelected(item, JTA2.DownloadActions.RESTART);
                                break;
                            case R.id.downloadCardViewMenu_resume:
                                handler.onMenuItemSelected(item, JTA2.DownloadActions.RESUME);
                                break;
                            case R.id.downloadCardViewMenu_pause:
                                handler.onMenuItemSelected(item, JTA2.DownloadActions.PAUSE);
                                break;
                            case R.id.downloadCardViewMenu_moveDown:
                                handler.onMenuItemSelected(item, JTA2.DownloadActions.MOVE_DOWN);
                                break;
                            case R.id.downloadCardViewMenu_moveUp:
                                handler.onMenuItemSelected(item, JTA2.DownloadActions.MOVE_UP);
                                break;
                        }
                        return true;
                    }
                });
                popupMenu.show();
            }
        });

        if (item.status == Download.Status.UNKNOWN || item.status == Download.Status.ERROR)
            holder.more.setVisibility(View.INVISIBLE);
    }

    @Override
    public int getItemCount() {
        return objs.size();
    }

    public enum SortBy {
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
    }

    private class StatusComparator implements Comparator<Download> {
        @Override
        public int compare(Download o1, Download o2) {
            if (o1.status == o2.status) return 0;
            else if (o1.status.ordinal() < o2.status.ordinal()) return -1;
            else return 1;
        }
    }

    private class DownloadSpeedComparator implements Comparator<Download> {
        @Override
        public int compare(Download o1, Download o2) {
            if (Objects.equals(o1.downloadSpeed, o2.downloadSpeed)) return 0;
            else if (o1.downloadSpeed > o2.downloadSpeed) return -1;
            else return 1;
        }
    }

    private class UploadSpeedComparator implements Comparator<Download> {
        @Override
        public int compare(Download o1, Download o2) {
            if (Objects.equals(o1.uploadSpeed, o2.uploadSpeed)) return 0;
            else if (o1.uploadSpeed > o2.uploadSpeed) return -1;
            else return 1;
        }
    }

    private class LengthComparator implements Comparator<Download> {
        @Override
        public int compare(Download o1, Download o2) {
            if (Objects.equals(o1.length, o2.length)) return 0;
            else if (o1.length > o2.length) return -1;
            else return 1;
        }
    }

    private class CompletedLengthComparator implements Comparator<Download> {
        @Override
        public int compare(Download o1, Download o2) {
            if (Objects.equals(o1.completedLength, o2.completedLength)) return 0;
            else if (o1.completedLength > o2.completedLength) return -1;
            else return 1;
        }
    }

    private class ProgressComparator implements Comparator<Download> {
        @Override
        public int compare(Download o1, Download o2) {
            if (Objects.equals(o1.getProgress(), o2.getProgress())) return 0;
            else if (o1.getProgress() > o2.getProgress()) return 1;
            else return -1;
        }
    }

    class DownloadViewHolder extends RecyclerView.ViewHolder {
        final DonutProgress donutProgress;
        final SuperTextView downloadName;
        final SuperTextView downloadStatus;
        final SuperTextView downloadSpeed;
        final SuperTextView downloadMissingTime;
        final LinearLayout details;
        final ImageButton detailsChartRefresh;
        final SuperTextView detailsGid;
        final SuperTextView detailsTotalLength;
        final SuperTextView detailsCompletedLength;
        final SuperTextView detailsUploadLength;
        final ImageButton expand;
        final Button more;
        final ImageButton menu;
        final LineChart detailsChart;

        DownloadViewHolder(View itemView) {
            super(itemView);

            donutProgress = (DonutProgress) itemView.findViewById(R.id.downloadCard_donutProgress);
            downloadName = (SuperTextView) itemView.findViewById(R.id.downloadCard_name);
            downloadStatus = (SuperTextView) itemView.findViewById(R.id.downloadCard_status);
            downloadSpeed = (SuperTextView) itemView.findViewById(R.id.downloadCard_downloadSpeed);
            downloadMissingTime = (SuperTextView) itemView.findViewById(R.id.downloadCard_missingTime);
            details = (LinearLayout) itemView.findViewById(R.id.downloadCard_details);
            expand = (ImageButton) itemView.findViewById(R.id.downloadCard_expand);
            more = (Button) itemView.findViewById(R.id.downloadCard_actionMore);
            menu = (ImageButton) itemView.findViewById(R.id.downloadCard_actionMenu);

            detailsChart = (LineChart) itemView.findViewById(R.id.downloadCard_detailsChart);
            detailsChartRefresh = (ImageButton) itemView.findViewById(R.id.downloadCard_detailsChartRefresh);
            detailsGid = (SuperTextView) itemView.findViewById(R.id.downloadCard_detailsGid);
            detailsTotalLength = (SuperTextView) itemView.findViewById(R.id.downloadCard_detailsTotalLength);
            detailsCompletedLength = (SuperTextView) itemView.findViewById(R.id.downloadCard_detailsCompletedLength);
            detailsUploadLength = (SuperTextView) itemView.findViewById(R.id.downloadCard_detailsUploadLength);
        }
    }
}
