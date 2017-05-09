package com.gianlu.aria2app.Main;

import android.content.Context;
import android.os.Handler;

import com.gianlu.aria2app.NetIO.ErrorHandler;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.JTA2.JTA2InitializingException;

import java.util.List;

public class UpdateUI extends Thread implements JTA2.IDownloadList {
    private final IUI listener;
    private final JTA2 jta2;
    private final Handler handler;
    private boolean _shouldStop = false;

    public UpdateUI(Context context, IUI listener) throws JTA2InitializingException {
        this.jta2 = JTA2.instantiate(context);
        this.listener = listener;
        this.handler = new Handler(context.getMainLooper());
    }

    @Override
    public void run() {
        while (!_shouldStop) {
            jta2.tellActive(this);
            jta2.tellWaiting(this);
            jta2.tellStopped(this);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                _shouldStop = true;
                onException(false, ex);
            }
        }
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
