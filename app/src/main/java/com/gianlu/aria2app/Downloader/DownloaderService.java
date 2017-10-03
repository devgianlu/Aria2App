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

import com.gianlu.aria2app.BuildConfig;
import com.gianlu.aria2app.NetIO.StatusCodeException;

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
    private static final int MAX_SIMULTANEOUS_DOWNLOADS = 3; // TODO: Should be selectable
    private final ExecutorService executorService = Executors.newFixedThreadPool(MAX_SIMULTANEOUS_DOWNLOADS);
    private final DownloadTasks downloads = new DownloadTasks();
    private LocalBroadcastManager broadcastManager;
    private Messenger messenger;

    @Override
    public void onCreate() {
        broadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        downloads.notifyCountChanged();
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

        executorService.execute(new DownloaderRunnable(task, get, tempFile));
        downloads.add(new DownloadTask(task));
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
                case DownloaderUtils.LIST_DOWNLOADS:
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("downloads", service.downloads);
                    service.sendBroadcast(DownloaderUtils.ACTION_LIST_DOWNLOADS, bundle);
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

        private void updateStatus(int id, DownloaderException ex) {
            updateStatus(id, DownloadTask.Status.FAILED, ex);

            if (BuildConfig.DEBUG) ex.printStackTrace();
        }

        private void updateStatus(int id, DownloadTask.Status status) {
            updateStatus(id, status, null);
        }

        private void updateStatus(int id, DownloadTask.Status status, @Nullable DownloaderException ex) {
            synchronized (this) {
                for (DownloadTask task : this) {
                    if (task.task.id == id) {
                        task.status = status;
                        task.ex = ex;
                    }
                }
            }
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
            downloads.updateStatus(id, DownloadTask.Status.STARTED);

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpResponse resp = client.execute(get);

                StatusLine sl = resp.getStatusLine();
                if (sl.getStatusCode() != HttpStatus.SC_OK) {
                    downloads.updateStatus(id, new DownloaderException(new StatusCodeException(sl)));
                    return;
                }

                HttpEntity entity = resp.getEntity();
                InputStream in = entity.getContent();
                try (FileOutputStream out = new FileOutputStream(tempFile, false)) {
                    byte[] buffer = new byte[4096];

                    int count;
                    while ((count = in.read(buffer)) != -1) {
                        out.write(buffer, 0, count);
                        out.flush();
                    }
                }

                EntityUtils.consumeQuietly(entity);

                if (!tempFile.renameTo(destFile)) {
                    downloads.updateStatus(id, new DownloaderException("Couldn't move completed download!"));
                    return;
                }

                if (!tempFile.delete()) tempFile.deleteOnExit();

                downloads.updateStatus(id, DownloadTask.Status.COMPLETED);
            } catch (IOException ex) {
                downloads.updateStatus(id, new DownloaderException(ex));
            }
        }
    }
}
