package com.gianlu.aria2app.Activities.MoreAboutDownload.Servers;

import android.content.Context;
import android.util.SparseArray;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.Download;
import com.gianlu.aria2app.NetIO.Aria2.Servers;
import com.gianlu.aria2app.NetIO.Updater.BaseDownloadUpdater;

class Updater extends BaseDownloadUpdater<SparseArray<Servers>> implements AbstractClient.OnResult<SparseArray<Servers>> {
    Updater(Context context, Download download, UpdaterListener<SparseArray<Servers>> listener) throws Aria2Helper.InitializingException {
        super(context, download, listener);
    }

    @Override
    public void loop() {
        download.servers(this);
    }

    @Override
    public void onResult(SparseArray<Servers> result) {
        hasResult(result);
    }

    @Override
    public void onException(Exception ex, boolean shouldForce) {
        errorOccurred(ex); // TODO: Handle no servers
    }
}

