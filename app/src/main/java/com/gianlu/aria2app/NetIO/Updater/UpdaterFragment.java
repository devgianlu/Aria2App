package com.gianlu.aria2app.NetIO.Updater;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.gianlu.aria2app.NetIO.OnRefresh;

public abstract class UpdaterFragment<P> extends Fragment {
    private boolean calledLoad = false;
    private UpdaterActivity<P> activity;

    public abstract void onUpdateUi(@NonNull P payload);

    protected abstract void onLoad(@NonNull P payload);

    public final void callOnLoad(@NonNull P payload) {
        if (!calledLoad) onLoad(payload);
        calledLoad = true;
    }

    protected final void refresh(OnRefresh listener) {
        if (getActivity() instanceof UpdaterActivity)
            ((UpdaterActivity) getActivity()).refresh(listener);
    }

    @Override
    @CallSuper
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity.requireLoadCall(this);
    }

    @Override
    @CallSuper
    @SuppressWarnings("unchecked")
    public void onAttach(Context context) {
        if (context instanceof UpdaterActivity) {
            activity = ((UpdaterActivity<P>) context);
            activity.attachFragment(this);
        }

        super.onAttach(context);
    }
}
