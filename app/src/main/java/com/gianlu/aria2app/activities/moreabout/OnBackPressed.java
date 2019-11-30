package com.gianlu.aria2app.activities.moreabout;

public interface OnBackPressed {
    int CODE_CLOSE_SHEET = 1;

    boolean canGoBack(int code);
}
