package com.gianlu.aria2app.DirectDownload;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gianlu.aria2app.NetIO.JTA2.File;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadLargeFileListener;

import java.util.Locale;

public class FileDownloadListener extends FileDownloadLargeFileListener {
    private final Activity context;
    private final ViewHolder holder;
    private long lastProgressTime = System.currentTimeMillis() / 1000;
    private long lastProgressSoFarBytes = 0;

    @SuppressWarnings("deprecation")
    public FileDownloadListener(final Activity context, File file, final BaseDownloadTask task) {
        this.context = context;
        this.holder = new ViewHolder(LayoutInflater.from(context).inflate(R.layout.downloading_dialog, null, false));

        holder.status.setText(R.string.starting);
        holder.url.setText(Html.fromHtml(context.getString(R.string.url, task.getUrl())));
        holder.path.setText(Html.fromHtml(context.getString(R.string.localPath, task.getPath())));

        CommonUtils.showDialog(context, new AlertDialog.Builder(context)
                .setTitle(file.getName())
                .setView(holder.rootView)
                .setCancelable(false)
                .setNeutralButton(R.string.background, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        CommonUtils.UIToast(context, Utils.ToastMessages.DO_NOT_CLOSE_APP);
                        // TODO: Background
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        task.pause();

                        java.io.File file = new java.io.File(task.getPath());
                        if (!file.delete())
                            CommonUtils.UIToast(context, Utils.ToastMessages.CANNOT_DELETE_FILE);
                    }
                }));
    }

    @Override
    protected void started(BaseDownloadTask task) {
        holder.status.setText(R.string.connecting);
        holder.invalidate();
        System.out.println("FileDownloadListener.started");
    }

    @Override
    protected void connected(BaseDownloadTask task, String etag, boolean isContinue, long soFarBytes, long totalBytes) {
        holder.status.setText(R.string.downloading);
        holder.invalidate();
        System.out.println("FileDownloadListener.connected");
    }

    @Override
    protected void retry(BaseDownloadTask task, Throwable ex, int retryingTimes, long soFarBytes) {
        System.out.println("FileDownloadListener.retry");
    }

    @Override
    protected void pending(BaseDownloadTask task, long soFarBytes, long totalBytes) {
        System.out.println("FileDownloadListener.pending");
    }

    @Override
    protected void progress(BaseDownloadTask task, final long soFarBytes, long totalBytes) {
        final float progress = ((float) soFarBytes) / ((float) totalBytes) * 100;
        final long now = System.currentTimeMillis() / 1000;

        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                holder.status.setText(R.string.downloading);
                holder.progress.setProgress((int) progress);
                holder.progressText.setText(String.format(Locale.getDefault(), "%.2f", progress));
                holder.speed.setText(CommonUtils.speedFormatter((soFarBytes - lastProgressSoFarBytes) / (now - lastProgressTime)));
                holder.invalidate();
            }
        });

        lastProgressTime = now;
        lastProgressSoFarBytes = soFarBytes;
        System.out.println("FileDownloadListener.progress: " + progress);
    }

    @Override
    protected void paused(BaseDownloadTask task, long soFarBytes, long totalBytes) {
        System.out.println("FileDownloadListener.paused");
    }

    @Override
    protected void completed(BaseDownloadTask task) {
        System.out.println("FileDownloadListener.completed");
    }

    @Override
    protected void error(BaseDownloadTask task, Throwable ex) {
        ex.printStackTrace();
        System.out.println("FileDownloadListener.error");
    }

    @Override
    protected void warn(BaseDownloadTask task) {
        System.out.println("FileDownloadListener.warn");
    }

    private class ViewHolder {
        public final LinearLayout rootView;
        public final TextView status;
        public final TextView path;
        public final TextView url;
        public final ProgressBar progress;
        public final TextView progressText;
        public final TextView speed;

        private ViewHolder(View rootView) {
            this.rootView = (LinearLayout) rootView;

            status = (TextView) rootView.findViewById(R.id.downloadingDialog_status);
            path = (TextView) rootView.findViewById(R.id.downloadingDialog_path);
            url = (TextView) rootView.findViewById(R.id.downloadingDialog_url);
            progress = (ProgressBar) rootView.findViewById(R.id.downloadingDialog_progress);
            progress.setMax(100);
            progressText = (TextView) rootView.findViewById(R.id.downloadingDialog_progressText);
            speed = (TextView) rootView.findViewById(R.id.downloadingDialog_speed);
        }

        public void invalidate() {
            rootView.invalidate();
        }
    }
}
