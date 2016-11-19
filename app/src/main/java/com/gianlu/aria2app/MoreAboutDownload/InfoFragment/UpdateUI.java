package com.gianlu.aria2app.MoreAboutDownload.InfoFragment;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gianlu.aria2app.Google.UncaughtExceptionHandler;
import com.gianlu.aria2app.Main.IThread;
import com.gianlu.aria2app.MoreAboutDownload.FilesFragment.FilesAdapter;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.IDownload;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class UpdateUI implements Runnable {
    public static boolean isTorrent = false;
    public static Download.STATUS status = Download.STATUS.UNKNOWN;
    public static int fileNum = 0;
    private static boolean bitfieldEnabled = true;
    private final Activity context;
    private final InfoPagerFragment.ViewHolder holder;
    private final String gid;
    private final int updateRate;
    private boolean first = true;
    private IDownloadObserver observer = null;
    private Download.STATUS lastStatus = null;
    private JTA2 jta2;
    private boolean _shouldStop;
    private int errorCounter = 0;
    private IThread handler;

    public UpdateUI(Activity context, String gid, InfoPagerFragment.ViewHolder holder) {
        this.gid = gid;
        this.context = context;
        this.holder = holder;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        updateRate = Integer.parseInt(sharedPreferences.getString("a2_updateRate", "2")) * 1000;

        try {
            jta2 = JTA2.newInstance(context);
        } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyManagementException | KeyStoreException ex) {
            CommonUtils.logMe(context, ex);
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

    static void setBitfieldEnabled(boolean enabled) {
        bitfieldEnabled = enabled;
    }

    private void stop() {
        _shouldStop = true;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private void stop(IThread handler) {
        _shouldStop = true;
        this.handler = handler;
    }

    void setObserver(IDownloadObserver observer) {
        this.observer = observer;
    }

    @Override
    public void run() {
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());

        while ((!_shouldStop) && jta2 != null) {
            jta2.tellStatus(gid, new IDownload() {
                @Override
                public void onDownload(final Download download) {
                    errorCounter = 0;

                    isTorrent = download.isBitTorrent;
                    status = download.status;
                    fileNum = download.files.size();

                    if (first) {
                        FilesAdapter.dir = download.dir;

                        if (observer != null) {
                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    observer.onReportDownloadPieces(download.numPieces);
                                }
                            });

                            first = false;
                        }
                    }

                    if (!Objects.equals(download.status, lastStatus))
                        if (observer != null) {
                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    observer.onDownloadStatusChanged(download.status);
                                }
                            });
                        }
                    lastStatus = download.status;

                    int xPosition;
                    if (download.status == Download.STATUS.ACTIVE) {
                        LineData data = holder.chart.getData();
                        if (data == null) holder.chart = Utils.setupChart(holder.chart, false);

                        if (data != null) {
                            int pos = data.getEntryCount() / 2 + 1;
                            data.addEntry(new Entry(pos, download.downloadSpeed), Utils.CHART_DOWNLOAD_SET);
                            data.addEntry(new Entry(pos, download.uploadSpeed), Utils.CHART_UPLOAD_SET);
                            data.notifyDataChanged();
                            xPosition = pos - 91;
                        } else {
                            xPosition = -1;
                        }
                    } else {
                        xPosition = 0;
                    }

                    final int finalXPosition = xPosition;
                    context.runOnUiThread(new Runnable() {
                        @SuppressWarnings("deprecation")
                        @Override
                        public void run() {
                            holder.loading.setVisibility(View.GONE);
                            holder.container.setVisibility(View.VISIBLE);

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

                            if (bitfieldEnabled) {
                                LinearLayout.LayoutParams bitParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                holder.bitfield.setLayoutParams(bitParams);
                                holder.bitfield.removeAllViews();
                                holder.bitfield.setColumnCount(holder.bitfield.getWidth() / 40);

                                for (int piece : Utils.bitfieldProcessor(download.numPieces, download.bitfield)) {
                                    View view = new View(context);
                                    view.setBackgroundColor(Color.argb(Utils.mapAlpha(piece), 255, 87, 34));
                                    GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                                    params.setMargins(4, 4, 4, 4);
                                    params.height = 32;
                                    params.width = 32;
                                    view.setLayoutParams(params);
                                    try {
                                        holder.bitfield.addView(view);
                                    } catch (IllegalArgumentException ignored) {
                                    }
                                }
                            }

                            holder.gid.setText(Html.fromHtml(context.getString(R.string.gid, download.gid)));
                            holder.completedLength.setText(Html.fromHtml(context.getString(R.string.completed_length, CommonUtils.dimensionFormatter(download.completedLength))));
                            holder.totalLength.setText(Html.fromHtml(context.getString(R.string.total_length, CommonUtils.dimensionFormatter(download.length))));
                            holder.uploadLength.setText(Html.fromHtml(context.getString(R.string.uploaded_length, CommonUtils.dimensionFormatter(download.uploadLength))));
                            holder.pieceLength.setText(Html.fromHtml(context.getString(R.string.pieces_length, CommonUtils.dimensionFormatter(download.pieceLength))));
                            holder.numPieces.setText(Html.fromHtml(context.getString(R.string.pieces, download.numPieces)));
                            holder.connections.setText(Html.fromHtml(context.getString(R.string.connections, download.connections)));
                            holder.directory.setText(Html.fromHtml(context.getString(R.string.directory, download.dir)));

                            if (download.status == Download.STATUS.WAITING)
                                holder.verifyIntegrityPending.setText(Html.fromHtml(context.getString(R.string.verifyIntegrityPending, String.valueOf(download.verifyIntegrityPending))));
                            else
                                holder.verifyIntegrityPending.setVisibility(View.GONE);

                            if (download.verifiedLength != null) {
                                holder.verifiedLength.setText(Html.fromHtml(context.getString(R.string.verifiedLength, CommonUtils.dimensionFormatter(download.verifiedLength))));
                            } else
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
                                    holder.btCreationDate.setText(Html.fromHtml(context.getString(R.string.creation_date, new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault()).format(new Date(download.bitTorrent.creationDate)))));
                                else
                                    holder.btCreationDate.setVisibility(View.GONE);


                                View.OnClickListener trackerListener = new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                        manager.setPrimaryClip(ClipData.newPlainText("announceTracker", ((TextView) view).getText().toString()));

                                        CommonUtils.UIToast(context, context.getString(R.string.copiedClipboard));
                                    }
                                };

                                holder.btAnnounceList.removeAllViews();
                                for (String tracker : download.bitTorrent.announceList) {
                                    TextView _tracker = CommonUtils.fastTextView(context, tracker);
                                    _tracker.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                                    _tracker.setPadding(50, 10, 0, 10);
                                    _tracker.setBackground(ContextCompat.getDrawable(context, R.drawable.ripple_effect_dark));
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
                        CommonUtils.UIToast(context, Utils.ToastMessages.FAILED_GATHERING_INFORMATION, exception);
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

        if (handler != null) {
            handler.stopped();
            handler = null;
        }
    }

    public interface IDownloadObserver {
        void onDownloadStatusChanged(Download.STATUS newStatus);

        void onReportDownloadPieces(int numPieces);
    }
}
