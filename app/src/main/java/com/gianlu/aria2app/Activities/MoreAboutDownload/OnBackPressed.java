package com.gianlu.aria2app.Activities.MoreAboutDownload;

public interface OnBackPressed {
    int CODE_CLOSE_SHEET = 1;

    boolean canGoBack(int code);

    void onBackPressed();
}
