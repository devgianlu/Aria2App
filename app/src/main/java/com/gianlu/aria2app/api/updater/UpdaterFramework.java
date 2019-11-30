package com.gianlu.aria2app.api.updater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.aria2app.ThisApplication;
import com.gianlu.aria2app.api.ErrorHandler;
import com.gianlu.aria2app.api.OnRefresh;
import com.gianlu.aria2app.api.aria2.Aria2Helper;
import com.gianlu.commonutils.CommonUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class UpdaterFramework {
    private static UpdaterFramework instance;
    private final Map<Wants<?>, PayloadProvider<?>> providers;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private UpdaterFramework() {
        this.providers = new HashMap<>();
    }

    @NonNull
    public static UpdaterFramework get() {
        if (instance == null) instance = new UpdaterFramework();
        return instance;
    }

    private static void debug(String msg) {
        if (CommonUtils.isDebug() && ThisApplication.DEBUG_UPDATER)
            System.out.println(UpdaterFramework.class.getSimpleName() + ": " + msg);
    }

    @Nullable
    public <P> PayloadProvider<P> attachReceiver(@NonNull ReceiverOwner owner, @NonNull final Receiver<P> receiver) {
        Wants<P> wants = receiver.wants();
        PayloadProvider<P> provider = getProvider(wants);
        if (provider == null) {
            try {
                provider = receiver.requireProvider();
                if (!provider.provides().equals(wants))
                    throw new IllegalStateException("What?! Required " + wants + " but got " + provider.provides());

                providers.put(wants, provider);
                provider.start(executorService);
            } catch (Aria2Helper.InitializingException ex) {
                if (!receiver.onCouldntLoad(ex)) ErrorHandler.get().notifyException(ex, false);
                return null;
            }

            debug("Created " + provider);
        }

        provider.attachReceiver(owner, receiver);
        provider.requirePayload(new PayloadUpdater.OnPayload<P>() {
            @Override
            public void onPayload(@NonNull P payload) {
                receiver.onLoad(payload);
            }

            @Override
            public boolean onException(@NonNull Exception ex) {
                return receiver.onCouldntLoad(ex);
            }
        });
        return provider;
    }

    protected void stopUpdaters(@NonNull ReceiverOwner owner) {
        for (PayloadProvider<?> provider : providers.values())
            if (provider.isOnlyOwner(owner)) provider.stop(null);
    }

    protected void startUpdaters(@NonNull ReceiverOwner owner) {
        for (PayloadProvider<?> provider : providers.values())
            if (provider.owns(owner)) provider.start(executorService);
    }

    public void removeUpdaters(@NonNull ReceiverOwner owner) {
        Iterator<PayloadProvider<?>> iterator = providers.values().iterator();
        while (iterator.hasNext()) {
            PayloadProvider<?> provider = iterator.next();
            if (provider.isOnlyOwner(owner)) {
                provider.stop(null);
                iterator.remove();
            } else if (provider.owns(owner)) {
                provider.removeOwner(owner);
            }
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <P> PayloadProvider<P> getProvider(@NonNull Wants<P> wants) {
        return (PayloadProvider<P>) providers.get(wants);
    }

    protected <P> void refresh(@NonNull Wants<P> wants, OnRefresh listener) {
        PayloadProvider<P> provider = getProvider(wants);
        if (provider != null) provider.refresh(executorService, listener);
        else listener.refreshed();
    }
}
