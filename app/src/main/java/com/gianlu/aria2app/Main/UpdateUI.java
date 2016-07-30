package com.gianlu.aria2app.Main;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.gianlu.aria2app.Google.UncaughtExceptionHandler;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.GlobalStats;
import com.gianlu.aria2app.NetIO.JTA2.IDownload;
import com.gianlu.aria2app.NetIO.JTA2.IStats;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.Utils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class UpdateUI implements Runnable {
    private MainCardAdapter adapter;
    private boolean _shouldStop;
    private JTA2 jta2;
    private Activity context;
    private Integer updateRate;
    private boolean _stopped;

    public UpdateUI(Activity context, MainCardAdapter adapter) {
        _shouldStop = false;
        _stopped = false;
        this.context = context;
        this.adapter = adapter;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        updateRate = Integer.parseInt(sharedPreferences.getString("a2_updateRate", "2")) * 1000;


        try {
            jta2 = Utils.readyJTA2(context);
        } catch (IOException | NoSuchAlgorithmException ex) {
            Utils.UIToast(context, Utils.TOAST_MESSAGES.WS_EXCEPTION, ex);
            stop();
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

    private void stop() {
        _shouldStop = true;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private void stop(IThread handler) {
        _shouldStop = true;
        while (!_stopped) ;
        handler.stopped();
    }

    @Override
    public void run() {
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(context));

        while ((!_shouldStop) && jta2 != null) {
            jta2.getGlobalStat(new IStats() {
                @Override
                public void onStats(GlobalStats stats) {
                    // TODO: Update main chart
                }

                @Override
                public void onException(Exception exception) {
                    _shouldStop = true;
                    Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception);
                }
            });

            for (int c = 0; c < adapter.getItemCount(); c++) {
                final int finalC = c;
                jta2.tellStatus(adapter.getItem(c).GID, new IDownload() {
                    @Override
                    public void onDownload(Download download) {
                        adapter.updateItem(finalC, download);
                    }

                    @Override
                    public void onException(Exception exception) {
                        _shouldStop = true;
                        Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception);
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
