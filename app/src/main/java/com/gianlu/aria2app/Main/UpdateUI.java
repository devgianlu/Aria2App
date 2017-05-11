package com.gianlu.aria2app.Main;

import android.content.Context;

import com.gianlu.aria2app.NetIO.BaseUpdater;
import com.gianlu.aria2app.NetIO.ErrorHandler;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.JTA2.JTA2InitializingException;

import java.util.List;

public class UpdateUI extends BaseUpdater implements JTA2.IDownloadList {
    private final IUI listener;

    public UpdateUI(Context context, IUI listener) throws JTA2InitializingException {
        super(context);
        this.listener = listener;
    }

    @Override
    public void loop() {
        jta2.tellActive(this);
        jta2.tellWaiting(this);
        jta2.tellStopped(this);
    }

    @Override
    public void onDownloads(final List<Download> downloads) {
        if (listener == null) return;

        handler.post(new Runnable() {
            @Override
            public void run() {
                for (Download download : downloads) listener.onUpdateAdapter(download);
            }
        });
    }

    @Override
    public void onException(boolean queuing, Exception ex) {
        ErrorHandler.get().notifyException(ex, false);
    }

    public interface IUI {
        void onUpdateAdapter(Download download);
    }
}
