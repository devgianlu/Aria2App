package com.gianlu.aria2app.NetIO.Updater;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.NetIO.OnRefresh;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;

public abstract class UpdaterActivity extends ActivityWithDialog {
    private final UpdaterFramework framework;

    public UpdaterActivity() {
        framework = new UpdaterFramework();
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

        framework.startUpdaters();
    }

    public <P> PayloadProvider<P> attachReceiver(@NonNull Receiver<P> receiver) {
        return framework.attachReceiver(receiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        framework.startUpdaters();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        framework.stopUpdaters();
    }

    @Override
    protected void onPause() {
        super.onPause();
        framework.stopUpdaters();
    }

    @Override
    protected void onStop() {
        super.onStop();
        framework.stopUpdaters();
    }

    protected void refresh(@NonNull Class<?> type, OnRefresh listener) {
        framework.refresh(type, listener);
    }
}
