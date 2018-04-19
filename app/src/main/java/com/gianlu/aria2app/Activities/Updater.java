package com.gianlu.aria2app.Activities;

import android.content.Context;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithHelper;
import com.gianlu.aria2app.NetIO.AriaRequests;
import com.gianlu.aria2app.NetIO.Updater.BaseUpdater;
import com.gianlu.aria2app.NetIO.Updater.UpdaterFramework;

class Updater extends BaseUpdater<DownloadWithHelper> implements AbstractClient.OnResult<DownloadWithHelper> {
    private final String gid;

    public Updater(Context context, String gid, UpdaterFramework.Interface<DownloadWithHelper> listener) throws Aria2Helper.InitializingException {
        super(context, listener);
        this.gid = gid;
    }

    @Override
    protected void loop() {
        helper.request(AriaRequests.tellStatus(gid), this);
    }

    @Override
    public void onResult(DownloadWithHelper result) {
        hasResult(result);
    }

    @Override
    public void onException(Exception ex, boolean shouldForce) {
        errorOccurred(ex, shouldForce);
    }
}
