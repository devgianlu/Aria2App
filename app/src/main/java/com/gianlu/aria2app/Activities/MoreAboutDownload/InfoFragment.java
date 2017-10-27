package com.gianlu.aria2app.Activities.MoreAboutDownload;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gianlu.aria2app.Activities.MoreAboutDownload.Info.UpdateUI;
import com.gianlu.aria2app.Adapters.BitfieldVisualizer;
import com.gianlu.aria2app.CountryFlags;
import com.gianlu.aria2app.NetIO.FreeGeoIP.FreeGeoIPApi;
import com.gianlu.aria2app.NetIO.FreeGeoIP.IPDetails;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.JTA2.JTA2InitializingException;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.MessageLayout;
import com.gianlu.commonutils.SuperTextView;
import com.gianlu.commonutils.Toaster;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;

import java.net.URI;
import java.util.Date;
import java.util.Locale;

public class InfoFragment extends BackPressedFragment implements UpdateUI.IUI, JTA2.IRemove, JTA2.IRestart, JTA2.IUnpause, JTA2.IPause, JTA2.IMove {
    private final CountryFlags flags = CountryFlags.get();
    private IStatusChanged listener;
    private UpdateUI updater;
    private ViewHolder holder;
    private Download.Status lastStatus = Download.Status.UNKNOWN;
    private FreeGeoIPApi freeGeoIPApi = FreeGeoIPApi.get();

    public static InfoFragment getInstance(Context context, Download download, IStatusChanged listener) {
        InfoFragment fragment = new InfoFragment();
        fragment.listener = listener;
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.info));
        args.putSerializable("download", download);
        fragment.setArguments(args);
        return fragment;
    }

    private void handleDownloadAction(final Download download, JTA2.DownloadActions action) {
        if (download == null) return; // Garbage collected

        final JTA2 jta2;
        try {
            jta2 = JTA2.instantiate(getContext());
        } catch (JTA2InitializingException ex) {
            onException(ex);
            return;
        }

        switch (action) {
            case MOVE_UP:
                jta2.moveUp(download.gid, this);
                break;
            case MOVE_DOWN:
                jta2.moveDown(download.gid, this);
                break;
            case PAUSE:
                jta2.pause(download.gid, this);
                break;
            case REMOVE:
                if (download.status == Download.Status.ACTIVE || download.status == Download.Status.PAUSED || download.status == Download.Status.WAITING) {
                    CommonUtils.showDialog(getActivity(), new AlertDialog.Builder(getContext())
                            .setTitle(getString(R.string.removeName, download.getName()))
                            .setMessage(R.string.removeDownloadAlert)
                            .setNegativeButton(android.R.string.no, null)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    removeDownload(jta2, download);
                                }
                            }));
                } else {
                    removeDownload(jta2, download);
                }
                break;
            case RESTART:
                jta2.restart(download.gid, this);
                break;
            case RESUME:
                jta2.unpause(download.gid, this);
                break;
        }
    }

    private void removeDownload(final JTA2 jta2, final Download download) {
        if (download.following != null) {
            CommonUtils.showDialog(getActivity(), new AlertDialog.Builder(getContext())
                    .setTitle(getString(R.string.removeMetadataName, download.getName()))
                    .setMessage(R.string.removeDownload_removeMetadata)
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            jta2.remove(download.gid, false, InfoFragment.this);
                        }
                    })
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            jta2.remove(download.gid, true, InfoFragment.this);
                        }
                    }));
        } else {
            jta2.remove(download.gid, false, InfoFragment.this);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        holder = new ViewHolder((ViewGroup) inflater.inflate(R.layout.info_fragment, container, false));
        MessageLayout.setPaddingTop(holder.rootView, 48);

        Download download = (Download) getArguments().getSerializable("download");
        if (download == null) {
            holder.loading.setVisibility(View.GONE);
            MessageLayout.show(holder.rootView, R.string.failedLoading, R.drawable.ic_error_outline_black_48dp);
            return holder.rootView;
        }

        holder.setup(download);

        try {
            updater = new UpdateUI(getContext(), download.gid, this);
            updater.start();
        } catch (JTA2InitializingException ex) {
            holder.loading.setVisibility(View.GONE);
            MessageLayout.show(holder.rootView, R.string.failedLoading, R.drawable.ic_error_outline_black_48dp);
            Logging.logMe(getContext(), ex);
            return holder.rootView;
        }

        return holder.rootView;
    }

    @Override
    public void onUpdateUI(Download download) {
        if (holder != null) holder.update(download);

        if (listener != null && lastStatus != download.status)
            listener.onStatusChanged(download.status);

        lastStatus = download.status;
    }

    @Override
    public boolean canGoBack(int code) {
        return true;
    }

    @Override
    public void onBackPressed() {
        if (updater != null) updater.stopThread(null);
    }

    @Override
    public void onPaused(String gid) {
        Toaster.show(getActivity(), Utils.Messages.PAUSED, gid);
    }

    @Override
    public void onRestarted(String gid) {
        Toaster.show(getActivity(), Utils.Messages.RESTARTED, gid);
    }

    @Override
    public void onUnpaused(String gid) {
        Toaster.show(getActivity(), Utils.Messages.RESUMED, gid);
    }

    @Override
    public void onMoved(String gid) {
        Toaster.show(getActivity(), Utils.Messages.MOVED, gid);
    }

    @Override
    public void onException(Exception ex) {
        Toaster.show(getActivity(), Utils.Messages.FAILED_PERFORMING_ACTION, ex);
    }

    @Override
    public void onRemoved(String gid) {
        Toaster.show(getActivity(), Utils.Messages.REMOVED, gid);
    }

    @Override
    public void onRemovedResult(String gid) {
        Toaster.show(getActivity(), Utils.Messages.RESULT_REMOVED, gid);
        if (listener != null) listener.onStatusChanged(Download.Status.UNKNOWN);
    }

    public interface IStatusChanged {
        void onStatusChanged(Download.Status newStatus);
    }

    public class ViewHolder {
        final ViewGroup rootView;
        final LinearLayout container;
        final ProgressBar loading;
        final SuperTextView progress;
        final SuperTextView downloadSpeed;
        final SuperTextView uploadSpeed;
        final SuperTextView remainingTime;
        final ImageButton pause;
        final ImageButton start;
        final ImageButton stop;
        final ImageButton restart;
        final ImageButton remove;
        final ImageButton moveUp;
        final ImageButton moveDown;
        final LineChart chart;
        final SuperTextView gid;
        final SuperTextView totalLength;
        final SuperTextView completedLength;
        final SuperTextView uploadLength;
        final SuperTextView pieceLength;
        final SuperTextView numPieces;
        final SuperTextView connections;
        final SuperTextView directory;
        final SuperTextView verifiedLength;
        final SuperTextView verifyIntegrityPending;
        final BitfieldVisualizer bitfield;
        final LinearLayout bitTorrentOnly;
        final SuperTextView btMode;
        final SuperTextView btSeeders;
        final SuperTextView btSeeder;
        final SuperTextView shareRatio;
        final SuperTextView btComment;
        final SuperTextView btCreationDate;
        final LinearLayout btAnnounceListContainer;
        final ImageButton toggleBtAnnounceList;
        final LinearLayout btAnnounceList;

        ViewHolder(ViewGroup rootView) {
            this.rootView = rootView;

            container = rootView.findViewById(R.id.infoFragment_container);
            loading = rootView.findViewById(R.id.infoFragment_loading);
            progress = rootView.findViewById(R.id.infoFragment_progress);
            downloadSpeed = rootView.findViewById(R.id.infoFragment_downloadSpeed);
            uploadSpeed = rootView.findViewById(R.id.infoFragment_uploadSpeed);
            remainingTime = rootView.findViewById(R.id.infoFragment_remainingTime);
            chart = rootView.findViewById(R.id.infoFragment_chart);
            pause = rootView.findViewById(R.id.infoFragment_pause);
            start = rootView.findViewById(R.id.infoFragment_start);
            stop = rootView.findViewById(R.id.infoFragment_stop);
            restart = rootView.findViewById(R.id.infoFragment_restart);
            remove = rootView.findViewById(R.id.infoFragment_remove);
            moveUp = rootView.findViewById(R.id.infoFragment_moveUp);
            moveDown = rootView.findViewById(R.id.infoFragment_moveDown);
            gid = rootView.findViewById(R.id.infoFragment_gid);
            totalLength = rootView.findViewById(R.id.infoFragment_totalLength);
            completedLength = rootView.findViewById(R.id.infoFragment_completedLength);
            uploadLength = rootView.findViewById(R.id.infoFragment_uploadLength);
            pieceLength = rootView.findViewById(R.id.infoFragment_pieceLength);
            numPieces = rootView.findViewById(R.id.infoFragment_numPieces);
            connections = rootView.findViewById(R.id.infoFragment_connections);
            directory = rootView.findViewById(R.id.infoFragment_directory);
            verifiedLength = rootView.findViewById(R.id.infoFragment_verifiedLength);
            verifyIntegrityPending = rootView.findViewById(R.id.infoFragment_verifyIntegrityPending);
            bitfield = rootView.findViewById(R.id.infoFragment_bitfield);

            bitTorrentOnly = rootView.findViewById(R.id.infoFragment_bitTorrentOnly);
            btMode = rootView.findViewById(R.id.infoFragment_btMode);
            btSeeders = rootView.findViewById(R.id.infoFragment_btSeeders);
            btSeeder = rootView.findViewById(R.id.infoFragment_btSeeder);
            shareRatio = rootView.findViewById(R.id.infoFragment_shareRatio);
            btComment = rootView.findViewById(R.id.infoFragment_btComment);
            btCreationDate = rootView.findViewById(R.id.infoFragment_btCreationDate);
            btAnnounceListContainer = rootView.findViewById(R.id.infoFragment_btAnnounceListContainer);
            toggleBtAnnounceList = rootView.findViewById(R.id.infoFragment_toggleBtAnnounceList);
            btAnnounceList = rootView.findViewById(R.id.infoFragment_btAnnounceList);
        }

        void setup(final Download download) {
            Utils.setupChart(chart, false, R.color.colorPrimaryDark);
            int colorRes = download.isTorrent() ? R.color.colorTorrent : R.color.colorAccent;
            chart.setNoDataTextColor(ContextCompat.getColor(getContext(), colorRes));
            bitfield.setColor(colorRes);
            progress.setTypeface("fonts/Roboto-Light.ttf");
            downloadSpeed.setTypeface("fonts/Roboto-Light.ttf");
            uploadSpeed.setTypeface("fonts/Roboto-Light.ttf");
            remainingTime.setTypeface("fonts/Roboto-Light.ttf");

            pause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleDownloadAction(download, JTA2.DownloadActions.PAUSE);
                }
            });
            start.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleDownloadAction(download, JTA2.DownloadActions.RESUME);
                }
            });
            stop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleDownloadAction(download, JTA2.DownloadActions.REMOVE);
                }
            });
            restart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleDownloadAction(download, JTA2.DownloadActions.RESTART);
                }
            });
            remove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleDownloadAction(download, JTA2.DownloadActions.REMOVE);
                }
            });

            toggleBtAnnounceList.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CommonUtils.animateCollapsingArrowBellows(toggleBtAnnounceList, CommonUtils.isExpanded(btAnnounceList));

                    if (CommonUtils.isExpanded(btAnnounceList))
                        CommonUtils.collapse(btAnnounceList);
                    else
                        CommonUtils.expand(btAnnounceList);
                }
            });

            if (download.torrent.announceList.isEmpty()) {
                btAnnounceListContainer.setVisibility(View.GONE);
            } else {
                btAnnounceListContainer.setVisibility(View.VISIBLE);
                btAnnounceList.removeAllViews();
                for (String url : download.torrent.announceList) {
                    final LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.bt_announce_item, btAnnounceList, false);
                    ((TextView) layout.getChildAt(0)).setText(url);
                    ((ImageView) layout.getChildAt(1)).setImageResource(R.drawable.ic_list_country_unknown);
                    btAnnounceList.addView(layout);

                    freeGeoIPApi.getIPDetails(URI.create(url).getHost(), new FreeGeoIPApi.IIPDetails() {
                        @Override
                        public void onDetails(IPDetails details) {
                            if (isAdded())
                                ((ImageView) layout.getChildAt(1)).setImageDrawable(flags.loadFlag(getContext(), details.countryCode));
                        }

                        @Override
                        public void onException(Exception ex) {
                            Logging.logMe(getContext(), ex);
                        }
                    });
                }
            }
        }

        void setActionsState(Download download) {
            start.setVisibility(View.VISIBLE);
            stop.setVisibility(View.VISIBLE);
            restart.setVisibility(View.VISIBLE);
            pause.setVisibility(View.VISIBLE);
            remove.setVisibility(View.VISIBLE);
            moveUp.setVisibility(View.VISIBLE);
            moveDown.setVisibility(View.VISIBLE);

            switch (download.status) {
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
                case COMPLETE:
                case REMOVED:
                    if (download.isTorrent()) restart.setVisibility(View.GONE);
                    pause.setVisibility(View.GONE);
                    start.setVisibility(View.GONE);
                    stop.setVisibility(View.GONE);
                    moveUp.setVisibility(View.GONE);
                    moveDown.setVisibility(View.GONE);
                    break;
                case UNKNOWN:
                    pause.setVisibility(View.GONE);
                    start.setVisibility(View.GONE);
                    stop.setVisibility(View.GONE);
                    restart.setVisibility(View.GONE);
                    remove.setVisibility(View.GONE);
                    moveUp.setVisibility(View.GONE);
                    moveDown.setVisibility(View.GONE);
                    break;
            }
        }

        boolean setChartState(Download download) {
            switch (download.status) {
                case ACTIVE:
                    return true;
                default:
                case PAUSED:
                case WAITING:
                case ERROR:
                case REMOVED:
                case COMPLETE:
                case UNKNOWN:
                    chart.clear();
                    chart.setNoDataText(getString(R.string.downloadIs, download.status.getFormal(getContext(), false)));
                    return false;
            }
        }

        void update(Download download) {
            if (!isAdded()) return;

            MessageLayout.hide(rootView);
            loading.setVisibility(View.GONE);
            container.setVisibility(View.VISIBLE);

            setActionsState(download);

            if (setChartState(download)) {
                LineData data = chart.getLineData();
                if (data == null) {
                    Utils.setupChart(chart, true, R.color.colorPrimaryDark);
                    data = chart.getLineData();
                }

                int pos = data.getEntryCount();
                data.addEntry(new Entry(pos, download.downloadSpeed), Utils.CHART_DOWNLOAD_SET);
                data.addEntry(new Entry(pos, download.uploadSpeed), Utils.CHART_UPLOAD_SET);
                data.notifyDataChanged();
                chart.notifyDataSetChanged();
                chart.setVisibleXRangeMaximum(90);
                chart.moveViewToX(data.getEntryCount());
            }

            progress.setText(String.format(Locale.getDefault(), "%.1f %%", download.getProgress()));
            downloadSpeed.setText(CommonUtils.speedFormatter(download.downloadSpeed, false));
            uploadSpeed.setText(CommonUtils.speedFormatter(download.uploadSpeed, false));
            remainingTime.setText(CommonUtils.timeFormatter(download.getMissingTime()));
            bitfield.update(download);

            gid.setHtml(R.string.gid, download.gid);
            totalLength.setHtml(R.string.total_length, CommonUtils.dimensionFormatter(download.length, false));
            completedLength.setHtml(R.string.completed_length, CommonUtils.dimensionFormatter(download.completedLength, false));
            uploadLength.setHtml(R.string.uploaded_length, CommonUtils.dimensionFormatter(download.uploadLength, false));
            pieceLength.setHtml(R.string.pieces_length, CommonUtils.dimensionFormatter(download.pieceLength, false));
            numPieces.setHtml(R.string.pieces, download.numPieces);
            connections.setHtml(R.string.connections, download.connections);
            directory.setHtml(R.string.directory, download.dir);
            verifiedLength.setHtml(R.string.verifiedLength, CommonUtils.dimensionFormatter(download.verifiedLength, false));
            verifyIntegrityPending.setHtml(R.string.verifyIntegrityPending, String.valueOf(download.verifyIntegrityPending));

            bitTorrentOnly.setVisibility(download.isTorrent() ? View.VISIBLE : View.GONE);
            if (download.isTorrent()) {
                btMode.setHtml(R.string.mode, download.torrent.mode.toString());
                btSeeders.setHtml(R.string.numSeeder, download.numSeeders);
                btSeeder.setHtml(R.string.seeder, String.valueOf(download.seeder));
                shareRatio.setHtml(R.string.shareRatio, String.format(Locale.getDefault(), "%.2f", download.shareRatio()));

                if (download.torrent.comment == null) {
                    btComment.setVisibility(View.GONE);
                } else {
                    btComment.setVisibility(View.VISIBLE);
                    btComment.setHtml(R.string.comment, download.torrent.comment);
                }

                if (download.torrent.creationDate == -1) {
                    btCreationDate.setVisibility(View.GONE);
                } else {
                    btCreationDate.setVisibility(View.VISIBLE);
                    btCreationDate.setHtml(R.string.creation_date, CommonUtils.getFullDateFormatter().format(new Date(download.torrent.creationDate)));
                }
            }
        }
    }
}
