package com.gianlu.aria2app.MoreAboutDownload.FilesFragment;

import android.view.View;

public class ViewHolder {
    public View rootView;
    public TYPE type;

    public ViewHolder(View rootView, TYPE type) {
        this.rootView = rootView;
        this.type = type;
    }

    public enum TYPE {
        FILE,
        DIRECTORY
    }
}
