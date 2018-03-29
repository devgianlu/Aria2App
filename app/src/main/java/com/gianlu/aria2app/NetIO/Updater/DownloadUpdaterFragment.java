package com.gianlu.aria2app.NetIO.Updater;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.NetIO.Aria2.Download;
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithHelper;

public abstract class DownloadUpdaterFragment extends UpdaterFragment {

    @Nullable
    protected abstract Download getDownload(@NonNull Bundle args);

    @Nullable
    protected final Download getDownload() {
        DownloadWithHelper helper = getDownloadWithHelper();
        return helper != null ? helper.get() : null;
    }

    @Nullable
    protected final DownloadWithHelper getDownloadWithHelper() {
        BaseDownloadUpdater updater = ((BaseDownloadUpdater) getUpdater());
        return updater != null ? updater.download : null;
    }

    @Nullable
    @Override
    public final BaseUpdater createUpdater(@NonNull Bundle args) {
        Download download = getDownload(args);
        if (download != null) return createUpdater(download);
        else return null;
    }

    @Nullable
    protected abstract BaseUpdater createUpdater(@NonNull Download download);
}
