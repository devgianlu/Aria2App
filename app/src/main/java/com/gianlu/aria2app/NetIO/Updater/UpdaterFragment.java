package com.gianlu.aria2app.NetIO.Updater;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.OnRefresh;

public abstract class UpdaterFragment<P> extends Fragment implements Receiver<P> {
    private boolean calledLoad = false;
    private PayloadProvider<P> provider;

    @Override
    public final void onLoad(@NonNull P payload) {
        if (!calledLoad) onLoadUi(payload);
        calledLoad = true;
    }

    @NonNull
    @Override
    public final PayloadProvider<P> requireProvider() throws Aria2Helper.InitializingException {
        Bundle args = getArguments();
        if (args == null)
            throw new Aria2Helper.InitializingException(new NullPointerException("Missing arguments!"));
        return requireProvider(args);
    }

    @NonNull
    protected abstract PayloadProvider<P> requireProvider(@NonNull Bundle args) throws Aria2Helper.InitializingException;

    protected abstract void onLoadUi(@NonNull P payload);

    protected final void refresh(@NonNull Class<P> type, OnRefresh listener) {
        if (getActivity() instanceof UpdaterActivity)
            ((UpdaterActivity) getActivity()).refresh(type, listener);
    }

    @Override
    @CallSuper
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (provider != null) provider.requireLoadCall(this);
    }

    @Override
    @CallSuper
    public void onAttach(Context context) {
        if (context instanceof UpdaterActivity) {
            UpdaterActivity activity = (UpdaterActivity) context;
            this.provider = activity.attachReceiver(this);
        }

        super.onAttach(context);
    }
}
