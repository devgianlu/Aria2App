package com.gianlu.aria2app.Activities.MoreAboutDownload.Peers;

import android.content.Context;

import com.gianlu.aria2app.NetIO.BaseUpdater;
import com.gianlu.aria2app.NetIO.ErrorHandler;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.JTA2.JTA2InitializingException;
import com.gianlu.aria2app.NetIO.JTA2.Peer;

import java.util.List;

public class UpdateUI extends BaseUpdater implements JTA2.IPeers {
    private final String gid;
    private final IUI listener;

    public UpdateUI(Context context, String gid, IUI listener) throws JTA2InitializingException {
        super(context);
        this.gid = gid;
        this.listener = listener;
    }

    @Override
    public void loop() {
        jta2.getPeers(gid, this);
    }

    @Override
    public void onPeers(final List<Peer> peers) {
        if (listener == null) return;
        handler.post(new Runnable() {
            @Override
            public void run() {
                listener.onUpdateAdapter(peers);
            }
        });
    }

    @Override
    public void onException(Exception ex) {
        ErrorHandler.get().notifyException(ex, false);
    }

    @Override
    public void onNoPeerData(final Exception ex) {
        if (listener == null) return;
        handler.post(new Runnable() {
            @Override
            public void run() {
                listener.onNoPeers(ex.getMessage());
            }
        });
    }

    public interface IUI {
        void onUpdateAdapter(List<Peer> peers);

        void onNoPeers(String reason);
    }
}
