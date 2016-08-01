package com.gianlu.aria2app.MoreAboutDownload.InfoFragment;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Html;

import com.gianlu.aria2app.DownloadsListing.Charting;
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
                    LineData data = holder.chart.getData();
                    data.addXValue(new SimpleDateFormat("hh:mm:ss", Locale.getDefault()).format(new java.util.Date()));
                    data.addEntry(new Entry(download.downloadSpeed, data.getDataSetByIndex(Charting.DOWNLOAD_SET).getEntryCount()), Charting.DOWNLOAD_SET);
                    data.addEntry(new Entry(download.uploadSpeed, data.getDataSetByIndex(Charting.UPLOAD_SET).getEntryCount()), Charting.UPLOAD_SET);
                    holder.chart.notifyDataSetChanged();
                    holder.chart.setVisibleXRangeMaximum(90);
                    holder.chart.moveViewToX(data.getXValCount() - 91);

                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            holder.completedLength.setText(Html.fromHtml(context.getString(R.string.completed_length, Utils.DimensionFormatter(download.completedLength))));
                            holder.totalLength.setText(Html.fromHtml(context.getString(R.string.total_length, Utils.DimensionFormatter(download.length))));
                            holder.uploadLength.setText(Html.fromHtml(context.getString(R.string.uploaded_length, Utils.DimensionFormatter(download.uploadedLength))));
                        }
                    });
                }

                @Override
                public void onException(Exception exception) {

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
