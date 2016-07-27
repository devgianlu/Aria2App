package com.gianlu.aria2app.Main;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.ListView;

import com.gianlu.aria2app.DownloadsListing.Charting;
import com.gianlu.aria2app.DownloadsListing.DownloadItem;
import com.gianlu.aria2app.Google.UncaughtExceptionHandler;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.GlobalStats;
import com.gianlu.aria2app.NetIO.JTA2.IDownload;
import com.gianlu.aria2app.NetIO.JTA2.IStats;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.Utils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class UpdateUI implements Runnable {
    private LineChart chart;
    private ListView downloadsListView;
    private boolean _shouldStop;
    private JTA2 jta2;
    private Activity context;
    private Integer updateRate;
    private boolean _stopped;

    public UpdateUI(Activity context, final LineChart chart, ListView downloadsListView) {
        _shouldStop = false;
        _stopped = false;
        this.context = context;
        this.chart = chart;
        this.downloadsListView = downloadsListView;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        updateRate = Integer.parseInt(sharedPreferences.getString("a2_updateRate", "2")) * 1000;


        try {
            jta2 = Utils.readyJTA2(context);
        } catch (IOException | NoSuchAlgorithmException ex) {
            Utils.UIToast(context, Utils.TOAST_MESSAGES.WS_EXCEPTION, ex);
            stop();
        }
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

        final LineData data = chart.getData();
        ILineDataSet downloadTmp = data.getDataSetByIndex(0);
        ILineDataSet uploadTmp = data.getDataSetByIndex(1);

        while ((!_shouldStop) && jta2 != null) {
            // ---- Update chart ---- //
            if (downloadTmp == null) {
                downloadTmp = Charting.InitDownloadSet(context);
                data.addDataSet(downloadTmp);
            }

            if (uploadTmp == null) {
                uploadTmp = Charting.InitUploadSet(context);
                data.addDataSet(uploadTmp);
            }

            final ILineDataSet downloadSet = downloadTmp;
            final ILineDataSet uploadSet = uploadTmp;

            jta2.getGlobalStat(new IStats() {
                @Override
                public void onStats(GlobalStats stats) {
                    data.addXValue(new SimpleDateFormat("hh:mm:ss", Locale.getDefault()).format(new java.util.Date()));
                    data.addEntry(new Entry(stats.downloadSpeed.floatValue(), downloadSet.getEntryCount()), 0);
                    data.addEntry(new Entry(stats.uploadSpeed.floatValue(), uploadSet.getEntryCount()), 1);

                    chart.notifyDataSetChanged();
                    chart.setVisibleXRangeMaximum(90);
                    chart.moveViewToX(data.getXValCount() - 91);
                }

                @Override
                public void onException(Exception exception) {
                    _shouldStop = true;
                    Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception);
                }
            });

            // ---- Update ListView ---- //
            for (int c = 0; c < downloadsListView.getCount(); c++) {
                final DownloadItem downloadItem = (DownloadItem) downloadsListView.getItemAtPosition(c);
                final String downloadGID = downloadItem.getDownloadGID();

                jta2.tellStatus(downloadGID, new IDownload() {
                    @Override
                    public void onDownload(Download download) {
                        downloadItem.download = download;
                    }

                    @Override
                    public void onException(Exception exception) {
                        _shouldStop = true;
                        Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception);
                    }
                });

                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        downloadsListView.invalidateViews();
                    }
                });
            }

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
