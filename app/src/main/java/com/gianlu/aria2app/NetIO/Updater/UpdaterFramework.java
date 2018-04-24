package com.gianlu.aria2app.NetIO.Updater;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.OnRefresh;

public final class UpdaterFramework<P> {
    private final Interface<P> listener;
    protected BaseUpdater<P> updater;

    UpdaterFramework(@NonNull Interface<P> listener) {
        this.listener = listener;
    }

    protected void stopUpdater() {
        if (updater != null) updater.safeStop(null);
        updater = null;
    }

    @Nullable
    public BaseUpdater<P> getUpdater() {
        return updater;
    }

    protected void startUpdater() {
        Bundle args = listener.getArguments();
        if (args != null && updater == null) {
            try {
                updater = listener.createUpdater(args);
                updater.start();
            } catch (Exception ex) {
                listener.onCouldntLoad(ex);
            }
        }
    }

    protected void refresh(final OnRefresh listener) {
        updater.safeStop(new BaseUpdater.OnStop() {
            @Override
            public void onStopped() {
                if (listener != null) listener.refreshed();
                stopUpdater();
                startUpdater();
            }
        });
    }

    public void requirePayload(AbstractClient.OnResult<P> listener) {
        if (updater != null) updater.requirePayload(listener);
        else listener.onException(new IllegalStateException("Updater not initialized yet!"), false);
    }

    public interface Interface<P> {

        @NonNull
        BaseUpdater<P> createUpdater(@NonNull Bundle args) throws Exception;

        @Nullable
        Bundle getArguments();

        void onPayload(@NonNull P payload);

        void onCouldntLoad(@NonNull Exception ex);
    }
}
