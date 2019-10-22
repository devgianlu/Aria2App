package com.gianlu.aria2app.NetIO.Updater;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.OnRefresh;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;

public abstract class UpdaterFragment<P> extends FragmentWithDialog implements Receiver<P>, ReceiverOwner {
    private boolean calledLoad = false;
    private PayloadProvider<P> provider;
    private Wants<P> wants;
    private UpdaterFramework framework;

    @Override
    public final void onLoad(@NonNull P payload) {
        if (!calledLoad) onLoadUi(payload);
        calledLoad = true;
    }

    @Override
    public boolean onUpdateException(@NonNull Exception ex) {
        return false;
    }

    @NonNull
    @Override
    public final PayloadProvider<P> requireProvider() throws Aria2Helper.InitializingException {
        Bundle args = getArguments();
        if (args == null) throw new IllegalStateException("Missing arguments!");
        if (getContext() == null)
            throw new Aria2Helper.InitializingException(new IllegalStateException("Context is null!"));
        return requireProvider(getContext(), args);
    }

    @NonNull
    @Override
    public final Wants<P> wants() {
        if (wants == null) {
            Bundle args = getArguments();
            if (args == null) throw new IllegalStateException("Missing arguments!");
            wants = wants(args);
        }

        return wants;
    }

    @NonNull
    protected final Aria2Helper getHelper() {
        return provider.getHelper();
    }

    @NonNull
    protected abstract Wants<P> wants(@NonNull Bundle args);

    @NonNull
    protected abstract PayloadProvider<P> requireProvider(@NonNull Context context, @NonNull Bundle args) throws Aria2Helper.InitializingException;

    protected abstract void onLoadUi(@NonNull P payload);

    protected final void refresh(OnRefresh listener) {
        if (getActivity() instanceof UpdaterActivity)
            ((UpdaterActivity) getActivity()).refresh(wants(), listener);
    }

    @Override
    @CallSuper
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (provider != null) provider.requireLoadCall(this);
    }

    @Override
    @CallSuper
    public void onAttach(@NonNull Context context) {
        if (context instanceof UpdaterActivity) {
            UpdaterActivity activity = (UpdaterActivity) context;
            this.framework = activity.getFramework();
            this.provider = activity.attachReceiver(this, this);
        }

        super.onAttach(context);
    }

    private void removeUpdaters() {
        if (framework != null) framework.removeUpdaters(this);
    }

    private void startUpdaters() {
        if (framework != null) framework.startUpdaters(this);
    }

    private void stopUpdaters() {
        if (framework != null) framework.stopUpdaters(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        startUpdaters();
    }

    @Override
    @CallSuper
    public void onDestroy() {
        super.onDestroy();
        removeUpdaters();
    }

    @Override
    @CallSuper
    public void onPause() {
        super.onPause();
        stopUpdaters();
    }

    @Override
    @CallSuper
    public void onStop() {
        super.onStop();
        stopUpdaters();
    }
}
