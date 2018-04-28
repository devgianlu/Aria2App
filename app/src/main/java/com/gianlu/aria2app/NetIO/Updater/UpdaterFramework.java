package com.gianlu.aria2app.NetIO.Updater;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.OnRefresh;

import java.util.HashMap;
import java.util.Map;

public final class UpdaterFramework {
    private final Map<Class<?>, PayloadProvider<?>> providers;

    UpdaterFramework() {
        this.providers = new HashMap<>();
    }

    @Nullable
    public <P> PayloadProvider<P> attachReceiver(@NonNull final Receiver<P> receiver) {
        Class<P> provides = receiver.provides();
        PayloadProvider<P> provider = getProvider(provides);
        if (provider == null) {
            try {
                provider = receiver.requireProvider();
                providers.put(provides, provider);
                provider.start();
            } catch (Aria2Helper.InitializingException ex) {
                receiver.onCouldntLoad(ex);
                return null;
            }
        }

        provider.attachReceiver(receiver);
        provider.requirePayload(new PayloadUpdater.OnPayload<P>() {
            @Override
            public void onPayload(@NonNull P payload) {
                receiver.onLoad(payload);
            }
        });
        return provider;
    }

    protected void stopUpdaters() {
        for (PayloadProvider<?> provider : providers.values()) provider.stop(null);
    }

    protected void startUpdaters() {
        for (PayloadProvider<?> provider : providers.values()) provider.start();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <P> PayloadProvider<P> getProvider(Class<P> type) {
        return (PayloadProvider<P>) providers.get(type);
    }

    protected <P> void refresh(Class<P> type, final OnRefresh listener) {
        PayloadProvider<P> provider = getProvider(type);
        if (provider != null) provider.refresh(listener);
        else listener.refreshed();
    }
}
