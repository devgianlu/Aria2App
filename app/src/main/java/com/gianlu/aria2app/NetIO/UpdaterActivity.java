package com.gianlu.aria2app.NetIO;

import android.support.annotation.Nullable;

import com.gianlu.commonutils.Dialogs.ActivityWithDialog;

public abstract class UpdaterActivity extends ActivityWithDialog {
    private BaseUpdater updater;

    @Nullable
    protected abstract BaseUpdater createUpdater();

    @Override
    protected void onResume() {
        super.onResume();
        restart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stop();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stop();
    }

    private void stop() {
        if (updater != null) updater.stopThread(null);
        updater = null;
    }

    private void start() {
        updater = createUpdater();
        if (updater != null) updater.start();
    }

    protected final void refresh(final OnRefresh listener) {
        if (updater != null) {
            updater.stopThread(new BaseUpdater.IThread() {
                @Override
                public void onStopped() {
                    listener.refreshed();
                    restart();
                }
            });
        }
    }

    private void restart() {
        stop();
        start();
    }
}
