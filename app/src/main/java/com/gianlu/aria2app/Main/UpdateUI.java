package com.gianlu.aria2app.Main;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.gianlu.aria2app.Google.UncaughtExceptionHandler;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.IDownload;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.Utils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class UpdateUI implements Runnable {
    private MainCardAdapter adapter;
    private boolean _shouldStop = false;
    private JTA2 jta2;
    private Activity context;
    private Integer updateRate;
    private int errorCounter = 0;
    private IThread handler;

    public UpdateUI(Activity context, MainCardAdapter adapter) {
        this.context = context;
        this.adapter = adapter;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        updateRate = Integer.parseInt(sharedPreferences.getString("a2_updateRate", "2")) * 1000;

        try {
            jta2 = JTA2.newInstance(context);
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
        this.handler = handler;
        _shouldStop = true;
    }

    @Override
    public void run() {
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(context));

        while ((!_shouldStop) && jta2 != null) {
            for (final Download d : adapter.getItems()) {
                jta2.tellStatus(d.gid, new IDownload() {
                    @Override
                    public void onDownload(Download download) {
                        errorCounter = 0;

                        adapter.updateItem(adapter.getItems().indexOf(d), download);
                    }

                    @Override
                    public void onException(Exception exception) {
                        if (exception.getMessage().endsWith("is not found")) return;

                        Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception);

                        errorCounter++;
                        if (errorCounter >= 2) _shouldStop = true;
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

        _shouldStop = false;
        if (handler != null)
            handler.stopped();
    }
}
