package com.gianlu.aria2app.MoreAboutDownload;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;

import java.util.Locale;

public class DownloadProgressDialogHelper {
    private static Activity context;
    private ProgressBar bar;
    private TextView url;
    private TextView percentage;
    private TextView progressSize;
    private TextView speed;

    private Long fileLength;
    private AlertDialog dialog;

    @SuppressLint("InflateParams")
    public DownloadProgressDialogHelper(AlertDialog dialog) {
        this.dialog = dialog;
        View view = ((LayoutInflater) dialog.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.download_file_dialog, null);
        bar = (ProgressBar) view.findViewById(R.id.downloadFileDialog_progressBar);
        url = (TextView) view.findViewById(R.id.downloadFileDialog_url);
        percentage = (TextView) view.findViewById(R.id.downloadFileDialog_percentage);
        progressSize = (TextView) view.findViewById(R.id.downloadFileDialog_progressSize);
        speed = (TextView) view.findViewById(R.id.downloadFileDialog_speed);

        dialog.setView(view);
    }

    public static void setContext(Activity context) {
        DownloadProgressDialogHelper.context = context;
    }

    public void setFileURL(String surl) {
        url.setText(surl);
    }

    public void setFileLength(long fileLength) {
        this.fileLength = fileLength;
    }

    public void setProgress(final Long downloaded) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bar.setProgress((int) (downloaded.floatValue() / fileLength.floatValue() * 100));
                percentage.setText(String.format(Locale.getDefault(), "%.02f %%", downloaded.floatValue() / fileLength.floatValue() * 100));
                progressSize.setText(String.format(Locale.getDefault(), "%s / %s", Utils.DimensionFormatter(downloaded.floatValue()), Utils.DimensionFormatter(fileLength.floatValue())));
            }
        });
    }

    public void dismiss() {
        dialog.dismiss();
    }

    public void show() {
        dialog.show();
    }
}