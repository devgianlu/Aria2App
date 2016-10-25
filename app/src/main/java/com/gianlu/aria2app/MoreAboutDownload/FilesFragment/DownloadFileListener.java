package com.gianlu.aria2app.MoreAboutDownload.FilesFragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Environment;
import android.support.v7.app.AlertDialog;

import com.gianlu.aria2app.Google.Analytics;
import com.gianlu.aria2app.NetIO.JTA2.File;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Services.DownloadService;
import com.gianlu.commonutils.CommonUtils;
import com.google.android.gms.analytics.HitBuilders;

import java.util.Objects;

class DownloadFileListener implements DialogInterface.OnClickListener {
    private final Activity context;
    private final File file;
    private final String dDir;

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
            CommonUtils.showDialog(context, new AlertDialog.Builder(context)
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

        int c = 1;
        while (ioFile.exists()) {
            String name = file.getName();
            String[] split = name.split("\\.");

            String newName;
            if (split.length == 1) {
                newName = name + "." + c;
            } else {
                String ext = split[split.length - 1];
                newName = name.substring(0, name.length() - ext.length() - 1) + "." + c + "." + ext;
            }

            ioFile = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), newName);
            c++;
        }

        if (Analytics.isTrackingAllowed(context))
            Analytics.getDefaultTracker(context.getApplication()).send(new HitBuilders.EventBuilder()
                    .setCategory(Analytics.CATEGORY_USER_INPUT)
                    .setAction(Analytics.ACTION_DOWNLOAD_FILE).build());

        context.startService(DownloadService.createStartIntent(context, ioFile, file.getRelativePath(dDir)));
    }
}
