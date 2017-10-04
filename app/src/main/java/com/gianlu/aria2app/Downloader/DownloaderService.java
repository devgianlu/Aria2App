package com.gianlu.aria2app.Downloader;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;

import com.gianlu.aria2app.NetIO.StatusCodeException;
import com.gianlu.aria2app.PKeys;
import com.gianlu.commonutils.Prefs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.HttpStatus;
import cz.msebera.android.httpclient.StatusLine;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.util.EntityUtils;

public class DownloaderService extends Service {
    private final DownloadTasks downloads = new DownloadTasks();
    private ExecutorService executorService;
    private LocalBroadcastManager broadcastManager;
    private Messenger messenger;

    @Override
    public void onCreate() {
        broadcastManager = LocalBroadcastManager.getInstance(this);
        downloads.notifyCountChanged();

        int maxSimultaneousDownloads = Prefs.getFakeInt(this, PKeys.DD_MAX_SIMULTANEOUS_DOWNLOADS, 3);
        if (maxSimultaneousDownloads <= 0) maxSimultaneousDownloads = 3;
        else if (maxSimultaneousDownloads > 10) maxSimultaneousDownloads = 10;
        executorService = Executors.newFixedThreadPool(maxSimultaneousDownloads);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (messenger == null) messenger = new Messenger(new ServiceHandler(this));
        return messenger.getBinder();
    }

    private void startDownload(DownloadStartConfig config) {
        if (config.tasks.isEmpty()) return;

        if (config.tasks.size() == 1) {
            startInternal(config.tasks.get(0));
        } else {
            // TODO: Simultaneous/consecutive download of multiple files (aka directory)
        }
    }

    private DownloaderRunnable resumeInternal() {
        throw new UnsupportedOperationException(); // TODO
    }

    private void startInternal(DownloadStartConfig.Task task) {
        File tempFile = new File(task.getCacheDir(), String.valueOf(task.id));
        HttpGet get = new HttpGet(task.uri);
        if (task.hasAuth())
            get.addHeader("Authorization", "Basic " + Base64.encodeToString((task.username + ":" + task.password).getBytes(), Base64.NO_WRAP));

        downloads.add(new DownloadTask(task));
        executorService.execute(new DownloaderRunnable(task, get, tempFile));
    }

    private void sendBroadcast(String action, Bundle bundle) {
        broadcastManager.sendBroadcast(new Intent(action).putExtras(bundle));
    }

    public static class DownloaderException extends Exception {
        DownloaderException(Throwable cause) {
            super(cause);
        }

        DownloaderException(String message) {
            super(message);
        }
    }

    private static class ServiceHandler extends Handler {
        private final WeakReference<DownloaderService> service;

        ServiceHandler(DownloaderService service) {
            this.service = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            DownloaderService service = this.service.get();
            if (service == null) {
                super.handleMessage(msg);
                return;
            }

            switch (msg.what) {
                case DownloaderUtils.START_DOWNLOAD:
                    service.startDownload((DownloadStartConfig) msg.obj);
                    break;
                case DownloaderUtils.REFRESH_COUNT:
                    service.downloads.notifyCountChanged();
                    break;
                case DownloaderUtils.LIST_DOWNLOADS:
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("downloads", service.downloads);
                    service.sendBroadcast(DownloaderUtils.ACTION_LIST_DOWNLOADS, bundle);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    public class DownloadTasks extends ArrayList<DownloadTask> implements Serializable {

        @Override
        public void add(int index, DownloadTask element) {
            super.add(index, element);
            notifyCountChanged();
        }

        private void notifyCountChanged() {
            Bundle bundle = new Bundle();
            bundle.putInt("count", size());
            sendBroadcast(DownloaderUtils.ACTION_COUNT_CHANGED, bundle);
        }

        @Override
        public boolean add(DownloadTask downloadTask) {
            boolean a = super.add(downloadTask);
            notifyCountChanged();
            return a;
        }

        @Nullable
        DownloadTask find(int id) {
            for (DownloadTask task : this)
                if (task.task.id == id)
                    return task;

            return null;
        }
    }

    private class DownloaderRunnable implements Runnable {
        private final int id;
        private final HttpGet get;
        private final File tempFile;
        private final File destFile;

        private DownloaderRunnable(DownloadStartConfig.Task task, HttpGet get, File tempFile) {
            this.id = task.id;
            this.get = get;
            this.tempFile = tempFile;
            this.destFile = task.destFile;
        }

        @Override
        public void run() {
            DownloadTask task = downloads.find(id);
            if (task == null) return; // What?

            task.status = DownloadTask.Status.STARTED;

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpResponse resp = client.execute(get);

                StatusLine sl = resp.getStatusLine();
                if (sl.getStatusCode() != HttpStatus.SC_OK) {
                    task.status = DownloadTask.Status.FAILED;
                    task.ex = new DownloaderException(new StatusCodeException(sl));
                    return;
                }

                HttpEntity entity = resp.getEntity();
                InputStream in = entity.getContent();

                task.length = entity.getContentLength();

                long downloaded = 0;
                try (FileOutputStream out = new FileOutputStream(tempFile, false)) {
                    byte[] buffer = new byte[4096];

                    int count;
                    while ((count = in.read(buffer)) != -1) {
                        out.write(buffer, 0, count);
                        out.flush();

                        downloaded += count;
                        task.status = DownloadTask.Status.RUNNING;
                        task.downloaded = downloaded;
                    }
                }

                EntityUtils.consumeQuietly(entity);

                if (!tempFile.renameTo(destFile)) {
                    task.status = DownloadTask.Status.FAILED;
                    task.ex = new DownloaderException("Couldn't move completed download!");
                    return;
                }

                if (!tempFile.delete()) tempFile.deleteOnExit();

                task.status = DownloadTask.Status.COMPLETED;
            } catch (IOException ex) {
                task.status = DownloadTask.Status.FAILED;
                task.ex = new DownloaderException(ex);
            }
        }
    }
}
