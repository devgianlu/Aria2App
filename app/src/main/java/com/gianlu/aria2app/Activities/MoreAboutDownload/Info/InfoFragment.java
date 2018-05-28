package com.gianlu.aria2app.Activities.MoreAboutDownload.Info;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

import com.gianlu.aria2app.Activities.MoreAboutDownload.BigUpdateProvider;
import com.gianlu.aria2app.Activities.MoreAboutDownload.OnBackPressed;
import com.gianlu.aria2app.Adapters.BitfieldVisualizer;
import com.gianlu.aria2app.CountryFlags;
import com.gianlu.aria2app.Main.HideSecondSpace;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.AriaException;
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithUpdate;
import com.gianlu.aria2app.NetIO.FreeGeoIP.FreeGeoIPApi;
import com.gianlu.aria2app.NetIO.FreeGeoIP.IPDetails;
import com.gianlu.aria2app.NetIO.Updater.PayloadProvider;
import com.gianlu.aria2app.NetIO.Updater.UpdaterFragment;
import com.gianlu.aria2app.NetIO.Updater.Wants;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.MessageLayout;
import com.gianlu.commonutils.SuperTextView;
import com.gianlu.commonutils.Toaster;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Locale;

public class InfoFragment extends UpdaterFragment<DownloadWithUpdate.BigUpdate> implements OnBackPressed, Aria2Helper.DownloadActionClick.Listener {
    private final CountryFlags flags = CountryFlags.get();
    private final FreeGeoIPApi freeGeoIPApi = FreeGeoIPApi.get();
    private ViewHolder holder;

    @NonNull
    public static InfoFragment getInstance(Context context, String gid) {
        InfoFragment fragment = new InfoFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.info));
        args.putString("gid", gid);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        holder = new ViewHolder((ViewGroup) inflater.inflate(R.layout.fragment_info, container, false));
        MessageLayout.setPaddingTop(holder.rootView, 48);
        return holder.rootView;
    }

    @Override
    public boolean onUpdateException(@NonNull Exception ex) {
        if (ex instanceof AriaException && ((AriaException) ex).isNotFound()) {
            if (getActivity() instanceof HideSecondSpace) {
                ((HideSecondSpace) getActivity()).hideSecondSpace();
                return true;
            }
        }

        return super.onUpdateException(ex);
    }

    @Override
    public boolean canGoBack(int code) {
        return true;
    }

    @Override
    public void onUpdateUi(@NonNull DownloadWithUpdate.BigUpdate payload) {
        holder.update(payload);
    }

    @Override
    public boolean onCouldntLoad(@NonNull Exception ex) {
        holder.loading.setVisibility(View.GONE);
        MessageLayout.show(holder.rootView, R.string.failedLoading, R.drawable.ic_error_outline_black_48dp);
        Logging.log(ex);
        return false;
    }

    @NonNull
    @Override
    public Wants<DownloadWithUpdate.BigUpdate> wants(@NonNull Bundle args) {
        return Wants.bigUpdate(args.getString("gid"));
    }

    @NonNull
    @Override
    protected PayloadProvider<DownloadWithUpdate.BigUpdate> requireProvider(@NonNull Context context, @NonNull Bundle args) throws Aria2Helper.InitializingException {
        return new BigUpdateProvider(context, args.getString("gid"));
    }

    @Override
    public void onLoadUi(@NonNull DownloadWithUpdate.BigUpdate payload) {
        holder.setup(payload.download());
    }

    @Override
    public void showDialog(AlertDialog.Builder builder) {
        DialogUtils.showDialog(getActivity(), builder);
    }

    @Override
    public void showToast(Toaster.Message msg, Exception ex) {
        Toaster.show(getActivity(), msg, ex);
    }

    @Override
    public void showToast(Toaster.Message msg, String extra) {
        Toaster.show(getActivity(), msg, extra);
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

        void setup(final DownloadWithUpdate download) {
            if (getContext() == null) return;

            DownloadWithUpdate.BigUpdate update = download.bigUpdate();

            Utils.setupChart(chart, false, R.color.colorPrimaryDark);
            int colorRes = update.isTorrent() ? R.color.colorTorrent : R.color.colorAccent;
            chart.setNoDataTextColor(ContextCompat.getColor(getContext(), colorRes));
            bitfield.setColor(colorRes);
            progress.setTypeface("fonts/Roboto-Light.ttf");
            downloadSpeed.setTypeface("fonts/Roboto-Light.ttf");
            uploadSpeed.setTypeface("fonts/Roboto-Light.ttf");
            remainingTime.setTypeface("fonts/Roboto-Light.ttf");

            pause.setOnClickListener(new Aria2Helper.DownloadActionClick(download, Aria2Helper.WhatAction.PAUSE, InfoFragment.this));
            start.setOnClickListener(new Aria2Helper.DownloadActionClick(download, Aria2Helper.WhatAction.RESUME, InfoFragment.this));
            stop.setOnClickListener(new Aria2Helper.DownloadActionClick(download, Aria2Helper.WhatAction.STOP, InfoFragment.this));
            restart.setOnClickListener(new Aria2Helper.DownloadActionClick(download, Aria2Helper.WhatAction.RESTART, InfoFragment.this));
            remove.setOnClickListener(new Aria2Helper.DownloadActionClick(download, Aria2Helper.WhatAction.REMOVE, InfoFragment.this));
            moveUp.setOnClickListener(new Aria2Helper.DownloadActionClick(download, Aria2Helper.WhatAction.MOVE_UP, InfoFragment.this));
            moveDown.setOnClickListener(new Aria2Helper.DownloadActionClick(download, Aria2Helper.WhatAction.MOVE_DOWN, InfoFragment.this));

            toggleBtAnnounceList.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CommonUtils.handleCollapseClick(toggleBtAnnounceList, btAnnounceList);
                }
            });

            if (update.torrent == null || update.torrent.announceList.isEmpty()) {
                btAnnounceListContainer.setVisibility(View.GONE);
            } else {
                btAnnounceListContainer.setVisibility(View.VISIBLE);
                btAnnounceList.removeAllViews();
                for (String url : update.torrent.announceList) {
                    final LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.item_bt_announce, btAnnounceList, false);
                    ((TextView) layout.getChildAt(0)).setText(url);
                    ((ImageView) layout.getChildAt(1)).setImageResource(R.drawable.ic_list_country_unknown);
                    btAnnounceList.addView(layout);

                    try {
                        URI uri = new URI(url);

                        freeGeoIPApi.getIPDetails(uri.getHost(), new FreeGeoIPApi.OnIpDetails() {
                            @Override
                            public void onDetails(IPDetails details) {
                                if (isAdded())
                                    ((ImageView) layout.getChildAt(1)).setImageDrawable(flags.loadFlag(getContext(), details.countryCode));
                            }

                            @Override
                            public void onException(Exception ex) {
                                Logging.log(ex);
                            }
                        });
                    } catch (URISyntaxException ex) {
                        Logging.log(ex);
                    }
                }
            }
        }

        void setActionsState(DownloadWithUpdate.BigUpdate update) {
            start.setVisibility(View.VISIBLE);
            stop.setVisibility(View.VISIBLE);
            restart.setVisibility(View.VISIBLE);
            pause.setVisibility(View.VISIBLE);
            remove.setVisibility(View.VISIBLE);
            moveUp.setVisibility(View.VISIBLE);
            moveDown.setVisibility(View.VISIBLE);

            switch (update.status) {
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
                    if (update.isTorrent()) restart.setVisibility(View.GONE);
                    pause.setVisibility(View.GONE);
                    start.setVisibility(View.GONE);
                    stop.setVisibility(View.GONE);
                    moveUp.setVisibility(View.GONE);
                    moveDown.setVisibility(View.GONE);
                    break;
            }
        }

        boolean setChartState(DownloadWithUpdate.BigUpdate update) {
            switch (update.status) {
                case ACTIVE:
                    return true;
                default:
                case PAUSED:
                case WAITING:
                case ERROR:
                case REMOVED:
                case COMPLETE:
                    chart.clear();
                    chart.setNoDataText(getString(R.string.downloadIs, update.status.getFormal(getContext(), false)));
                    return false;
            }
        }

        void update(DownloadWithUpdate.BigUpdate update) {
            if (!isAdded()) return;

            MessageLayout.hide(rootView);
            loading.setVisibility(View.GONE);
            container.setVisibility(View.VISIBLE);

            setActionsState(update);

            if (setChartState(update)) {
                LineData data = chart.getLineData();
                if (data == null) {
                    Utils.setupChart(chart, true, R.color.colorPrimaryDark);
                    data = chart.getLineData();
                }

                int pos = data.getEntryCount();
                data.addEntry(new Entry(pos, update.downloadSpeed), Utils.CHART_DOWNLOAD_SET);
                data.addEntry(new Entry(pos, update.uploadSpeed), Utils.CHART_UPLOAD_SET);
                data.notifyDataChanged();
                chart.notifyDataSetChanged();
                chart.setVisibleXRangeMaximum(90);
                chart.moveViewToX(data.getEntryCount());
            }

            progress.setText(String.format(Locale.getDefault(), "%.1f %%", update.getProgress()));
            downloadSpeed.setText(CommonUtils.speedFormatter(update.downloadSpeed, false));
            uploadSpeed.setText(CommonUtils.speedFormatter(update.uploadSpeed, false));
            remainingTime.setText(CommonUtils.timeFormatter(update.getMissingTime()));
            bitfield.update(update);

            gid.setHtml(R.string.gid, update.download().gid);
            totalLength.setHtml(R.string.total_length, CommonUtils.dimensionFormatter(update.length, false));
            completedLength.setHtml(R.string.completed_length, CommonUtils.dimensionFormatter(update.completedLength, false));
            uploadLength.setHtml(R.string.uploaded_length, CommonUtils.dimensionFormatter(update.uploadLength, false));
            pieceLength.setHtml(R.string.pieces_length, CommonUtils.dimensionFormatter(update.pieceLength, false));
            numPieces.setHtml(R.string.pieces, update.numPieces);
            connections.setHtml(R.string.connections, update.connections);
            directory.setHtml(R.string.directory, update.dir);

            verifyIntegrityPending.setHtml(R.string.verifyIntegrityPending, String.valueOf(update.verifyIntegrityPending));
            if (update.verifyIntegrityPending) {
                verifiedLength.setVisibility(View.VISIBLE);
                verifiedLength.setHtml(R.string.verifiedLength, CommonUtils.dimensionFormatter(update.verifiedLength, false));
            } else {
                verifiedLength.setVisibility(View.GONE);
            }

            bitTorrentOnly.setVisibility(update.isTorrent() ? View.VISIBLE : View.GONE);
            if (update.isTorrent()) {
                btMode.setHtml(R.string.mode, update.torrent.mode.toString());
                btSeeders.setHtml(R.string.numSeeder, update.numSeeders);
                btSeeder.setHtml(R.string.seeder, String.valueOf(update.seeder));
                shareRatio.setHtml(R.string.shareRatio, String.format(Locale.getDefault(), "%.2f", update.shareRatio()));

                if (update.torrent.comment == null) {
                    btComment.setVisibility(View.GONE);
                } else {
                    btComment.setVisibility(View.VISIBLE);
                    btComment.setHtml(R.string.comment, update.torrent.comment);
                }

                if (update.torrent.creationDate == -1) {
                    btCreationDate.setVisibility(View.GONE);
                } else {
                    btCreationDate.setVisibility(View.VISIBLE);
                    btCreationDate.setHtml(R.string.creation_date, CommonUtils.getFullDateFormatter().format(new Date(update.torrent.creationDate * 1000)));
                }
            }
        }
    }
}
