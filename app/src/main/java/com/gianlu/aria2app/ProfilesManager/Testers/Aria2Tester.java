package com.gianlu.aria2app.ProfilesManager.Testers;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.AriaRequests;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.Logging;

import java.util.concurrent.atomic.AtomicBoolean;

class Aria2Tester extends BaseTester<Boolean> {
    Aria2Tester(Context context, MultiProfile.UserProfile profile, IPublish<Boolean> listener) {
        super(context, profile, listener);
    }

    private void publishError(Exception ex, boolean authenticated) {
        publishMessage(ex.getMessage(), R.color.red);
        if (authenticated)
            publishMessage("Your token or username and password may be wrong", R.color.red);
    }

    private <O> boolean runRequest(AbstractClient.AriaRequestWithResult<O> request, Aria2Helper helper, final boolean authenticated) {
        final AtomicBoolean lock = new AtomicBoolean(false);
        helper.request(request, new AbstractClient.OnResult<O>() {
            @Override
            public void onResult(O result) {
                synchronized (lock) {
                    lock.set(true);
                    lock.notify();
                }
            }

            @Override
            public void onException(Exception ex) {
                publishError(ex, authenticated);

                synchronized (lock) {
                    lock.set(false);
                    lock.notify();
                }
            }
        });

        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException ex) {
                Logging.log(ex);
            }

            return lock.get();
        }
    }

    @Nullable
    public Boolean call(@Nullable Object prevResult) {
        AbstractClient client;
        if (prevResult instanceof AbstractClient)
            client = (AbstractClient) prevResult;
        else
            throw new IllegalStateException("Previous result should be a client, but was " + prevResult);

        Aria2Helper helper = new Aria2Helper(context, client);
        publishMessage("Started unauthenticated request...", android.R.color.tertiary_text_light);
        if (!runRequest(AriaRequests.listMethods(), helper, false)) return null;
        publishMessage("Unauthenticated request was successful", R.color.green);

        publishMessage("Started authenticated request...", android.R.color.tertiary_text_light);
        if (!runRequest(AriaRequests.getVersion(), helper, true)) return null;
        publishMessage("Authenticated request was successful", R.color.green);

        return true;
    }

    @NonNull
    @Override
    public String describe() {
        return "aria2 test";
    }
}
