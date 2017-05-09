package com.gianlu.aria2app.Activities.MoreAboutDownload.FilesFragment;

import com.gianlu.aria2app.NetIO.JTA2.AFile;

public class TreeFile {
    public AFile file;
    public FileViewHolder viewHolder;

    TreeFile(AFile file) {
        this.file = file;
    }
}
