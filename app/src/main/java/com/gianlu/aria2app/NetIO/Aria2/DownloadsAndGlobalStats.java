package com.gianlu.aria2app.NetIO.Aria2;

import java.util.ArrayList;
import java.util.List;

public class DownloadsAndGlobalStats {
    public final GlobalStats stats;
    public final List<Download> downloads;

    DownloadsAndGlobalStats(List<Download> allDownloads, boolean ignoreMetadata, GlobalStats stats) {
        this.stats = stats;

        downloads = new ArrayList<>();
        if (ignoreMetadata) {
            for (Download download : allDownloads)
                if (!(download.isMetadata() && (download.followedBy != null || download.status == Download.Status.COMPLETE)))
                    downloads.add(download);
        } else {
            downloads.addAll(allDownloads);
        }
    }
}
