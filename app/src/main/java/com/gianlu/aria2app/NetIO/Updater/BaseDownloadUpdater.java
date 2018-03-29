package com.gianlu.aria2app.NetIO.Updater;

import android.content.Context;

import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.Download;
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithHelper;

public abstract class BaseDownloadUpdater<P> extends BaseUpdater<P> {
    protected final DownloadWithHelper download;

    public BaseDownloadUpdater(Context context, Download download, UpdaterListener<P> listener) throws Aria2Helper.InitializingException {
        super(context, listener);
        this.download = download.wrap(helper.getClient());
    }
}
