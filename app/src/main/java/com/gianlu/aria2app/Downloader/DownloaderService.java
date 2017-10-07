package com.gianlu.aria2app.Downloader;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.NonNull;
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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.HttpStatus;
import cz.msebera.android.httpclient.StatusLine;
import cz.msebera.android.httpclient.client.config.RequestConfig;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;

public class DownloaderService extends Service {
    private final DownloadTasks downloads = new DownloadTasks();
    private final List<DownloaderRunnable> activeRunnables = Collections.synchronizedList(new ArrayList<DownloaderRunnable>());
    private ExecutorService executorService;
    private LocalBroadcastManager broadcastManager;
    private Messenger messenger;
    private SavedDownloadsManager savedDownloadsManager;

    @Override
    public void onCreate() {
        broadcastManager = LocalBroadcastManager.getInstance(this);
        savedDownloadsManager = SavedDownloadsManager.get(this);
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

    private void startInternal(DownloadStartConfig.Task task) {
        File tempFile = new File(task.getCacheDir(), String.valueOf(task.id));
        HttpGet get = new HttpGet(task.uri);
        if (task.hasAuth())
            get.addHeader("Authorization", "Basic " + Base64.encodeToString((task.username + ":" + task.password).getBytes(), Base64.NO_WRAP));

        downloads.removeById(task.id);
        downloads.add(new DownloadTask(task));
        executorService.execute(new DownloaderRunnable(task, get, tempFile));
    }

    private void resumeInternal(SavedDownloadsManager.SavedState state) {
        try {
            DownloadStartConfig config = DownloadStartConfig.createForSavedState(this, state);
            startDownload(config);
        } catch (DownloadStartConfig.CannotCreateStartConfigException | DownloaderUtils.InvalidPathException | URISyntaxException e) {
            e.printStackTrace(); // TODO
        }
    }

    private void sendBroadcast(@NonNull String action, @NonNull Bundle bundle) {
        broadcastManager.sendBroadcast(new Intent(action).putExtras(bundle));
    }

    private void removeDownload(int id) {
        for (DownloaderRunnable runnable : activeRunnables) {
            if (runnable != null && runnable.singleTask.id == id) {
                runnable.stop();
                return;
            }
        }

        downloads.removeById(id);
    }

    private void pauseDownload(int id) {
        for (DownloaderRunnable runnable : activeRunnables) {
            if (runnable != null && runnable.singleTask.id == id) {
                runnable.pause();
                return;
            }
        }
    }

    private void resumeDownload(int id) {
        SavedDownloadsManager.SavedState state = savedDownloadsManager.getSavedState(id);
        if (state != null) resumeInternal(state);
        else throw new RuntimeException("THIS SHOULD BE HANDLED!!"); // TODO
    }

    private void restartDownload(int id) {
        throw new UnsupportedOperationException(); // TODO
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
                    bundle.putSerializable("downloads", new ArrayList<>(service.downloads));
                    service.sendBroadcast(DownloaderUtils.ACTION_LIST_DOWNLOADS, bundle);
                    break;
                case DownloaderUtils.PAUSE_DOWNLOAD:
                    service.pauseDownload(msg.arg1);
                    break;
                case DownloaderUtils.REMOVE_DOWNLOAD:
                    service.removeDownload(msg.arg1);
                    break;
                case DownloaderUtils.RESUME_DOWNLOAD:
                    service.resumeDownload(msg.arg1);
                    break;
                case DownloaderUtils.RESTART_DOWNLOAD:
                    service.restartDownload(msg.arg1);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private class DownloadTasks extends ArrayList<DownloadTask> implements Serializable {

        @Override
        public void add(int index, DownloadTask element) {
            super.add(index, element);
            notifyItemAdded(element);
        }

        @Override
        public boolean add(DownloadTask element) {
            boolean a = super.add(element);
            notifyItemAdded(element);
            return a;
        }

        private void notifyItemAdded(DownloadTask item) {
            Bundle bundle = new Bundle();
            bundle.putSerializable("item", item);
            sendBroadcast(DownloaderUtils.ACTION_ITEM_INSERTED, bundle);

            notifyCountChanged();
        }

        private void notifyItemRemoved(int pos) {
            Bundle bundle = new Bundle();
            bundle.putInt("pos", pos);
            sendBroadcast(DownloaderUtils.ACTION_ITEM_REMOVED, bundle);

            notifyCountChanged();
        }

        private void notifyItemChanged(DownloadTask item) {
            Bundle bundle = new Bundle();
            bundle.putInt("pos", indexOf(item));
            bundle.putSerializable("item", item);
            sendBroadcast(DownloaderUtils.ACTION_ITEM_CHANGED, bundle);
        }

        private void notifyCountChanged() {
            Bundle bundle = new Bundle();
            bundle.putInt("count", size());
            sendBroadcast(DownloaderUtils.ACTION_COUNT_CHANGED, bundle);
        }

        @Override
        public DownloadTask remove(int index) {
            DownloadTask a = super.remove(index);
            notifyItemRemoved(index);

            if (a.task.resumable)
                savedDownloadsManager.removeState(DownloaderService.this, a.task.id);

            return a;
        }

        @Override
        public boolean remove(Object o) {
            int pos = indexOf(o);
            boolean a = super.remove(o);
            notifyItemRemoved(pos);
            return a;
        }

        @Nullable
        DownloadTask find(int id) {
            for (DownloadTask task : this)
                if (task.task.id == id)
                    return task;

            return null;
        }

        void removeById(int id) {
            ListIterator<DownloadTask> iterator = listIterator();
            while (iterator.hasNext())
                if (iterator.next().task.id == id)
                    iterator.remove();
        }
    }

    private class DownloaderRunnable implements Runnable {
        private final HttpGet get;
        private final File tempFile;
        private final DownloadStartConfig.Task singleTask;
        private volatile boolean shouldStop = false;
        private volatile boolean saveState = false;

        private DownloaderRunnable(DownloadStartConfig.Task task, HttpGet get, File tempFile) {
            this.singleTask = task;
            this.get = get;
            this.tempFile = tempFile;
        }

        private void saveState() {
            savedDownloadsManager.saveState(DownloaderService.this, singleTask.id, singleTask.uri, tempFile, singleTask.destFile, singleTask.getProfileId());
        }

        private void pause() {
            shouldStop = true;
            saveState = true;
        }

        private void stop() {
            shouldStop = true;
            saveState = false;
        }

        @Override
        public void run() {
            DownloadTask task = downloads.find(singleTask.id);
            if (task == null) return; // What?

            task.status = DownloadTask.Status.STARTED;
            downloads.notifyItemChanged(task);

            try (CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom()
                    .setConnectTimeout(5000)
                    .setConnectionRequestTimeout(5000)
                    .setSocketTimeout(5000)
                    .build()).build()) {

                long downloaded = 0;
                if (singleTask.resumable && tempFile.exists()) {
                    long toSkip = tempFile.length();
                    if (toSkip <= 0) {
                        saveState();

                        task.status = DownloadTask.Status.PAUSED;
                        task.ex = new DownloaderException("File length is lower than 0: " + toSkip);
                        downloads.notifyItemChanged(task);
                        DownloaderService.this.activeRunnables.remove(this);
                        return;
                    }

                    get.addHeader("Range", "bytes=" + toSkip + "-");
                    downloaded = toSkip;
                }

                HttpResponse resp = client.execute(get);

                StatusLine sl = resp.getStatusLine();
                if (singleTask.resumable) {
                    if (sl.getStatusCode() != HttpStatus.SC_PARTIAL_CONTENT) {
                        task.status = DownloadTask.Status.FAILED;
                        task.ex = new DownloaderException("Server doesn't support partial content.");
                        downloads.notifyItemChanged(task);
                        return;
                    }
                } else {
                    if (sl.getStatusCode() != HttpStatus.SC_OK) {
                        task.status = DownloadTask.Status.FAILED;
                        task.ex = new DownloaderException(new StatusCodeException(sl));
                        downloads.notifyItemChanged(task);
                        return;
                    }
                }

                DownloaderService.this.activeRunnables.add(this);

                HttpEntity entity = resp.getEntity();
                InputStream in = entity.getContent();

                task.length = entity.getContentLength() + downloaded;
                downloads.notifyItemChanged(task);

                try (FileOutputStream out = new FileOutputStream(tempFile, singleTask.resumable)) {
                    byte[] buffer = new byte[4096];

                    int count;
                    while (!shouldStop && (count = in.read(buffer)) != -1) {
                        out.write(buffer, 0, count);
                        out.flush();

                        downloaded += count;
                        task.status = DownloadTask.Status.RUNNING;
                        task.downloaded = downloaded;
                        downloads.notifyItemChanged(task); // FIXME: Refreshing too fast
                    }
                }

                if (shouldStop) {
                    get.releaseConnection();

                    if (saveState) {
                        saveState();

                        task.status = DownloadTask.Status.PAUSED;
                        downloads.notifyItemChanged(task);
                        DownloaderService.this.activeRunnables.remove(this);
                        return; // IMPORTANT: Won't delete the cache file
                    } else {
                        downloads.removeById(singleTask.id);
                    }
                } else {
                    if (!tempFile.renameTo(singleTask.destFile)) {
                        task.status = DownloadTask.Status.FAILED;
                        task.ex = new DownloaderException("Couldn't move completed download!");
                        downloads.notifyItemChanged(task);
                        DownloaderService.this.activeRunnables.remove(this);
                        return;
                    }

                    task.status = DownloadTask.Status.COMPLETED;
                    downloads.notifyItemChanged(task);
                }

                if (!tempFile.delete()) tempFile.deleteOnExit();

                DownloaderService.this.activeRunnables.remove(this);
            } catch (IOException ex) {
                task.status = DownloadTask.Status.FAILED;
                task.ex = new DownloaderException(ex);
                downloads.notifyItemChanged(task);
            }
        }
    }
}
