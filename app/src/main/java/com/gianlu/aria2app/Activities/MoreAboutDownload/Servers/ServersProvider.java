package com.gianlu.aria2app.Activities.MoreAboutDownload.Servers;

import android.content.Context;
import android.support.annotation.NonNull;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.SparseServersWithFiles;
import com.gianlu.aria2app.NetIO.Updater.PayloadProvider;
import com.gianlu.aria2app.NetIO.Updater.PayloadUpdater;
import com.gianlu.aria2app.NetIO.Updater.Wants;

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
        public void onException(Exception ex, boolean shouldForce) {
            errorOccurred(ex);
        }
    }
}
