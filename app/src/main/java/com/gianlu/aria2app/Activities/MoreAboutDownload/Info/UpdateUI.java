package com.gianlu.aria2app.Activities.MoreAboutDownload.Info;

import android.content.Context;

import com.gianlu.aria2app.NetIO.BaseUpdater;
import com.gianlu.aria2app.NetIO.ErrorHandler;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.JTA2.JTA2InitializingException;

public class UpdateUI extends BaseUpdater implements JTA2.IDownload {
    private final String gid;
    private final IUI listener;

    public UpdateUI(Context context, String gid, IUI listener) throws JTA2InitializingException {
        super(context);
        this.gid = gid;
        this.listener = listener;
    }

    @Override
    public void loop() {
        jta2.tellStatus(gid, this);
    }

    @Override
    public void onDownload(final Download download) {
        if (listener == null) return;

        handler.post(new Runnable() {
            @Override
            public void run() {
                listener.onUpdateUI(download);
            }
        });
    }

    @Override
    public void onException(Exception ex) {
        ErrorHandler.get().notifyException(ex, false);
    }

    public interface IUI {
        void onUpdateUI(Download download);
    }
}
