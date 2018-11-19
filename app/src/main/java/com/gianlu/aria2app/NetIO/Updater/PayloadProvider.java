package com.gianlu.aria2app.NetIO.Updater;

import android.content.Context;

import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.OnRefresh;
import com.gianlu.aria2app.ThisApplication;
import com.gianlu.commonutils.CommonUtils;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class PayloadProvider<P> implements PayloadUpdater.OnPayload<P> {
    protected final PayloadUpdater<P> updater;
    private final Wants<P> provides;
    private final Set<Receiver<P>> attachedReceivers = new HashSet<>();
    private final Set<ReceiverOwner> owners = new HashSet<>();
    private final Set<PayloadUpdater.OnPayload<P>> requireListeners;
    protected P lastPayload;

    public PayloadProvider(@NonNull Context context, @NonNull Wants<P> provides) throws Aria2Helper.InitializingException {
        this.updater = requireUpdater(context);
        this.provides = provides;
        this.requireListeners = new HashSet<>();
    }

    private static void debug(String msg) {
        if (CommonUtils.isDebug() && ThisApplication.DEBUG_UPDATER)
            System.out.println(PayloadProvider.class.getSimpleName() + ": " + msg);
    }

    @NonNull
    public final Wants<P> provides() {
        return provides;
    }

    @Override
    public final boolean onException(@NonNull Exception ex) {
        debug("Exception in " + this);

        synchronized (requireListeners) {
            if (!requireListeners.isEmpty()) {
                boolean handled = false;
                for (PayloadUpdater.OnPayload<P> listener : requireListeners)
                    if (listener.onException(ex)) handled = true;

                requireListeners.clear();
                return handled;
            } else {
                boolean handled = false;
                for (Receiver<P> receiver : attachedReceivers)
                    if (receiver.onUpdateException(ex)) handled = true;

                return handled;
            }
        }
    }

    @Override
    public final void onPayload(@NonNull P payload) {
        lastPayload = payload;

        debug("Payload deployed by " + this);

        synchronized (requireListeners) {
            if (!requireListeners.isEmpty()) {
                for (PayloadUpdater.OnPayload<P> listener : requireListeners)
                    listener.onPayload(payload);

                requireListeners.clear();
            }
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

        debug("Attached " + this + " to " + receiver + " by " + owner);
    }

    public boolean isOnlyOwner(ReceiverOwner owner) {
        return owners.size() == 1 && owners.contains(owner);
    }

    public boolean owns(ReceiverOwner owner) {
        return owners.contains(owner);
    }

    public void removeOwner(ReceiverOwner owner) {
        owners.remove(owner);
        debug("Removed " + owner + " from " + this);
    }

    public void refresh(final ExecutorService executorService, @Nullable final OnRefresh listener) {
        updater.safeStop(() -> {
            stop(null);
            start(executorService);
            if (listener != null) listener.refreshed();
        });
    }

    public void stop(@Nullable PayloadUpdater.OnStop listener) {
        if (updater.safeStop(listener))
            debug("Stopped " + this);
    }

    public void start(@NonNull ExecutorService executorService) {
        if (!updater.isRunning()) {
            executorService.submit(updater);
            debug("Started " + this);
        }
    }

    public void requirePayload(@NonNull PayloadUpdater.OnPayload<P> listener) {
        synchronized (requireListeners) {
            requireListeners.add(listener);
            debug("Required payload from " + this);
        }
    }

    @NonNull
    public Aria2Helper getHelper() {
        return updater.getHelper();
    }

    @NotNull
    @Override
    public String toString() {
        return "PayloadProvider{" +
                "provides=" + provides +
                ", attachedReceivers=" + attachedReceivers +
                ", owners=" + owners +
                ", requireListeners=" + requireListeners +
                '}';
    }
}
