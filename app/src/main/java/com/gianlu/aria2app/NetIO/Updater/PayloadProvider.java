package com.gianlu.aria2app.NetIO.Updater;

import android.support.annotation.NonNull;

public interface PayloadProvider<P> {
    void requireLoadCall(@NonNull UpdaterFragment<P> fragment);

    void onUpdateUi(@NonNull P payload);

    void onLoad(@NonNull P payload);

    @NonNull
    Class<P> provides();
}
