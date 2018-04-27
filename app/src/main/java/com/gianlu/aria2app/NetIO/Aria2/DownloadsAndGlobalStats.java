package com.gianlu.aria2app.NetIO.Aria2;

import java.util.ArrayList;
import java.util.List;

public class DownloadsAndGlobalStats {
    public final GlobalStats stats;
    public final List<DownloadWithUpdate> downloads;

    DownloadsAndGlobalStats(List<DownloadWithUpdate> allDownloads, boolean ignoreMetadata, GlobalStats stats) {
        this.stats = stats;

        downloads = new ArrayList<>();
        if (ignoreMetadata) {
            for (DownloadWithUpdate download : allDownloads) {
                DownloadWithUpdate.SmallUpdate last = download.update();
                if (!(last.isMetadata() && (last.followedBy != null || last.status == Download.Status.COMPLETE)))
                    downloads.add(download);
            }
        } else {
            downloads.addAll(allDownloads);
        }
    }
}
