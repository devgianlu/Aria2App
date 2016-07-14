package com.gianlu.jtitan.Aria2Helper;

import java.util.List;

public interface IDownloadList {
    void onDownloads(List<Download> downloads);

    void onException(Exception exception);
}
