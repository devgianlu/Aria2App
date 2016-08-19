package com.gianlu.aria2app.MoreAboutDownload.FilesFragment;

import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gianlu.aria2app.R;

public class DirectoryViewHolder extends ViewHolder {
    public ImageButton toggle;
    public TextView name;
    public ProgressBar progressBar;
    public TextView percentage;

    public DirectoryViewHolder(View rootView) {
        super(rootView, TYPE.DIRECTORY);

        toggle = (ImageButton) rootView.findViewById(R.id.directoryItem_toggle);
        name = (TextView) rootView.findViewById(R.id.directoryItem_name);
        progressBar = (ProgressBar) rootView.findViewById(R.id.directoryItem_progressBar);
        percentage = (TextView) rootView.findViewById(R.id.directoryItem_percentage);
    }
}
