package com.gianlu.aria2app.MoreAboutDownload.FilesFragment;

import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.gianlu.aria2app.R;

class DirectoryViewHolder extends ViewHolder {
    ImageButton toggle;
    TextView name;

    DirectoryViewHolder(View rootView) {
        super(rootView, TYPE.DIRECTORY);

        toggle = (ImageButton) rootView.findViewById(R.id.directoryItem_toggle);
        name = (TextView) rootView.findViewById(R.id.directoryItem_name);
    }
}
