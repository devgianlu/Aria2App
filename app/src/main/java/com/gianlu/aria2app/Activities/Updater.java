package com.gianlu.aria2app.Activities;

import android.content.Context;
import android.support.annotation.NonNull;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithUpdate;
import com.gianlu.aria2app.NetIO.AriaRequests;
import com.gianlu.aria2app.NetIO.Updater.BaseUpdater;
import com.gianlu.aria2app.NetIO.Updater.UpdaterFramework;

class Updater extends BaseUpdater<DownloadWithUpdate.BigUpdate> implements AbstractClient.OnResult<DownloadWithUpdate> {
    private final String gid;

    public Updater(Context context, String gid, UpdaterFramework.Interface<DownloadWithUpdate.BigUpdate> listener) throws Aria2Helper.InitializingException {
        super(context, listener);
        this.gid = gid;
    }

    @Override
    protected void loop() {
        helper.request(AriaRequests.tellStatus(gid), this);
    }

    @Override
    public void onResult(@NonNull DownloadWithUpdate result) {
        hasResult(result.bigUpdate());
    }

    @Override
    public void onException(Exception ex, boolean shouldForce) {
        errorOccurred(ex, shouldForce);
    }
}
