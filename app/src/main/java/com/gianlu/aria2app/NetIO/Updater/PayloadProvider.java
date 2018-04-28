package com.gianlu.aria2app.NetIO.Updater;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.OnRefresh;

import java.util.ArrayList;
import java.util.List;

public abstract class PayloadProvider<P> implements PayloadUpdater.OnPayload<P> {
    protected final PayloadUpdater<P> updater;
    private final List<Receiver<P>> attachedReceivers = new ArrayList<>();
    private P lastPayload;
    private PayloadUpdater.OnPayload<P> requireListener = null;

    public PayloadProvider(@NonNull Context context) throws Aria2Helper.InitializingException {
        updater = requireUpdater(context);
    }

    @Override
    public final void onPayload(@NonNull P payload) {
        lastPayload = payload;

        if (requireListener != null) {
            requireListener.onPayload(payload);
            requireListener = null;
        }

        for (Receiver<P> receiver : attachedReceivers) receiver.onUpdateUi(payload);
    }

    @NonNull
    protected abstract PayloadUpdater<P> requireUpdater(@NonNull Context context) throws Aria2Helper.InitializingException;

    public void requireLoadCall(@NonNull Receiver<P> receiver) {
        if (lastPayload != null) receiver.onLoad(lastPayload);
    }

    public void attachReceiver(@NonNull Receiver<P> receiver) {
        attachedReceivers.add(receiver);
    }

    public void refresh(@Nullable final OnRefresh listener) {
        updater.safeStop(new PayloadUpdater.OnStop() {
            @Override
            public void onStopped() {
                stop(null);
                start();
                if (listener != null) listener.refreshed();
            }
        });
    }

    public void stop(@Nullable PayloadUpdater.OnStop listener) {
        updater.safeStop(listener);
    }

    public void start() {
        new Thread(updater).start();
    }

    public void requirePayload(PayloadUpdater.OnPayload<P> listener) {
        requireListener = listener;
    }
}
