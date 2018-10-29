package com.gianlu.aria2app.Main;

import android.content.Context;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.DownloadsAndGlobalStats;
import com.gianlu.aria2app.NetIO.Updater.PayloadProvider;
import com.gianlu.aria2app.NetIO.Updater.PayloadUpdater;
import com.gianlu.aria2app.NetIO.Updater.Wants;

import androidx.annotation.NonNull;

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
        Updater(Context context) throws Aria2Helper.InitializingException {
            super(context, MainProvider.this);
        }

        @Override
        public void loop() {
            helper.tellAllAndGlobalStats(this);
        }

        @Override
        public void onResult(@NonNull DownloadsAndGlobalStats result) {
            hasResult(result);
        }

        @Override
        public void onException(Exception ex) {
            errorOccurred(ex);
        }
    }
}
