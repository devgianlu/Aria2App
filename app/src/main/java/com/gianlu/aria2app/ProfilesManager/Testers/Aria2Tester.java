package com.gianlu.aria2app.ProfilesManager.Testers;

import android.content.Context;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.AriaException;
import com.gianlu.aria2app.NetIO.Aria2.VersionInfo;
import com.gianlu.aria2app.NetIO.AriaRequests;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.commonutils.Logging;

import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class Aria2Tester extends BaseTester<Boolean> {
    Aria2Tester(Context context, MultiProfile.UserProfile profile, PublishListener<Boolean> listener) {
        super(context, profile, listener);
    }

    private void publishError(Exception ex, boolean authenticated) {
        publishMessage(ex.getClass().getName() + ": " + ex.getMessage(), Level.ERROR);
        if (authenticated)
            publishMessage("Your token or username and password may be wrong", Level.ERROR);
    }

    private <O> Object runRequest(AbstractClient.AriaRequestWithResult<O> request, Aria2Helper helper) {
        final AtomicReference<Object> lock = new AtomicReference<>(null);
        helper.request(request, new AbstractClient.OnResult<O>() {
            @Override
            public void onResult(@NonNull O result) {
                synchronized (lock) {
                    lock.set(result);
                    lock.notify();
                }
            }

            @Override
            public void onException(Exception ex) {
                synchronized (lock) {
                    lock.set(ex);
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

        Aria2Helper helper = new Aria2Helper(client);
        publishMessage("Started unauthenticated request...", Level.INFO);

        Object unauthenticatedResult = runRequest(AriaRequests.listMethods(), helper);
        if (unauthenticatedResult instanceof Exception) {
            if (unauthenticatedResult instanceof AriaException) {
                if (((AriaException) unauthenticatedResult).getMessage().contains("No such method")) {
                    Object authenticatedResult = runAuthenticated(helper);
                    if (authenticatedResult instanceof VersionInfo) {
                        publishMessage("Skipped unauthenticated request due to outdated version ("
                                + ((VersionInfo) authenticatedResult).version
                                + "), assuming to be successful...", Level.INFO);
                        return true;
                    }
                }
            }

            publishError((Exception) unauthenticatedResult, false);
            return null;
        } else {
            publishMessage("Unauthenticated request was successful", Level.SUCCESS);

            if (runAuthenticated(helper) instanceof Exception) return null;
            else return true;
        }
    }

    @NonNull
    private Object runAuthenticated(Aria2Helper helper) {
        publishMessage("Started authenticated request...", Level.INFO);
        Object result = runRequest(AriaRequests.getVersion(), helper);
        if (result instanceof Exception) publishError((Exception) result, true);
        else publishMessage("Authenticated request was successful", Level.SUCCESS);
        return result;
    }

    @NonNull
    @Override
    public String describe() {
        return "aria2 test";
    }
}
