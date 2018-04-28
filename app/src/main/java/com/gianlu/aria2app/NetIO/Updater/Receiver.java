package com.gianlu.aria2app.NetIO.Updater;

import android.support.annotation.NonNull;

import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;

public interface Receiver<P> {
    void onUpdateUi(@NonNull P payload);

    void onLoad(@NonNull P payload);

    void onCouldntLoad(@NonNull Exception ex);

    @NonNull
    Class<P> provides();

    @NonNull
    PayloadProvider<P> requireProvider() throws Aria2Helper.InitializingException;
}

