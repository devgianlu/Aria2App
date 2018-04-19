package com.gianlu.aria2app.Activities.MoreAboutDownload.Files;

import android.content.Context;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.AriaFile;
import com.gianlu.aria2app.NetIO.Aria2.Download;
import com.gianlu.aria2app.NetIO.Updater.BaseDownloadUpdater;
import com.gianlu.aria2app.NetIO.Updater.UpdaterFramework;

import java.util.List;

class Updater extends BaseDownloadUpdater<List<AriaFile>> implements AbstractClient.OnResult<List<AriaFile>> {
    Updater(Context context, Download download, UpdaterFramework.Interface<List<AriaFile>> listener) throws Aria2Helper.InitializingException {
        super(context, download, listener);
    }

    @Override
    public void loop() {
        download.files(this);
    }

    @Override
    public void onResult(List<AriaFile> result) {
        hasResult(result);
    }

    @Override
    public void onException(Exception ex, boolean shouldForce) {
        errorOccurred(ex, shouldForce);
    }
}
