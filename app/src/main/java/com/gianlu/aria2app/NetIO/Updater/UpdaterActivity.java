package com.gianlu.aria2app.NetIO.Updater;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.OnRefresh;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;

public abstract class UpdaterActivity<P> extends ActivityWithDialog implements UpdaterFramework.Interface<P> {
    private final UpdaterFramework<P> framework;

    public UpdaterActivity() {
        framework = new UpdaterFramework<>(this);
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

        framework.startUpdater();
        framework.requirePayload(new AbstractClient.OnResult<P>() {
            @Override
            public void onResult(P result) {
                onLoad(result);
            }

            @Override
            public void onException(Exception ex, boolean shouldForce) {
                onCouldntLoad(ex);
            }
        });
    }

    protected abstract void onLoad(@NonNull P payload);

    @Override
    protected void onResume() {
        super.onResume();
        framework.restartUpdater();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        framework.stopUpdater();
    }

    @Override
    protected void onPause() {
        super.onPause();
        framework.stopUpdater();
    }

    @Override
    protected void onStop() {
        super.onStop();
        framework.stopUpdater();
    }

    @NonNull
    @Override
    public Bundle getArguments() {
        Bundle args = getIntent().getExtras();
        return args == null ? new Bundle() : args;
    }

    protected void refresh(OnRefresh listener) {
        framework.refresh(listener);
    }

    @Nullable
    protected BaseUpdater getUpdater() {
        return framework.getUpdater();
    }
}
