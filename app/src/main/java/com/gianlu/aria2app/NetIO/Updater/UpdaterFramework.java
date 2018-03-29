package com.gianlu.aria2app.NetIO.Updater;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.NetIO.OnRefresh;

final class UpdaterFramework {
    private final Interface listener;
    protected BaseUpdater updater;

    UpdaterFramework(Interface listener) {
        this.listener = listener;
    }

    protected void stopUpdater() {
        if (updater != null) updater.safeStop(null);
        updater = null;
    }

    @Nullable
    public BaseUpdater getUpdater() {
        return updater;
    }

    private void startUpdater() {
        Bundle args = listener.getArguments();
        if (args != null) {
            updater = listener.createUpdater(args);
            if (updater != null) updater.start();
        }
    }

    protected void refresh(final OnRefresh listener) {
        updater.safeStop(new BaseUpdater.OnStop() {
            @Override
            public void onStopped() {
                if (listener != null) listener.refreshed();
                restartUpdater();
            }
        });
    }

    protected void restartUpdater() {
        stopUpdater();
        startUpdater();
    }

    protected interface Interface {

        BaseUpdater createUpdater(@NonNull Bundle args);

        @Nullable
        Bundle getArguments();
    }
}
