package com.gianlu.aria2app.activities.moreabout.info;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.gianlu.aria2app.CountryFlags;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.aria2app.activities.moreabout.BigUpdateProvider;
import com.gianlu.aria2app.activities.moreabout.OnBackPressed;
import com.gianlu.aria2app.adapters.BitfieldVisualizer;
import com.gianlu.aria2app.api.aria2.Aria2Helper;
import com.gianlu.aria2app.api.aria2.AriaException;
import com.gianlu.aria2app.api.aria2.DownloadWithUpdate;
import com.gianlu.aria2app.api.geolocalization.GeoIP;
import com.gianlu.aria2app.api.geolocalization.IPDetails;
import com.gianlu.aria2app.api.updater.PayloadProvider;
import com.gianlu.aria2app.api.updater.UpdaterFragment;
import com.gianlu.aria2app.api.updater.Wants;
import com.gianlu.aria2app.main.HideSecondSpace;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.misc.MessageView;
import com.gianlu.commonutils.misc.SuperTextView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Locale;

public class InfoFragment extends UpdaterFragment<DownloadWithUpdate.BigUpdate> implements OnBackPressed, Aria2Helper.DownloadActionClick.Listener {
    private static final String TAG = InfoFragment.class.getSimpleName();
    private final CountryFlags flags = CountryFlags.get();
    private final GeoIP geoIP = GeoIP.get();
    private ViewHolder holder;
    private MessageView message;

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
        message = holder.rootView.findViewById(R.id.infoFragment_message);
        return holder.rootView;
    }

    @Override
    public boolean onUpdateException(@NonNull Exception ex) {
        if (ex instanceof AriaException && ((AriaException) ex).isNotFound()
                && getActivity() instanceof HideSecondSpace) {
            ((HideSecondSpace) getActivity()).hideSecondSpace();
            return true;
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
        message.error(R.string.failedLoading);
        Log.e(TAG, "Failed loading info.", ex);
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

        void setup(@NonNull DownloadWithUpdate download) {
            if (getContext() == null) return;

            DownloadWithUpdate.BigUpdate update = download.bigUpdate();

            Utils.setupChart(chart, false);
            int colorAccent = ContextCompat.getColor(getContext(), update.getColorVariant());
            chart.setNoDataTextColor(colorAccent);
            bitfield.setColor(colorAccent);

            pause.setOnClickListener(new Aria2Helper.DownloadActionClick(download, Aria2Helper.WhatAction.PAUSE, InfoFragment.this));
            start.setOnClickListener(new Aria2Helper.DownloadActionClick(download, Aria2Helper.WhatAction.RESUME, InfoFragment.this));
            stop.setOnClickListener(new Aria2Helper.DownloadActionClick(download, Aria2Helper.WhatAction.STOP, InfoFragment.this));
            restart.setOnClickListener(new Aria2Helper.DownloadActionClick(download, Aria2Helper.WhatAction.RESTART, InfoFragment.this));
            remove.setOnClickListener(new Aria2Helper.DownloadActionClick(download, Aria2Helper.WhatAction.REMOVE, InfoFragment.this));
            moveUp.setOnClickListener(new Aria2Helper.DownloadActionClick(download, Aria2Helper.WhatAction.MOVE_UP, InfoFragment.this));
            moveDown.setOnClickListener(new Aria2Helper.DownloadActionClick(download, Aria2Helper.WhatAction.MOVE_DOWN, InfoFragment.this));

            if (update.torrent == null || update.torrent.announceList.isEmpty()) {
                btAnnounceListContainer.setVisibility(View.GONE);
            } else {
                btAnnounceListContainer.setVisibility(View.VISIBLE);

                toggleBtAnnounceList.setOnClickListener(v -> CommonUtils.handleCollapseClick(toggleBtAnnounceList, btAnnounceList));

                btAnnounceList.removeAllViews();
                for (String url : update.torrent.announceList) {
                    final LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.item_bt_announce, btAnnounceList, false);
                    ((TextView) layout.getChildAt(0)).setText(url);
                    ((ImageView) layout.getChildAt(1)).setImageResource(R.drawable.ic_list_unknown);
                    btAnnounceList.addView(layout);

                    try {
                        URI uri = new URI(url);
                        geoIP.getIPDetails(uri.getHost(), getActivity(), new GeoIP.OnIpDetails() {
                            @Override
                            public void onDetails(@NonNull IPDetails details) {
                                ((ImageView) layout.getChildAt(1)).setImageDrawable(flags.loadFlag(layout.getContext(), details.countryCode));
                            }

                            @Override
                            public void onException(@NonNull Exception ex) {
                            }
                        });
                    } catch (URISyntaxException ignored) {
                    }
                }
            }
        }

        void setActionsState(@NonNull DownloadWithUpdate.BigUpdate update) {
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

        boolean setChartState(@NonNull DownloadWithUpdate.BigUpdate update) {
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
                    chart.setNoDataText(update.status.getFormal(getContext(), true));
                    return false;
            }
        }

        void update(@NonNull DownloadWithUpdate.BigUpdate update) {
            if (!isAdded()) return;

            message.hide();
            loading.setVisibility(View.GONE);
            container.setVisibility(View.VISIBLE);

            setActionsState(update);

            if (setChartState(update)) {
                LineData data = chart.getLineData();
                if (data == null) {
                    Utils.setupChart(chart, true);
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
