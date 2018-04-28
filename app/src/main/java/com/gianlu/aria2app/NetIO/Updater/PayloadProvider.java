package com.gianlu.aria2app.NetIO.Updater;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.OnRefresh;
import com.gianlu.commonutils.CommonUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public abstract class PayloadProvider<P> implements PayloadUpdater.OnPayload<P> {
    protected final PayloadUpdater<P> updater;
    private final Wants<P> provides;
    private final Set<Receiver<P>> attachedReceivers = new HashSet<>();
    private final Set<ReceiverOwner> owners = new HashSet<>();
    private P lastPayload;
    private PayloadUpdater.OnPayload<P> requireListener = null;

    public PayloadProvider(@NonNull Context context, @NonNull Wants<P> provides) throws Aria2Helper.InitializingException {
        this.updater = requireUpdater(context);
        this.provides = provides;
    }

    @NonNull
    public final Wants<P> provides() {
        return provides;
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

    public void attachReceiver(@NonNull ReceiverOwner owner, @NonNull Receiver<P> receiver) {
        attachedReceivers.add(receiver);
        owners.add(owner);

        if (CommonUtils.isDebug())
            System.out.println("Attached receiver to " + provides() + " by " + owner.toString());
    }

    public boolean isOnlyOwner(ReceiverOwner owner) {
        return owners.size() == 1 && owners.contains(owner);
    }

    public boolean owns(ReceiverOwner owner) {
        return owners.contains(owner);
    }

    public void removeOwner(ReceiverOwner owner) {
        owners.remove(owner);
    }

    public void refresh(final ExecutorService executorService, @Nullable final OnRefresh listener) {
        updater.safeStop(new PayloadUpdater.OnStop() {
            @Override
            public void onStopped() {
                stop(null);
                start(executorService);
                if (listener != null) listener.refreshed();
            }
        });
    }

    public void stop(@Nullable PayloadUpdater.OnStop listener) {
        if (updater.safeStop(listener) && CommonUtils.isDebug())
            System.out.println("Provider stopped " + provides());
    }

    public void start(@NonNull ExecutorService executorService) {
        if (!updater.isRunning()) {
            executorService.submit(updater);
            if (CommonUtils.isDebug()) System.out.println("Provider started " + provides());
        }
    }

    public void requirePayload(PayloadUpdater.OnPayload<P> listener) {
        requireListener = listener;
    }
}
