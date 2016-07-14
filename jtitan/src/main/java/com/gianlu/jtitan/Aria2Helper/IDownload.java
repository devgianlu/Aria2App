package com.gianlu.jtitan.Aria2Helper;

public interface IDownload {
    void onDownload(Download download);

    void onException(Exception exception);
}
