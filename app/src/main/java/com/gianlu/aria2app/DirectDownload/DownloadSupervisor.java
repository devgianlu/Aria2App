package com.gianlu.aria2app.DirectDownload;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Base64;

import com.gianlu.aria2app.CurrentProfile;
import com.gianlu.aria2app.MoreAboutDownload.FilesFragment.TreeDirectory;
import com.gianlu.aria2app.MoreAboutDownload.FilesFragment.TreeFile;
import com.gianlu.aria2app.MoreAboutDownload.InfoFragment.UpdateUI;
import com.gianlu.aria2app.NetIO.JTA2.AFile;
import com.gianlu.aria2app.Profile.DirectDownload;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadLargeFileListener;
import com.liulishuo.filedownloader.FileDownloader;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DownloadSupervisor extends FileDownloadLargeFileListener {
    private static DownloadSupervisor supervisor;
    private final List<BaseDownloadTask> downloads;
    private IUpdateCount countListener;
    private IListener listener;

    private DownloadSupervisor() {
        downloads = new ArrayList<>();
    }

    public static DownloadSupervisor getInstance() {
        if (supervisor == null)
            supervisor = new DownloadSupervisor();

        return supervisor;
    }

    @Nullable
    private static BaseDownloadTask createTask(Activity context, AFile file, FileDownloadLargeFileListener listener) {
        DirectDownload dd = CurrentProfile.getCurrentProfile(context).directDownload;

        String downloadPath = PreferenceManager.getDefaultSharedPreferences(context).getString("dd_downloadPath", null);
        File localPath;
        if (downloadPath == null) {
            localPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        } else {
            File path = new File(downloadPath);
            if (path.exists() && path.isDirectory()) {
                localPath = Utils.createDownloadLocalPath(path, file.getName());
            } else {
                CommonUtils.UIToast(context, Utils.ToastMessages.INVALID_DOWNLOAD_PATH, path.getAbsolutePath());
                return null;
            }
        }

        URL remoteURL = Utils.createDownloadRemoteURL(context, UpdateUI.dir, file);
        if (remoteURL == null) {
            CommonUtils.UIToast(context, Utils.ToastMessages.FAILED_DOWNLOAD_FILE, new NullPointerException("Remote URL is null"));
            return null;
        }

        BaseDownloadTask downloadTask = FileDownloader.getImpl().create(remoteURL.toString());
        downloadTask.setPath(localPath.getAbsolutePath())
                .setCallbackProgressMinInterval(1000)
                .setListener(listener);

        if (dd.auth)
            downloadTask.addHeader("Authorization", "Basic " + Base64.encodeToString((dd.username + ":" + dd.password).getBytes(), Base64.NO_WRAP));

        return downloadTask;
    }

    private static List<BaseDownloadTask> createTasks(Activity context, FileDownloadLargeFileListener listener, TreeDirectory parent) {
        List<BaseDownloadTask> list = new ArrayList<>();

        for (TreeDirectory child : parent.children) {
            list.addAll(createTasks(context, listener, child));
        }

        for (TreeFile file : parent.files) {
            BaseDownloadTask task = createTask(context, file.file, listener);
            if (task != null)
                list.add(task);
        }

        return list;
    }

    public void setDownloadsCountListener(IUpdateCount listener) {
        this.countListener = listener;
        listener.onUpdateDownloadsCount(downloads.size());
    }

    public void attachListener(IListener listener) {
        this.listener = listener;

        listener.onUpdateAdapter(downloads.size());
    }

    @Override
    protected void connected(BaseDownloadTask task, String etag, boolean isContinue, long soFarBytes, long totalBytes) {
        if (listener != null)
            listener.onUpdateAdapter(downloads.size());
    }

    @Override
    protected void retry(BaseDownloadTask task, Throwable ex, int retryingTimes, long soFarBytes) {
        if (listener != null) {
            Context context = listener.onUpdateAdapter(downloads.size());
            if (context != null)
                CommonUtils.logMe(context, ex);
        }
    }

    @Override
    protected void started(BaseDownloadTask task) {
        if (listener != null)
            listener.onUpdateAdapter(downloads.size());
    }

    @Override
    protected void completed(BaseDownloadTask task) {
        if (listener != null)
            listener.onUpdateAdapter(downloads.size());
    }

    @Override
    protected void error(BaseDownloadTask task, Throwable ex) {
        if (listener != null) {
            Context context = listener.onUpdateAdapter(downloads.size());
            if (context != null)
                CommonUtils.logMe(context, ex);
        }
    }

    @Override
    protected void warn(BaseDownloadTask task) {
        if (listener != null)
            listener.onUpdateAdapter(downloads.size());
    }

    @Override
    protected void pending(BaseDownloadTask task, long soFarBytes, long totalBytes) {
        if (listener != null)
            listener.onUpdateAdapter(downloads.size());
    }

    @Override
    protected void progress(BaseDownloadTask task, long soFarBytes, long totalBytes) {
        if (listener != null)
            listener.onUpdateAdapter(downloads.size());
    }

    @Override
    protected void paused(BaseDownloadTask task, long soFarBytes, long totalBytes) {
        if (listener != null)
            listener.onUpdateAdapter(downloads.size());
    }

    public void start(Activity context, AFile file) {
        BaseDownloadTask downloadTask = createTask(context, file, this);
        if (downloadTask != null) {
            downloadTask.start();
            downloads.add(downloadTask);
        }

        if (countListener != null)
            countListener.onUpdateDownloadsCount(downloads.size());
    }

    public void start(Activity context, TreeDirectory parent) {
        final List<BaseDownloadTask> tasks = createTasks(context, this, parent);
        for (BaseDownloadTask task : tasks) {
            task.start();
            downloads.add(task);
        }

        if (countListener != null)
            countListener.onUpdateDownloadsCount(downloads.size());
    }

    public List<BaseDownloadTask> getDownloads() {
        return downloads;
    }

    @Nullable
    public BaseDownloadTask get(int id) {
        for (BaseDownloadTask obj : new ArrayList<>(downloads))
            if (obj.getId() == id)
                return obj;

        return null;
    }

    public void remove(int id) {
        for (BaseDownloadTask obj : new ArrayList<>(downloads))
            if (obj.getId() == id)
                downloads.remove(obj);
    }

    public interface IListener {
        Context onUpdateAdapter(int count);
    }

    public interface IUpdateCount {
        void onUpdateDownloadsCount(int count);
    }
}
