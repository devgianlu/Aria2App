package com.gianlu.aria2app.NetIO.Updater;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.gianlu.aria2app.NetIO.OnRefresh;

public abstract class UpdaterFragment extends Fragment implements UpdaterFramework.Interface {
    private final UpdaterFramework framework;

    public UpdaterFragment() {
        framework = new UpdaterFramework(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        framework.restartUpdater();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        framework.stopUpdater();
    }

    @Override
    public void onPause() {
        super.onPause();
        framework.stopUpdater();
    }

    @Override
    public void onStop() {
        super.onStop();
        framework.stopUpdater();
    }

    protected void stopUpdater() {
        framework.stopUpdater();
    }

    protected void refresh(OnRefresh listener) {
        framework.refresh(listener);
    }

    @Nullable
    protected BaseUpdater getUpdater() {
        return framework.getUpdater();
    }
}
