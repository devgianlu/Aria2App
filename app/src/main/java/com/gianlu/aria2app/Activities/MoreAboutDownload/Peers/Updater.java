package com.gianlu.aria2app.Activities.MoreAboutDownload.Peers;

import android.content.Context;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.Download;
import com.gianlu.aria2app.NetIO.Aria2.Peers;
import com.gianlu.aria2app.NetIO.Updater.BaseDownloadUpdater;

class Updater extends BaseDownloadUpdater<Peers> implements AbstractClient.OnResult<Peers> {
    Updater(Context context, Download download, UpdaterListener<Peers> listener) throws Aria2Helper.InitializingException {
        super(context, download, listener);
    }

    @Override
    public void loop() {
        download.peers(this);
    }

    @Override
    public void onResult(Peers result) {
        hasResult(result);
    }

    @Override
    public void onException(Exception ex) {
        errorOccurred(ex);
    }
}
