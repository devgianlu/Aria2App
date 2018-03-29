package com.gianlu.aria2app.Activities.MoreAboutDownload.Info;

import android.content.Context;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.Download;
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithHelper;
import com.gianlu.aria2app.NetIO.Updater.BaseDownloadUpdater;

public class UpdateUI extends BaseDownloadUpdater<Download> implements AbstractClient.OnResult<DownloadWithHelper> {
    public UpdateUI(Context context, Download download, UpdaterListener<Download> listener) throws Aria2Helper.InitializingException {
        super(context, download, listener);
    }

    @Override
    public void loop() {
        download.update(this);
    }

    @Override
    public void onResult(DownloadWithHelper result) {
        hasResult(result.get());
    }

    @Override
    public void onException(Exception ex) {
        errorOccurred(ex, false);
    }
}
