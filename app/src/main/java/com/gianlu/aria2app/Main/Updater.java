package com.gianlu.aria2app.Main;

import android.content.Context;
import android.support.annotation.NonNull;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.DownloadsAndGlobalStats;
import com.gianlu.aria2app.NetIO.Updater.BaseUpdater;
import com.gianlu.aria2app.NetIO.Updater.UpdaterFramework;
import com.gianlu.aria2app.PKeys;
import com.gianlu.commonutils.Preferences.Prefs;

class Updater extends BaseUpdater<DownloadsAndGlobalStats> implements AbstractClient.OnResult<DownloadsAndGlobalStats> {
    private final boolean hideMetadata;

    Updater(Context context, UpdaterFramework.Interface<DownloadsAndGlobalStats> listener) throws Aria2Helper.InitializingException {
        super(context, listener);
        this.hideMetadata = Prefs.getBoolean(context, PKeys.A2_HIDE_METADATA, false);
    }

    @Override
    public void loop() {
        helper.tellAllAndGlobalStats(hideMetadata, this);
    }

    @Override
    public void onResult(@NonNull DownloadsAndGlobalStats result) {
        hasResult(result);
    }

    @Override
    public void onException(Exception ex, boolean shouldForce) {
        errorOccurred(ex, shouldForce);
    }
}
