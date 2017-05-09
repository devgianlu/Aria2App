package com.gianlu.aria2app.Activities.MoreAboutDownload.FilesFragment;

import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gianlu.aria2app.R;

class FileViewHolder extends ViewHolder {
    public final TextView name;
    public final ImageView status;
    final ProgressBar progressBar;
    final TextView percentage;

    FileViewHolder(View rootView) {
        super(rootView);

        name = (TextView) rootView.findViewById(R.id.fileItem_name);
        progressBar = (ProgressBar) rootView.findViewById(R.id.fileItem_progressBar);
        percentage = (TextView) rootView.findViewById(R.id.fileItem_percentage);
        status = (ImageView) rootView.findViewById(R.id.fileItem_status);
    }
}
