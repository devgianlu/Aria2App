package com.gianlu.aria2app.NetIO;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

public abstract class UpdaterFragment extends Fragment {
    private BaseUpdater updater;

    @Nullable
    protected abstract BaseUpdater createUpdater(@NonNull Bundle args);

    @Override
    public void onResume() {
        super.onResume();
        restartUpdater();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopUpdater();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopUpdater();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopUpdater();
    }

    protected void stopUpdater() {
        if (updater != null) updater.stopThread(null);
        updater = null;
    }

    protected void startUpdater() {
        Bundle args = getArguments();
        if (args != null) {
            updater = createUpdater(args);
            if (updater != null) updater.start();
        }
    }

    protected final void refresh(final OnRefresh listener) {
        updater.stopThread(new BaseUpdater.IThread() {
            @Override
            public void onStopped() {
                if (listener != null) listener.refreshed();
                restartUpdater();
            }
        });
    }

    protected void restartUpdater() {
        stopUpdater();
        startUpdater();
    }
}
