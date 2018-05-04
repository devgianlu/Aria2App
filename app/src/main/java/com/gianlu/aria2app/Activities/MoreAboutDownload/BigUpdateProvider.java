package com.gianlu.aria2app.Activities.MoreAboutDownload;

import android.content.Context;
import android.support.annotation.NonNull;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithUpdate;
import com.gianlu.aria2app.NetIO.AriaRequests;
import com.gianlu.aria2app.NetIO.Updater.PayloadProvider;
import com.gianlu.aria2app.NetIO.Updater.PayloadUpdater;
import com.gianlu.aria2app.NetIO.Updater.Wants;

public class BigUpdateProvider extends PayloadProvider<DownloadWithUpdate.BigUpdate> {
    private final String gid;

    public BigUpdateProvider(@NonNull Context context, String gid) throws Aria2Helper.InitializingException {
        super(context, Wants.bigUpdate(gid));
        this.gid = gid;
    }

    @NonNull
    @Override
    protected PayloadUpdater<DownloadWithUpdate.BigUpdate> requireUpdater(@NonNull Context context) throws Aria2Helper.InitializingException {
        return new Updater(context, this);
    }

    private class Updater extends PayloadUpdater<DownloadWithUpdate.BigUpdate> implements AbstractClient.OnResult<DownloadWithUpdate> {

        public Updater(Context context, OnPayload<DownloadWithUpdate.BigUpdate> listener) throws Aria2Helper.InitializingException {
            super(context, listener);
        }

        @Override
        protected void loop() {
            if (lastPayload != null) {
                helper.request(AriaRequests.tellStatus(lastPayload.download()), this);
            } else {
                helper.request(AriaRequests.tellStatus(gid), this);
            }
        }

        @Override
        public void onResult(@NonNull DownloadWithUpdate result) {
            hasResult(result.bigUpdate());
        }

        @Override
        public void onException(Exception ex, boolean shouldForce) {
            errorOccurred(ex);
        }
    }
}
