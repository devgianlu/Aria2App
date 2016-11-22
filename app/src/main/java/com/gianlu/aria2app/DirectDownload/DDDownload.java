package com.gianlu.aria2app.DirectDownload;

import com.liulishuo.filedownloader.BaseDownloadTask;

public class DDDownload {
    public int id;
    public String name;

    private DDDownload() {
    }

    public static DDDownload fromTask(BaseDownloadTask task) {
        DDDownload item = new DDDownload();
        item.id = task.getId();
        item.name = task.getFilename();

        return item;
    }
}
