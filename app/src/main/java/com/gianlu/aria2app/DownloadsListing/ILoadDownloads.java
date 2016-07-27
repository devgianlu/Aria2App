package com.gianlu.aria2app.DownloadsListing;

public interface ILoadDownloads {
    void onStart();

    void onException(Exception ex);
    void onEnd();
}
