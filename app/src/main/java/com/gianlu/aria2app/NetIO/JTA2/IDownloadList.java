package com.gianlu.aria2app.NetIO.JTA2;

import java.util.List;

public interface IDownloadList {
    void onDownloads(List<Download> downloads);

    void onException(Exception exception);
}
