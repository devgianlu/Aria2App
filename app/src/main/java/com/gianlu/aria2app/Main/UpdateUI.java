package com.gianlu.aria2app.Main;

import android.content.Context;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.DownloadsAndGlobalStats;
import com.gianlu.aria2app.NetIO.Updater.BaseUpdater;
import com.gianlu.aria2app.PKeys;
import com.gianlu.commonutils.Preferences.Prefs;

public class UpdateUI extends BaseUpdater<DownloadsAndGlobalStats> implements AbstractClient.OnResult<DownloadsAndGlobalStats> {
    private final boolean hideMetadata;

    public UpdateUI(Context context, UpdaterListener<DownloadsAndGlobalStats> listener) throws Aria2Helper.InitializingException {
        super(context, listener);
        this.hideMetadata = Prefs.getBoolean(context, PKeys.A2_HIDE_METADATA, false);
    }

    @Override
    public void loop() {
        aria2Helper.tellAllAndGlobalStats(hideMetadata, this);
    }

    @Override
    public void onResult(DownloadsAndGlobalStats result) {
        hasResult(result);
    }

    @Override
    public void onException(Exception ex) {
        errorOccurred(ex, false);
    }
}
