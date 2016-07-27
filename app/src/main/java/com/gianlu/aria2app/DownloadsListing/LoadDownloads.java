package com.gianlu.aria2app.DownloadsListing;

import android.app.Activity;
import android.preference.PreferenceManager;
import android.widget.ListView;

import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.IDownloadList;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.Utils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class LoadDownloads implements Runnable {
    private ListView listView;
    private Activity context;
    private ILoadDownloads handler;
    private JTA2 jta2;
    private boolean hideMetadata;

    public LoadDownloads(Activity context, ListView listView, ILoadDownloads handler) {
        this.listView = listView;
        this.context = context;
        this.handler = handler;

        hideMetadata = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("a2_hideMetadata", false);

        try {
            jta2 = Utils.readyJTA2(context);
        } catch (IOException | NoSuchAlgorithmException ex) {
            handler.onException(ex);
        }
    }

    @Override
    public void run() {
        handler.onStart();
        if (jta2 == null) return;
        final List<DownloadItem> downloadsList = new ArrayList<>();

        //Active
        jta2.tellActive(new IDownloadList() {
            @Override
            public void onDownloads(List<Download> downloads) {
                for (Download download : downloads) {
                    if (hideMetadata && download.getName().startsWith("[METADATA]") && !download.followedBy.isEmpty())
                        continue;
                    downloadsList.add(new DownloadItem(download));
                }

                //Waiting
                jta2.tellWaiting(new IDownloadList() {
                    @Override
                    public void onDownloads(List<Download> downloads) {
                        for (Download download : downloads) {
                            if (hideMetadata && download.getName().startsWith("[METADATA]") && !download.followedBy.isEmpty())
                                continue;
                            downloadsList.add(new DownloadItem(download));
                        }

                        //Stopped
                        jta2.tellStopped(new IDownloadList() {
                            @Override
                            public void onDownloads(List<Download> downloads) {
                                for (Download download : downloads) {
                                    if (hideMetadata && download.getName().startsWith("[METADATA]") && !download.followedBy.isEmpty())
                                        continue;
                                    downloadsList.add(new DownloadItem(download));
                                }
                                final DownloadItemAdapter adapter = new DownloadItemAdapter(context, downloadsList);
                                context.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        listView.setAdapter(adapter);
                                        handler.onEnd();
                                    }
                                });
                            }

                            @Override
                            public void onException(final Exception exception) {
                                Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception);
                                handler.onEnd();
                            }
                        });
                    }

                    @Override
                    public void onException(final Exception exception) {
                        Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception);
                        handler.onEnd();
                    }
                });
            }

            @Override
            public void onException(final Exception exception) {
                Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception);
                handler.onEnd();
            }
        });
    }
}
