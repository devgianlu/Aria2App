package com.gianlu.aria2app.Main;

import android.content.Context;

import com.gianlu.aria2app.NetIO.BaseUpdater;
import com.gianlu.aria2app.NetIO.ErrorHandler;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.JTA2.JTA2InitializingException;
import com.gianlu.aria2app.Prefs;

import java.util.ArrayList;
import java.util.List;

public class UpdateUI extends BaseUpdater implements JTA2.IDownloadList {
    private final IUI listener;
    private final boolean hideMetadata;

    public UpdateUI(Context context, IUI listener) throws JTA2InitializingException {
        super(context);
        this.listener = listener;
        this.hideMetadata = Prefs.getBoolean(context, Prefs.Keys.A2_HIDE_METADATA, false);
    }

    @Override
    public void loop() {
        jta2.tellAll(this);
    }

    @Override
    public void onDownloads(final List<Download> downloads) {
        if (listener == null) return;

        handler.post(new Runnable() {
            @Override
            public void run() {
                List<Download> filteredDownloads = new ArrayList<>();
                for (Download download : downloads)
                    if (!(hideMetadata && download.isMetadata()) || !download.isLinked())
                        filteredDownloads.add(download);

                listener.onUpdateAdapter(filteredDownloads);
            }
        });
    }

    @Override
    public void onException(boolean queuing, Exception ex) {
        ErrorHandler.get().notifyException(ex, false);
    }

    public interface IUI {
        void onUpdateAdapter(List<Download> downloads);
    }
}
