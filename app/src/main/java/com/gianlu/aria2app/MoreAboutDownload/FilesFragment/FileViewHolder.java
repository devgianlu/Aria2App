package com.gianlu.aria2app.MoreAboutDownload.FilesFragment;

import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gianlu.aria2app.R;

public class FileViewHolder extends ViewHolder {
    public TextView name;
    public ProgressBar progressBar;
    public TextView percentage;
    public ImageView status;

    public FileViewHolder(View rootView) {
        super(rootView, TYPE.FILE);

        name = (TextView) rootView.findViewById(R.id.fileItem_name);
        progressBar = (ProgressBar) rootView.findViewById(R.id.fileItem_progressBar);
        percentage = (TextView) rootView.findViewById(R.id.fileItem_percentage);
        status = (ImageView) rootView.findViewById(R.id.fileItem_status);
    }
}
