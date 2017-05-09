package com.gianlu.aria2app.Activities.MoreAboutDownload.FilesFragment;

import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.gianlu.aria2app.R;

class DirectoryViewHolder extends ViewHolder {
    final ImageButton toggle;
    final TextView name;

    DirectoryViewHolder(View rootView) {
        super(rootView);

        toggle = (ImageButton) rootView.findViewById(R.id.directoryItem_toggle);
        name = (TextView) rootView.findViewById(R.id.directoryItem_name);
    }
}
