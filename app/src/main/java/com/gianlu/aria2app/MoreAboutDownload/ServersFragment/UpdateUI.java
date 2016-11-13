package com.gianlu.aria2app.MoreAboutDownload.ServersFragment;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.gianlu.aria2app.Google.UncaughtExceptionHandler;
import com.gianlu.aria2app.Main.IThread;
import com.gianlu.aria2app.NetIO.JTA2.IServers;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.JTA2.Server;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Map;

class UpdateUI implements Runnable {
    private final Activity context;
    private final ServerCardAdapter adapter;
    private final String gid;
    private final int updateRate;
    private JTA2 jta2;
    private boolean _shouldStop;
    private int errorCounter = 0;
    private IThread handler;

    public UpdateUI(Activity context, String gid, ServerCardAdapter adapter) {
        this.gid = gid;
        this.context = context;
        this.adapter = adapter;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        updateRate = Integer.parseInt(sharedPreferences.getString("a2_updateRate", "2")) * 1000;

        try {
            jta2 = JTA2.newInstance(context);
        } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyManagementException | KeyStoreException e) {
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
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());

        while ((!_shouldStop) && jta2 != null) {
            jta2.getServers(gid, new IServers() {
                @Override
                public void onServers(final Map<Integer, List<Server>> servers) {
                    errorCounter = 0;

                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.onUpdate(servers);
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

                @Override
                public void onDownloadNotActive(final Exception exception) {
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.onDisplayNoData(exception.getMessage());
                        }
                    });
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
