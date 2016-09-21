package com.gianlu.aria2app.Main;

import android.app.Activity;
import android.preference.PreferenceManager;

import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.IDownloadList;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class LoadDownloads implements Runnable {
    private ILoading handler;
    private JTA2 jta2;
    private boolean hideMetadata;

    public LoadDownloads(Activity context, ILoading handler) {
        this.handler = handler;

        hideMetadata = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("a2_hideMetadata", false);

        try {
            jta2 = JTA2.newInstance(context);
        } catch (IOException | NoSuchAlgorithmException ex) {
            handler.onException(false, ex);
            jta2 = null;
        }
    }

    @Override
    public void run() {
        if (jta2 == null) return;

        if (handler != null)
            handler.onStarted();
        final List<Download> downloadsList = new ArrayList<>();

        //Active
        jta2.tellActive(new IDownloadList() {
            @Override
            public void onDownloads(List<Download> downloads) {
                for (Download download : downloads) {
                    if (hideMetadata && download.getName().startsWith("[METADATA]") && !download.followedBy.isEmpty())
                        continue;
                    downloadsList.add(download);
                }

                //Waiting
                jta2.tellWaiting(new IDownloadList() {
                    @Override
                    public void onDownloads(List<Download> downloads) {
                        for (Download download : downloads) {
                            if (hideMetadata && download.getName().startsWith("[METADATA]") && !download.followedBy.isEmpty())
                                continue;
                            downloadsList.add(download);
                        }

                        //Stopped
                        jta2.tellStopped(new IDownloadList() {
                            @Override
                            public void onDownloads(List<Download> downloads) {
                                for (Download download : downloads) {
                                    if (hideMetadata && download.getName().startsWith("[METADATA]") && !download.followedBy.isEmpty())
                                        continue;
                                    downloadsList.add(download);
                                }
                                if (handler != null)
                                    handler.onLoaded(jta2, downloadsList);
                            }

                            @Override
                            public void onException(boolean q, final Exception exception) {
                                if (handler != null)
                                    handler.onException(q, exception);
                            }
                        });
                    }

                    @Override
                    public void onException(boolean q, final Exception exception) {
                        if (handler != null)
                            handler.onException(q, exception);
                    }
                });
            }

            @Override
            public void onException(boolean q, final Exception exception) {
                if (handler != null)
                    handler.onException(q, exception);
            }
        });
    }

    public interface ILoading {
        void onStarted();

        void onLoaded(JTA2 jta2, List<Download> downloads);

        void onException(boolean queuing, Exception ex);
    }
}
