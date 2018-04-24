package com.gianlu.aria2app.NetIO.Updater;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import com.gianlu.aria2app.NetIO.OnRefresh;

public abstract class UpdaterFragment<P> extends Fragment {

    public abstract void onUpdateUi(@NonNull P payload);

    public abstract void onLoad(@NonNull P payload);

    protected final void refresh(OnRefresh listener) {
        if (getActivity() instanceof UpdaterActivity)
            ((UpdaterActivity) getActivity()).refresh(listener);
    }

    @Override
    @CallSuper
    @SuppressWarnings("unchecked")
    public void onAttach(Context context) {
        if (context instanceof UpdaterActivity)
            ((UpdaterActivity<P>) context).attachFragment(this);

        super.onAttach(context);
    }
}
