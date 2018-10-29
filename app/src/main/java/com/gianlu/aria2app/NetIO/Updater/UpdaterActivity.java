package com.gianlu.aria2app.NetIO.Updater;

import android.os.Bundle;

import com.gianlu.aria2app.NetIO.OnRefresh;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class UpdaterActivity extends ActivityWithDialog implements ReceiverOwner {
    private final UpdaterFramework framework;

    public UpdaterActivity() {
        framework = UpdaterFramework.get();
    }

    protected void onPreCreate(@Nullable Bundle savedInstanceState) {
    }

    protected void onPostCreate() {
    }

    @Override
    protected final void onCreate(@Nullable Bundle savedInstanceState) {
        onPreCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        onPostCreate();

        framework.startUpdaters(this);
    }

    @Nullable
    public final <P> PayloadProvider<P> attachReceiver(@NonNull ReceiverOwner owner, @NonNull Receiver<P> receiver) {
        return framework.attachReceiver(owner, receiver);
    }

    @Override
    @CallSuper
    protected void onResume() {
        super.onResume();
        framework.startUpdaters(this);
    }

    @Override
    @CallSuper
    protected void onDestroy() {
        super.onDestroy();
        framework.removeUpdaters(this);
    }

    @Override
    @CallSuper
    protected void onPause() {
        super.onPause();
        framework.stopUpdaters(this);
    }

    @Override
    @CallSuper
    protected void onStop() {
        super.onStop();
        framework.stopUpdaters(this);
    }

    protected final void refresh(@NonNull Wants<?> wants, OnRefresh listener) {
        framework.refresh(wants, listener);
    }

    @NonNull
    public UpdaterFramework getFramework() {
        return framework;
    }
}
