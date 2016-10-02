package com.gianlu.aria2app.MoreAboutDownload.FilesFragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Environment;
import android.support.v7.app.AlertDialog;

import com.gianlu.aria2app.NetIO.JTA2.File;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Services.DownloadService;
import com.gianlu.aria2app.Utils;

import java.util.Objects;

class DownloadFileListener implements DialogInterface.OnClickListener {
    private Activity context;
    private File file;
    private String dDir;

    DownloadFileListener(Activity context, File file, String dDir) {
        this.context = context;
        this.file = file;
        this.dDir = dDir;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (Objects.equals(file.completedLength, file.length)) {
            shouldStartDownload();
        } else {
            Utils.showDialog(context, new AlertDialog.Builder(context)
                    .setTitle(R.string.downloadIncomplete)
                    .setMessage(R.string.downloadIncompleteMessage)
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            shouldStartDownload();
                        }
                    }));
        }
    }

    private void shouldStartDownload() {
        java.io.File ioFile = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), file.getName());

        int c = 0;
        while (ioFile.exists()) {
            String[] split = file.getName().split(".");
            split[split.length - 1] += "." + c;

            String newName = "";
            boolean first = true;
            for (String s : split) {
                if (!first)
                    newName += ".";
                else
                    first = false;

                newName += s;
            }

            //noinspection ResultOfMethodCallIgnored
            ioFile.renameTo(new java.io.File(ioFile.getParent(), newName));
            c++;
        }

        context.startService(DownloadService.createStartIntent(context, ioFile, file.getRelativePath(dDir)));
    }
}
