package com.gianlu.aria2app.profiles.testers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.aria2app.api.AbstractClient;
import com.gianlu.aria2app.api.aria2.Aria2Helper;
import com.gianlu.aria2app.api.aria2.AriaException;
import com.gianlu.aria2app.api.aria2.VersionInfo;
import com.gianlu.aria2app.api.AriaRequests;
import com.gianlu.aria2app.profiles.MultiProfile;
import com.gianlu.commonutils.logging.Logging;

import java.util.concurrent.atomic.AtomicReference;

class Aria2Tester extends BaseTester<Boolean> {
    Aria2Tester(Context context, MultiProfile.UserProfile profile, PublishListener<Boolean> listener) {
        super(context, profile, listener);
    }

    private void publishError(@NonNull Exception ex, boolean authenticated) {
        publishMessage(ex.getClass().getName() + ": " + ex.getMessage(), Level.ERROR);
        if (authenticated)
            publishMessage("Your token or username and password may be wrong", Level.ERROR);
    }

    @Nullable
    private <O> Object runRequest(@NonNull AbstractClient.AriaRequestWithResult<O> request, @NonNull Aria2Helper helper) {
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
            public void onException(@NonNull Exception ex) {
                synchronized (lock) {
                    lock.set(ex);
                    lock.notify();
                }
            }
        });

        synchronized (lock) {
            try {
                lock.wait(5000);
            } catch (InterruptedException ex) {
                Logging.log(ex);
            }

            return lock.get();
        }
    }

    @Nullable
    private Boolean call(@NonNull AbstractClient client) {
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

    @Nullable
    public Boolean call(@Nullable Object prevResult) {
        AbstractClient client;
        if (prevResult instanceof AbstractClient)
            client = (AbstractClient) prevResult;
        else
            throw new IllegalStateException("Previous result should be a client, but was " + prevResult);

        Boolean result = call(client);
        client.close();
        return result;
    }

    @NonNull
    private Object runAuthenticated(@NonNull Aria2Helper helper) {
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
