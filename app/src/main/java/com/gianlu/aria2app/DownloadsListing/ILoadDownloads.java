package com.gianlu.aria2app.DownloadsListing;

public interface ILoadDownloads {
    void onStart();

    void onException(boolean queuing, Exception ex);
    void onEnd();
}
