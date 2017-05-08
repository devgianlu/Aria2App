package com.gianlu.aria2app.Main;

import android.app.Activity;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gianlu.aria2app.DonutProgress;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.GlobalStats;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainCardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_CARD = 0;
    private static final int TYPE_SUMMARY = 1;
    final boolean hasSummary;
    private final Activity context;
    private final List<Download> objs;
    private final IActions handler;
    private final List<Download.STATUS> filters;
    private final LayoutInflater inflater;
    private final Typeface roboto;

    public MainCardAdapter(Activity context, List<Download> objs, boolean hasSummary, IActions handler) {
        this.context = context;
        this.objs = objs;
        this.hasSummary = hasSummary;
        this.handler = handler;
        this.filters = new ArrayList<>();
        inflater = LayoutInflater.from(context);

        roboto = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Light.ttf");
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

    public void addFilter(Download.STATUS status) {
        filters.add(status);
        notifyDataSetChanged();
    }

    public void removeFilter(Download.STATUS status) {
        filters.remove(status);
        notifyDataSetChanged();
    }

    void updateItem(final int position, final Download update) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyItemChanged(position + (hasSummary ? 1 : 0), update);
            }
        });
    }

    public Download getItem(String gid) {
        for (Download download : objs)
            if (download.gid.equals(gid))
                return download;
        return null;
    }

    public void removeItem(String gid) {
        int index = objs.indexOf(getItem(gid));
        if (index != -1) {
            objs.remove(index);
            notifyDataSetChanged();
        }

        if (handler != null)
            handler.onItemCountUpdated(objs.size());
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && hasSummary)
            return TYPE_SUMMARY;
        else
            return TYPE_CARD;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_SUMMARY)
            return new SummaryViewHolder(inflater.inflate(R.layout.summary_cardview, parent, false));
        else
            return new DownloadViewHolder(inflater.inflate(R.layout.download_cardview, parent, false));
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
        } else {
            if (position == 0 && hasSummary) {
                SummaryViewHolder castHolder = (SummaryViewHolder) holder;
                GlobalStats stats = (GlobalStats) payloads.get(0);

                castHolder.downloadSpeed.setText(CommonUtils.speedFormatter(stats.downloadSpeed));
                castHolder.uploadSpeed.setText(CommonUtils.speedFormatter(stats.uploadSpeed));

                castHolder.active.setText(Html.fromHtml(context.getString(R.string.summaryActive, stats.numActive)));
                castHolder.waiting.setText(Html.fromHtml(context.getString(R.string.summaryWaiting, stats.numWaiting)));
                castHolder.stopped.setText(Html.fromHtml(context.getString(R.string.summaryStopped, stats.numStoppedTotal, stats.numStopped)));

                LineData data = castHolder.chart.getData();
                if (data == null) {
                    Utils.setupChart(castHolder.chart, true);
                    data = castHolder.chart.getData();
                }

                if (data != null) {
                    int pos = data.getEntryCount() / 2 + 1;
                    data.addEntry(new Entry(pos, stats.downloadSpeed), Utils.CHART_DOWNLOAD_SET);
                    data.addEntry(new Entry(pos, stats.uploadSpeed), Utils.CHART_UPLOAD_SET);
                    data.notifyDataChanged();
                    castHolder.chart.notifyDataSetChanged();

                    castHolder.chart.setVisibleXRangeMaximum(90);
                    castHolder.chart.moveViewToX(pos - 91);
                }
            } else {
                DownloadViewHolder castHolder = (DownloadViewHolder) holder;
                Download item = (Download) payloads.get(0);

                if (item.status == Download.STATUS.ACTIVE) {
                    castHolder.detailsChartRefresh.setEnabled(true);

                    LineData data = castHolder.detailsChart.getData();
                    if (data == null) {
                        Utils.setupChart(castHolder.detailsChart, true);
                        data = castHolder.detailsChart.getData();
                    }

                    if (data != null) {
                        int pos = data.getEntryCount() / 2 + 1;
                        data.addEntry(new Entry(pos, item.downloadSpeed), Utils.CHART_DOWNLOAD_SET);
                        data.addEntry(new Entry(pos, item.uploadSpeed), Utils.CHART_UPLOAD_SET);
                        data.notifyDataChanged();
                        castHolder.detailsChart.notifyDataSetChanged();

                        castHolder.detailsChart.setVisibleXRangeMaximum(90);
                        castHolder.detailsChart.moveViewToX(pos - 91);
                    }
                } else {
                    castHolder.detailsChartRefresh.setEnabled(false);

                    castHolder.detailsChart.clear();
                    castHolder.detailsChart.setNoDataText(context.getString(R.string.downloadIs, item.status.getFormal(context, false)));
                }

                castHolder.donutProgress.setProgress(item.getProgress());
                castHolder.downloadName.setText(item.getName());
                if (item.status == Download.STATUS.ERROR)
                    castHolder.downloadStatus.setText(String.format(Locale.getDefault(), "%s #%d: %s", item.status.getFormal(context, true), item.errorCode, item.errorMessage));
                else
                    castHolder.downloadStatus.setText(item.status.getFormal(context, true));
                castHolder.downloadSpeed.setText(CommonUtils.speedFormatter(item.downloadSpeed));
                castHolder.downloadMissingTime.setText(CommonUtils.timeFormatter(item.getMissingTime()));

                castHolder.detailsCompletedLength.setText(Html.fromHtml(context.getString(R.string.completed_length, CommonUtils.dimensionFormatter(item.completedLength))));
                castHolder.detailsUploadLength.setText(Html.fromHtml(context.getString(R.string.uploaded_length, CommonUtils.dimensionFormatter(item.uploadLength))));

                if (item.status == Download.STATUS.UNKNOWN || item.status == Download.STATUS.ERROR)
                    castHolder.more.setVisibility(View.INVISIBLE);
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position == 0 && hasSummary) {
            final SummaryViewHolder castHolder = (SummaryViewHolder) holder;
            Utils.setupChart(castHolder.chart, true);
            castHolder.chartRefresh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Utils.setupChart(castHolder.chart, true);
                }
            });

            castHolder.downloadSpeed.setTypeface(roboto);
            castHolder.downloadSpeed.setText(CommonUtils.speedFormatter(0));
            castHolder.uploadSpeed.setTypeface(roboto);
            castHolder.uploadSpeed.setText(CommonUtils.speedFormatter(0));

            castHolder.active.setText(Html.fromHtml(context.getString(R.string.summaryActive, 0)));
            castHolder.waiting.setText(Html.fromHtml(context.getString(R.string.summaryWaiting, 0)));
            castHolder.stopped.setText(Html.fromHtml(context.getString(R.string.summaryStopped, 0, 0)));
        } else {
            final DownloadViewHolder castHolder = (DownloadViewHolder) holder;
            final Download item = objs.get(position - (hasSummary ? 1 : 0));

            // Static
            final int color;
            if (item.isBitTorrent)
                color = ContextCompat.getColor(context, R.color.colorTorrent_pressed);
            else
                color = ContextCompat.getColor(context, R.color.colorAccent);

            Utils.setupChart(castHolder.detailsChart, true);
            castHolder.donutProgress.setFinishedStrokeColor(color);
            // castHolder.donutProgress.setUnfinishedStrokeColor(Color.argb(26, Color.red(color), Color.green(color), Color.blue(color)));

            castHolder.detailsGid.setText(Html.fromHtml(context.getString(R.string.gid, item.gid)));
            castHolder.detailsTotalLength.setText(Html.fromHtml(context.getString(R.string.total_length, CommonUtils.dimensionFormatter(item.length))));

            castHolder.expand.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    CommonUtils.animateCollapsingArrowBellows((ImageButton) view, CommonUtils.isExpanded(castHolder.details));

                    if (CommonUtils.isExpanded(castHolder.details)) {
                        CommonUtils.collapse(castHolder.details);
                        CommonUtils.collapseTitle(castHolder.downloadName);
                    } else {
                        CommonUtils.expand(castHolder.details);
                        CommonUtils.expandTitle(castHolder.downloadName);
                    }
                }
            });
            castHolder.detailsChartRefresh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Utils.setupChart(castHolder.detailsChart, true);
                }
            });
            castHolder.more.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    handler.onMoreClick(item);
                }
            });
            castHolder.menu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PopupMenu popupMenu = new PopupMenu(context, castHolder.menu, Gravity.BOTTOM);
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

            if (item.status == Download.STATUS.UNKNOWN || item.status == Download.STATUS.ERROR)
                castHolder.more.setVisibility(View.INVISIBLE);

            if (filters.contains(item.status))
                castHolder.itemView.setVisibility(View.GONE);
            else
                castHolder.itemView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        if (handler != null)
            handler.onItemCountUpdated(objs.size());
        return objs.size() + (hasSummary ? 1 : 0);
    }

    List<Download> getItems() {
        return objs;
    }

    void updateSummary(final GlobalStats stats) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyItemChanged(0, stats);
            }
        });
    }

    public enum SortBy {
        STATUS,
        PROGRESS,
        DOWNLOAD_SPEED,
        UPLOAD_SPEED,
        COMPLETED_LENGTH,
        LENGTH
    }

    public interface IActions {
        void onMoreClick(Download item);

        void onItemCountUpdated(int count);

        void onMenuItemSelected(Download download, JTA2.DownloadActions action);
    }

    private class StatusComparator implements Comparator<Download> {
        @Override
        public int compare(Download o1, Download o2) {
            if (o1.status == o2.status)
                return 0;
            else if (o1.status.ordinal() < o2.status.ordinal())
                return -1;
            else
                return 1;
        }
    }

    private class DownloadSpeedComparator implements Comparator<Download> {
        @Override
        public int compare(Download o1, Download o2) {
            if (Objects.equals(o1.downloadSpeed, o2.downloadSpeed))
                return 0;
            else if (o1.downloadSpeed > o2.downloadSpeed)
                return -1;
            else
                return 1;
        }
    }

    private class UploadSpeedComparator implements Comparator<Download> {
        @Override
        public int compare(Download o1, Download o2) {
            if (Objects.equals(o1.uploadSpeed, o2.uploadSpeed))
                return 0;
            else if (o1.uploadSpeed > o2.uploadSpeed)
                return -1;
            else
                return 1;
        }
    }

    private class LengthComparator implements Comparator<Download> {
        @Override
        public int compare(Download o1, Download o2) {
            if (Objects.equals(o1.length, o2.length))
                return 0;
            else if (o1.length > o2.length)
                return -1;
            else
                return 1;
        }
    }

    private class CompletedLengthComparator implements Comparator<Download> {
        @Override
        public int compare(Download o1, Download o2) {
            if (Objects.equals(o1.completedLength, o2.completedLength))
                return 0;
            else if (o1.completedLength > o2.completedLength)
                return -1;
            else
                return 1;
        }
    }

    private class ProgressComparator implements Comparator<Download> {
        @Override
        public int compare(Download o1, Download o2) {
            if (Objects.equals(o1.getProgress(), o2.getProgress()))
                return 0;
            else if (o1.getProgress() > o2.getProgress())
                return 1;
            else
                return -1;
        }
    }

    private class SummaryViewHolder extends RecyclerView.ViewHolder {
        final LineChart chart;
        final ImageButton chartRefresh;
        final TextView downloadSpeed;
        final TextView uploadSpeed;
        final TextView active;
        final TextView waiting;
        final TextView stopped;

        SummaryViewHolder(View itemView) {
            super(itemView);

            chart = (LineChart) itemView.findViewById(R.id.summaryCardViewDetails_chart);
            chartRefresh = (ImageButton) itemView.findViewById(R.id.summaryCardViewDetails_chartRefresh);
            downloadSpeed = (TextView) itemView.findViewById(R.id.summaryCardViewDetails_downloadSpeed);
            uploadSpeed = (TextView) itemView.findViewById(R.id.summaryCardViewDetails_uploadSpeed);
            active = (TextView) itemView.findViewById(R.id.summaryCardViewDetails_active);
            waiting = (TextView) itemView.findViewById(R.id.summaryCardViewDetails_waiting);
            stopped = (TextView) itemView.findViewById(R.id.summaryCardViewDetails_stopped);
        }
    }

    private class DownloadViewHolder extends RecyclerView.ViewHolder {
        final DonutProgress donutProgress;
        final TextView downloadName;
        final TextView downloadStatus;
        final TextView downloadSpeed;
        final TextView downloadMissingTime;
        final LinearLayout details;
        final ImageButton detailsChartRefresh;
        final TextView detailsGid;
        final TextView detailsTotalLength;
        final TextView detailsCompletedLength;
        final TextView detailsUploadLength;
        final ImageButton expand;
        final Button more;
        final ImageButton menu;
        final LineChart detailsChart;

        DownloadViewHolder(View itemView) {
            super(itemView);

            donutProgress = (DonutProgress) itemView.findViewById(R.id.downloadCardView_donutProgress);
            downloadName = (TextView) itemView.findViewById(R.id.downloadCardView_name);
            downloadStatus = (TextView) itemView.findViewById(R.id.downloadCardView_status);
            downloadSpeed = (TextView) itemView.findViewById(R.id.downloadCardView_downloadSpeed);
            downloadMissingTime = (TextView) itemView.findViewById(R.id.downloadCardView_missingTime);
            details = (LinearLayout) itemView.findViewById(R.id.downloadCardView_details);
            expand = (ImageButton) itemView.findViewById(R.id.downloadCardView_expand);
            more = (Button) itemView.findViewById(R.id.downloadCardView_actionMore);
            menu = (ImageButton) itemView.findViewById(R.id.downloadCardView_actionMenu);

            detailsChart = (LineChart) itemView.findViewById(R.id.downloadCardViewDetails_chart);
            detailsChartRefresh = (ImageButton) itemView.findViewById(R.id.downloadCardViewDetails_chartRefresh);
            detailsGid = (TextView) itemView.findViewById(R.id.downloadCardViewDetails_gid);
            detailsTotalLength = (TextView) itemView.findViewById(R.id.downloadCardViewDetails_totalLength);
            detailsCompletedLength = (TextView) itemView.findViewById(R.id.downloadCardViewDetails_completedLength);
            detailsUploadLength = (TextView) itemView.findViewById(R.id.downloadCardViewDetails_uploadLength);
        }
    }
}
