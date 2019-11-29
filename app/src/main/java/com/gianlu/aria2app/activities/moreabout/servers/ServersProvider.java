package com.gianlu.aria2app.activities.moreabout.servers;

import android.content.Context;

import com.gianlu.aria2app.api.AbstractClient;
import com.gianlu.aria2app.api.aria2.Aria2Helper;
import com.gianlu.aria2app.api.aria2.SparseServersWithFiles;
import com.gianlu.aria2app.api.updater.PayloadProvider;
import com.gianlu.aria2app.api.updater.PayloadUpdater;
import com.gianlu.aria2app.api.updater.Wants;

import androidx.annotation.NonNull;

public class ServersProvider extends PayloadProvider<SparseServersWithFiles> {
    private final String gid;

    ServersProvider(@NonNull Context context, String gid) throws Aria2Helper.InitializingException {
        super(context, Wants.serversAndFiles(gid));
        this.gid = gid;
    }

    @NonNull
    @Override
    protected PayloadUpdater<SparseServersWithFiles> requireUpdater(@NonNull Context context) throws Aria2Helper.InitializingException {
        return new Updater(context, this);
    }

    private class Updater extends PayloadUpdater<SparseServersWithFiles> implements AbstractClient.OnResult<SparseServersWithFiles> {

        public Updater(Context context, OnPayload<SparseServersWithFiles> listener) throws Aria2Helper.InitializingException {
            super(context, listener);
        }

        @Override
        protected void loop() {
            helper.getServersAndFiles(gid, this);
        }

        @Override
        public void onResult(@NonNull SparseServersWithFiles result) {
            hasResult(result);
        }

        @Override
        public void onException(@NonNull Exception ex) {
            errorOccurred(ex);
        }
    }
}
