package com.gianlu.aria2app.MoreAboutDownload.FilesFragment;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.gianlu.aria2app.Google.UncaughtExceptionHandler;
import com.gianlu.aria2app.Main.IThread;
import com.gianlu.aria2app.NetIO.JTA2.File;
import com.gianlu.aria2app.NetIO.JTA2.IFiles;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

class UpdateUI implements Runnable {
    private final Activity context;
    private final FilesAdapter adapter;
    private final String gid;
    private final int updateRate;
    private JTA2 jta2;
    private boolean _shouldStop;
    private int errorCounter = 0;
    private IThread handler;

    public UpdateUI(Activity context, String gid, FilesAdapter adapter) {
        this.gid = gid;
        this.context = context;
        this.adapter = adapter;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        updateRate = Integer.parseInt(sharedPreferences.getString("a2_updateRate", "2")) * 1000;

        try {
            jta2 = JTA2.newInstance(context);
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

    private void stop() {
        _shouldStop = true;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private void stop(IThread handler) {
        _shouldStop = true;
        this.handler = handler;
    }

    @Override
    public void run() {
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(context));

        while ((!_shouldStop) && jta2 != null) {
            jta2.getFiles(gid, new IFiles() {
                @Override
                public void onFiles(final List<File> files) {
                    errorCounter = 0;

                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.onUpdate(files);
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
}
