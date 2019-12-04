package com.gianlu.aria2app.main;

import android.content.Context;

import com.gianlu.aria2app.api.AbstractClient;
import com.gianlu.aria2app.api.aria2.Aria2Helper;
import com.gianlu.aria2app.api.aria2.DownloadsAndGlobalStats;
import com.gianlu.aria2app.api.updater.PayloadProvider;
import com.gianlu.aria2app.api.updater.PayloadUpdater;
import com.gianlu.aria2app.api.updater.Wants;

import androidx.annotation.NonNull;

class MainProvider extends PayloadProvider<DownloadsAndGlobalStats> {

    MainProvider(Context context) throws Aria2Helper.InitializingException {
        super(context.getApplicationContext(), Wants.downloadsAndStats());
    }

    @NonNull
    @Override
    protected PayloadUpdater<DownloadsAndGlobalStats> requireUpdater(@NonNull Context context) throws Aria2Helper.InitializingException {
        return new Updater(context);
    }

    private class Updater extends PayloadUpdater<DownloadsAndGlobalStats> implements AbstractClient.OnResult<DownloadsAndGlobalStats> {
        Updater(Context context) throws Aria2Helper.InitializingException {
            super(context.getApplicationContext(), MainProvider.this);
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
        public void onException(@NonNull Exception ex) {
            errorOccurred(ex);
        }
    }
}
