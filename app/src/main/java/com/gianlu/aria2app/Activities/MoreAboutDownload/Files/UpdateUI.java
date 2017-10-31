package com.gianlu.aria2app.Activities.MoreAboutDownload.Files;

import android.content.Context;

import com.gianlu.aria2app.NetIO.BaseUpdater;
import com.gianlu.aria2app.NetIO.ErrorHandler;
import com.gianlu.aria2app.NetIO.JTA2.AriaFile;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;

import java.util.List;
import java.util.Map;

public class UpdateUI extends BaseUpdater implements JTA2.IFiles {
    private final String gid;
    private final IUI listener;
    private String commonRoot;

    public UpdateUI(Context context, String gid, IUI listener) throws JTA2.InitializingException {
        super(context);
        this.gid = gid;
        this.listener = listener;
    }

    @Override
    public synchronized void start() {
        jta2.getOption(gid, new JTA2.IOption() {
            @Override
            public void onOptions(Map<String, String> options) {
                String dir = options.get("dir");
                if (dir == null) {
                    onException(new Exception("dir is undefined!"));
                    return;
                }

                TreeNode.guessSeparator(dir);

                start(dir);
            }

            @Override
            public void onException(final Exception ex) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null) listener.onFatalException(ex);
                    }
                });
            }
        });
    }

    private void start(String commonRoot) {
        this.commonRoot = commonRoot;
        super.start();
    }

    @Override
    public void loop() {
        jta2.getFiles(gid, this);
    }

    @Override
    public void onFiles(final List<AriaFile> files) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) listener.onUpdateHierarchy(files, commonRoot);
            }
        });
    }

    @Override
    public void onException(Exception ex) {
        ErrorHandler.get().notifyException(ex, false);
    }

    public interface IUI {
        void onUpdateHierarchy(List<AriaFile> files, String commonRoot);

        void onFatalException(Exception ex);
    }
}
