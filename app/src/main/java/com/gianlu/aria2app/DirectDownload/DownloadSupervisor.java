package com.gianlu.aria2app.DirectDownload;

public class DownloadSupervisor {
    private static DownloadSupervisor supervisor;

    public static DownloadSupervisor getInstance() {
        if (supervisor == null)
            supervisor = new DownloadSupervisor();

        return supervisor;
    }
}
