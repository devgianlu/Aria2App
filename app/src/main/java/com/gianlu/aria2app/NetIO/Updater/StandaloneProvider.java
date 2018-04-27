package com.gianlu.aria2app.NetIO.Updater;

import android.support.annotation.NonNull;

public class StandaloneProvider<P> implements PayloadProvider<P> {
    private final Class<P> provides;

    public StandaloneProvider(Class<P> provides) {
        this.provides = provides;
    }

    @Override
    public void requireLoadCall(@NonNull UpdaterFragment<P> fragment) {
        if (lastPayload != null) fragment.callOnLoad(lastPayload);
    }

    @Override
    public void onUpdateUi(@NonNull P payload) {

    }

    @Override
    public void onLoad(@NonNull P payload) {

    }

    @NonNull
    @Override
    public Class<P> provides() {
        return provides;
    }
}
