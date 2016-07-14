package com.gianlu.aria2app.Main;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.ListView;

import com.gianlu.aria2app.DownloadsListing.Charting;
import com.gianlu.aria2app.DownloadsListing.DownloadItem;
import com.gianlu.aria2app.Google.UncaughtExceptionHandler;
import com.gianlu.aria2app.Utils;
import com.gianlu.jtitan.Aria2Helper.Download;
import com.gianlu.jtitan.Aria2Helper.GlobalStats;
import com.gianlu.jtitan.Aria2Helper.JTA2;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class UpdateUI implements Runnable {
    private LineChart chart;
    private ListView downloadsListView;
    private boolean _shouldStop;
    private JTA2 jta2;
    private Activity context;
    private Integer updateRate;
    private int errorCount = 0;
    private boolean _stopped;

    public UpdateUI(Activity context, final LineChart chart, ListView downloadsListView) {
        _shouldStop = false;
        _stopped = false;
        this.context = context;
        this.chart = chart;
        this.downloadsListView = downloadsListView;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        updateRate = Integer.parseInt(sharedPreferences.getString("a2_updateRate", "2")) * 1000;

        jta2 = Utils.readyJTA2(context);
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

        while (!_shouldStop) {
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

            GlobalStats globalStats;
            try {
                globalStats = jta2.getGlobalStat();
            } catch (IOException ex) {
                Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex.getMessage());
                _shouldStop = true;
                break;
            }

            data.addXValue(new SimpleDateFormat("hh:mm:ss", Locale.getDefault()).format(new java.util.Date()));
            data.addEntry(new Entry(globalStats.downloadSpeed.floatValue(), downloadSet.getEntryCount()), 0);
            data.addEntry(new Entry(globalStats.uploadSpeed.floatValue(), uploadSet.getEntryCount()), 1);

            chart.notifyDataSetChanged();
            chart.setVisibleXRangeMaximum(90);
            chart.moveViewToX(data.getXValCount() - 91);

            // ---- Update ListView ---- //
            for (int c = 0; c < downloadsListView.getCount(); c++) {
                final DownloadItem downloadItem = (DownloadItem) downloadsListView.getItemAtPosition(c);
                final String downloadGID = downloadItem.getDownloadGID();

                Download status;
                try {
                    status = jta2.tellStatus(downloadGID);
                    errorCount = 0;

                    downloadItem.download = status;
                } catch (IOException ex) {
                    errorCount += 1;
                    if (errorCount > 6) {
                        Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex.getMessage());
                        _shouldStop = true;
                        break;
                    }
                }


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
