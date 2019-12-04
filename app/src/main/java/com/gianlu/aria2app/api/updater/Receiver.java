package com.gianlu.aria2app.api.updater;

import com.gianlu.aria2app.api.aria2.Aria2Helper;

import androidx.annotation.NonNull;

public interface Receiver<P> {
    void onUpdateUi(@NonNull P payload);

    void onLoad(@NonNull P payload);

    /**
     * @see PayloadUpdater.OnPayload#onException(Exception)
     */
    boolean onCouldntLoad(@NonNull Exception ex);

    /**
     * @see PayloadUpdater.OnPayload#onException(Exception)
     */
    boolean onUpdateException(@NonNull Exception ex);

    @NonNull
    Wants<P> wants();

    @NonNull
    PayloadProvider<P> requireProvider() throws Aria2Helper.InitializingException;
}

