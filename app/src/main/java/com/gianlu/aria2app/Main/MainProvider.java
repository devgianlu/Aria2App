package com.gianlu.aria2app.Main;

import android.content.Context;
import android.support.annotation.NonNull;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.DownloadsAndGlobalStats;
import com.gianlu.aria2app.NetIO.Updater.PayloadProvider;
import com.gianlu.aria2app.NetIO.Updater.PayloadUpdater;
import com.gianlu.aria2app.NetIO.Updater.Wants;
import com.gianlu.aria2app.PKeys;
import com.gianlu.commonutils.Preferences.Prefs;

class MainProvider extends PayloadProvider<DownloadsAndGlobalStats> {

    MainProvider(Context context) throws Aria2Helper.InitializingException {
        super(context, Wants.downloadsAndStats());
    }

    @NonNull
    @Override
    protected PayloadUpdater<DownloadsAndGlobalStats> requireUpdater(@NonNull Context context) throws Aria2Helper.InitializingException {
        return new Updater(context);
    }

    private class Updater extends PayloadUpdater<DownloadsAndGlobalStats> implements AbstractClient.OnResult<DownloadsAndGlobalStats> {
        private final boolean hideMetadata;

        Updater(Context context) throws Aria2Helper.InitializingException {
            super(context, MainProvider.this);
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
            errorOccurred(ex);
        }
    }
}
