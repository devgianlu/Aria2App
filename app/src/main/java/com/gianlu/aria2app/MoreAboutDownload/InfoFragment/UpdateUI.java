package com.gianlu.aria2app.MoreAboutDownload.InfoFragment;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gianlu.aria2app.Google.UncaughtExceptionHandler;
import com.gianlu.aria2app.Main.IThread;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.IDownload;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class UpdateUI implements Runnable {
    private Activity context;
    private InfoPagerFragment.ViewHolder holder;
    private JTA2 jta2;
    private String gid;
    private int updateRate;
    private boolean _shouldStop;
    private boolean _stopped;
    private int errorCounter = 0;

    public UpdateUI(Activity context, String gid, InfoPagerFragment.ViewHolder holder) {
        this.gid = gid;
        this.context = context;
        this.holder = holder;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        updateRate = Integer.parseInt(sharedPreferences.getString("a2_updateRate", "2")) * 1000;

        try {
            jta2 = Utils.readyJTA2(context);
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static void stop(UpdateUI updateUI) {
        if (updateUI != null) updateUI.stop();
    }

    public static void stop(UpdateUI updateUI, IThread handler) {
        if (updateUI == null)
            handler.stopped();
        else
            updateUI.stop(handler);
    }

    public void stop() {
        _shouldStop = true;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    public void stop(IThread handler) {
        _shouldStop = true;
        while (!_stopped) ;
        handler.stopped();
    }

    @Override
    public void run() {
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(context));

        while ((!_shouldStop) && jta2 != null) {
            jta2.tellStatus(gid, new IDownload() {
                @Override
                public void onDownload(final Download download) {
                    errorCounter = 0;

                    int xPosition;
                    if (download.status == Download.STATUS.ACTIVE) {
                        LineData data = holder.chart.getData();
                        if (data == null) holder.chart = Utils.setupChart(holder.chart, false);

                        if (data != null) {
                            data.addXValue(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new java.util.Date()));
                            data.addEntry(new Entry(download.downloadSpeed, data.getDataSetByIndex(Utils.CHART_DOWNLOAD_SET).getEntryCount()), Utils.CHART_DOWNLOAD_SET);
                            data.addEntry(new Entry(download.uploadSpeed, data.getDataSetByIndex(Utils.CHART_UPLOAD_SET).getEntryCount()), Utils.CHART_UPLOAD_SET);
                            xPosition = data.getXValCount() - 91;
                        } else {
                            xPosition = -1;
                        }
                    } else {
                        xPosition = 0;
                    }

                    final int finalXPosition = xPosition;
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (download.status == Download.STATUS.ACTIVE) {
                                holder.chartRefresh.setEnabled(true);

                                if (finalXPosition != -1) {
                                    holder.chart.notifyDataSetChanged();
                                    holder.chart.setVisibleXRangeMaximum(90);
                                    holder.chart.moveViewToX(finalXPosition);
                                }
                            } else {
                                holder.chartRefresh.setEnabled(false);

                                holder.chart.clear();
                                holder.chart.setNoDataText(context.getString(R.string.downloadIs, download.status.getFormal(context, false)));
                            }

                            /* TODO: Bitfield representation
                            if (first) {
                                boolean[] pieces = Utils.bitfieldProcessor(download.numPieces, download.bitfield);
                                System.out.println(Arrays.toString(pieces));
                                first = false;
                            }
                            */

                            holder.gid.setText(Html.fromHtml(context.getString(R.string.gid, download.gid)));
                            holder.completedLength.setText(Html.fromHtml(context.getString(R.string.completed_length, Utils.dimensionFormatter(download.completedLength))));
                            holder.totalLength.setText(Html.fromHtml(context.getString(R.string.total_length, Utils.dimensionFormatter(download.length))));
                            holder.uploadLength.setText(Html.fromHtml(context.getString(R.string.uploaded_length, Utils.dimensionFormatter(download.uploadedLength))));
                            holder.pieceLength.setText(Html.fromHtml(context.getString(R.string.pieces_length, Utils.dimensionFormatter(download.pieceLength))));
                            holder.numPieces.setText(Html.fromHtml(context.getString(R.string.pieces, download.numPieces)));
                            holder.connections.setText(Html.fromHtml(context.getString(R.string.connections, download.connections)));
                            holder.directory.setText(Html.fromHtml(context.getString(R.string.directory, download.dir)));

                            if (download.status == Download.STATUS.WAITING)
                                holder.verifyIntegrityPending.setText(Html.fromHtml(context.getString(R.string.verifyIntegrityPending, String.valueOf(download.verifyIntegrityPending))));
                            else
                                holder.verifyIntegrityPending.setVisibility(View.GONE);

                            if (download.verifiedLength != null)
                                holder.verifiedLength.setText(Html.fromHtml(context.getString(R.string.verifiedLength, Utils.dimensionFormatter(download.verifiedLength))));
                            else
                                holder.verifiedLength.setVisibility(View.GONE);


                            if (download.isBitTorrent) {
                                holder.btMode.setText(Html.fromHtml(context.getString(R.string.mode, download.bitTorrent.mode.toString())));
                                holder.btSeeders.setText(Html.fromHtml(context.getString(R.string.numSeeder, download.numSeeders)));
                                holder.btSeeder.setText(Html.fromHtml(context.getString(R.string.seeder, String.valueOf(download.seeder))));
                                if (download.bitTorrent.comment != null && (!download.bitTorrent.comment.isEmpty()))
                                    holder.btComment.setText(Html.fromHtml(context.getString(R.string.comment, download.bitTorrent.comment)));
                                else
                                    holder.btComment.setVisibility(View.GONE);
                                if (download.bitTorrent.creationDate != null)
                                    holder.btCreationDate.setText(Html.fromHtml(context.getString(R.string.creation_date, new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault()).format(new java.util.Date(download.bitTorrent.creationDate)))));
                                else
                                    holder.btCreationDate.setVisibility(View.GONE);


                                View.OnClickListener trackerListener = new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                        manager.setPrimaryClip(ClipData.newPlainText("announceTracker", ((TextView) view).getText().toString()));

                                        Utils.UIToast(context, context.getString(R.string.copiedClipboard));
                                    }
                                };

                                holder.btAnnounceList.removeAllViews();
                                for (String tracker : download.bitTorrent.announceList) {
                                    TextView _tracker = new TextView(context);
                                    _tracker.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                                    _tracker.setPadding(50, 10, 0, 10);
                                    _tracker.setBackground(ContextCompat.getDrawable(context, R.drawable.ripple_effect_dark));
                                    _tracker.setText(tracker);
                                    _tracker.setOnClickListener(trackerListener);

                                    holder.btAnnounceList.addView(_tracker);
                                }
                            } else {
                                holder.bitTorrentOnly.setVisibility(View.GONE);
                            }
                        }
                    });
                }

                @Override
                public void onException(Exception exception) {
                    errorCounter++;
                    if (errorCounter <= 2) {
                        Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception);
                    } else {
                        _shouldStop = true;
                    }
                }
            });

            try {
                Thread.sleep(updateRate);
            } catch (InterruptedException ex) {
                _shouldStop = true;
                break;
            }
        }

        _stopped = true;
    }
}
