package com.gianlu.aria2app.Activities.MoreAboutDownload.Servers;

import android.content.Context;
import android.util.SparseArray;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.AriaException;
import com.gianlu.aria2app.NetIO.Aria2.Download;
import com.gianlu.aria2app.NetIO.Aria2.Servers;
import com.gianlu.aria2app.NetIO.Updater.BaseDownloadUpdater;
import com.gianlu.aria2app.NetIO.Updater.UpdaterFramework;

class Updater extends BaseDownloadUpdater<SparseArray<Servers>> implements AbstractClient.OnResult<SparseArray<Servers>> {
    Updater(Context context, Download download, UpdaterFramework.Interface<SparseArray<Servers>> listener) throws Aria2Helper.InitializingException {
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
        if (ex instanceof AriaException && ex.getMessage().startsWith("No active download")) {
            hasResult(new SparseArray<Servers>());
        } else {
            errorOccurred(ex, shouldForce);
        }
    }
}

