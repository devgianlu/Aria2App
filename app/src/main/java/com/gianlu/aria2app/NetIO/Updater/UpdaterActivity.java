package com.gianlu.aria2app.NetIO.Updater;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.NetIO.OnRefresh;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;

public abstract class UpdaterActivity extends ActivityWithDialog implements UpdaterFramework.Interface {
    private final UpdaterFramework framework;

    public UpdaterActivity() {
        framework = new UpdaterFramework(this);
    }

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

    @Nullable
    @Override
    public Bundle getArguments() {
        return getIntent().getExtras();
    }

    protected void refresh(OnRefresh listener) {
        framework.refresh(listener);
    }
}
