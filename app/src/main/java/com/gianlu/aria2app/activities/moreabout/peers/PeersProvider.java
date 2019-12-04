package com.gianlu.aria2app.activities.moreabout.peers;

import android.content.Context;

import com.gianlu.aria2app.api.AbstractClient;
import com.gianlu.aria2app.api.aria2.Aria2Helper;
import com.gianlu.aria2app.api.aria2.Peers;
import com.gianlu.aria2app.api.AriaRequests;
import com.gianlu.aria2app.api.updater.PayloadProvider;
import com.gianlu.aria2app.api.updater.PayloadUpdater;
import com.gianlu.aria2app.api.updater.Wants;

import androidx.annotation.NonNull;

public class PeersProvider extends PayloadProvider<Peers> {
    private final String gid;

    PeersProvider(@NonNull Context context, String gid) throws Aria2Helper.InitializingException {
        super(context, Wants.peers(gid));
        this.gid = gid;
    }

    @NonNull
    @Override
    protected PayloadUpdater<Peers> requireUpdater(@NonNull Context context) throws Aria2Helper.InitializingException {
        return new Updater(context, this);
    }

    private class Updater extends PayloadUpdater<Peers> implements AbstractClient.OnResult<Peers> {

        public Updater(Context context, OnPayload<Peers> listener) throws Aria2Helper.InitializingException {
            super(context, listener);
        }

        @Override
        protected void loop() {
            helper.request(AriaRequests.getPeers(gid), this);
        }

        @Override
        public void onResult(@NonNull Peers result) {
            hasResult(result);
        }

        @Override
        public void onException(@NonNull Exception ex) {
            errorOccurred(ex);
        }
    }
}
