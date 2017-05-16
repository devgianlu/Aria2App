package com.gianlu.aria2app.Activities.MoreAboutDownload.Servers;

import android.content.Context;
import android.util.SparseArray;

import com.gianlu.aria2app.NetIO.BaseUpdater;
import com.gianlu.aria2app.NetIO.ErrorHandler;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.JTA2.JTA2InitializingException;
import com.gianlu.aria2app.NetIO.JTA2.Server;

import java.util.List;

public class UpdateUI extends BaseUpdater implements JTA2.IServers {
    private final String gid;
    private final IUI listener;

    public UpdateUI(Context context, String gid, IUI listener) throws JTA2InitializingException {
        super(context);
        this.gid = gid;
        this.listener = listener;
    }

    @Override
    public void loop() {
        jta2.getServers(gid, this);
    }

    @Override
    public void onServers(final SparseArray<List<Server>> servers) {
        if (listener == null) return;
        handler.post(new Runnable() {
            @Override
            public void run() {
                listener.onUpdateAdapter(servers);
            }
        });
    }

    @Override
    public void onException(Exception ex) {
        ErrorHandler.get().notifyException(ex, false);
    }

    @Override
    public void onDownloadNotActive(final Exception ex) {
        if (listener == null) return;
        handler.post(new Runnable() {
            @Override
            public void run() {
                listener.onNoServers(ex.getMessage());
            }
        });
    }

    public interface IUI {
        void onUpdateAdapter(SparseArray<List<Server>> servers);

        void onNoServers(String reason);
    }
}

