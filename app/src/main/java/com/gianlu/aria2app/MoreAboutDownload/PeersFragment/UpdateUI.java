package com.gianlu.aria2app.MoreAboutDownload.PeersFragment;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.gianlu.aria2app.Google.UncaughtExceptionHandler;
import com.gianlu.aria2app.Main.IThread;
import com.gianlu.aria2app.NetIO.JTA2.IPeers;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.JTA2.Peer;
import com.gianlu.aria2app.Utils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class UpdateUI implements Runnable {
    private Activity context;
    private PeerCardAdapter adapter;
    private JTA2 jta2;
    private String gid;
    private int updateRate;
    private boolean _shouldStop;
    private boolean _stopped;
    private int errorCounter = 0;

    public UpdateUI(Activity context, String gid, PeerCardAdapter adapter) {
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
            jta2.getPeers(gid, new IPeers() {
                @Override
                public void onPeers(final List<Peer> peers) {
                    errorCounter = 0;

                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.onUpdate(peers);
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

                @Override
                public void onNoPeerData(Exception exception) {
                    adapter.onDisplayNoData(exception.getMessage());
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
