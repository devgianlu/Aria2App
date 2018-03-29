package com.gianlu.aria2app.NetIO.Updater;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.NetIO.Aria2.Download;
import com.gianlu.aria2app.NetIO.Aria2.DownloadStatic;
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithHelper;

public abstract class DownloadUpdaterFragment extends UpdaterFragment {

    @Nullable
    protected abstract Download getDownload(@NonNull Bundle args);

    @NonNull
    protected final DownloadStatic getDownload() {
        return getDownloadWithHelper().get();
    }

    @NonNull
    protected final DownloadWithHelper getDownloadWithHelper() {
        return ((BaseDownloadUpdater) updater).download;
    }

    @Nullable
    @Override
    protected final BaseUpdater createUpdater(@NonNull Bundle args) {
        Download download = getDownload(args);
        if (download != null) return createUpdater(download);
        else return null;
    }

    @Nullable
    protected abstract BaseUpdater createUpdater(@NonNull Download download);
}
