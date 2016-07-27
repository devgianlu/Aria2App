package com.gianlu.aria2app.NetIO.JTA2;

public interface IDownload {
    void onDownload(Download download);

    void onException(Exception exception);
}
