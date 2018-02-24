package com.gianlu.aria2app.Downloader;

import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiManager;
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
import com.gianlu.commonutils.Preferences.Prefs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DownloaderService extends Service {
    private static final int TIMEOUT = 5;
    private final DownloadTasks downloads = new DownloadTasks();
    private final List<DownloaderRunnable> activeRunnables = Collections.synchronizedList(new ArrayList<DownloaderRunnable>());
    private OkHttpClient client;
    private ExecutorService executorService;
    private LocalBroadcastManager broadcastManager;
    private Messenger messenger;
    private SavedDownloadsManager savedDownloadsManager;
    private WifiManager.WifiLock wifiLock;

    @Override
    public void onCreate() {
        savedDownloadsManager = SavedDownloadsManager.get(this);
        broadcastManager = LocalBroadcastManager.getInstance(this);
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wifiManager != null) {
            wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "DownloaderService");
            wifiLock.setReferenceCounted(false);
        }

        downloads.notifyCountChanged();

        int maxSimultaneousDownloads = Prefs.getFakeInt(this, PKeys.DD_MAX_SIMULTANEOUS_DOWNLOADS, 3);
        if (maxSimultaneousDownloads <= 0) maxSimultaneousDownloads = 3;
        else if (maxSimultaneousDownloads > 10) maxSimultaneousDownloads = 10;
        executorService = Executors.newFixedThreadPool(maxSimultaneousDownloads);

        client = new OkHttpClient.Builder().readTimeout(TIMEOUT, TimeUnit.SECONDS)
                .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getBooleanExtra("resume", false)) resumeSavedStates();
        return super.onStartCommand(intent, flags, startId);
    }

    private void resumeSavedStates() {
        for (SavedDownloadsManager.SavedState state : savedDownloadsManager.getAll())
            resumeInternal(state);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (messenger == null) messenger = new Messenger(new ServiceHandler(this));
        return messenger.getBinder();
    }

    private void startDownload(DownloadStartConfig config) {
        wifiLock.acquire();

        for (DownloadStartConfig.Task task : config.tasks)
            startInternal(task);
    }

    private void startInternal(DownloadStartConfig.Task task) {
        File tempFile = new File(task.getCacheDir(), String.valueOf(task.id));
        Request.Builder request = new Request.Builder();
        request.get().url(task.url);
        if (task.hasAuth())
            request.header("Authorization", "Basic " + Base64.encodeToString((task.username + ":" + task.password).getBytes(), Base64.NO_WRAP));

        downloads.removeById(task.id);
        downloads.add(new DownloadTask(task));
        executorService.execute(new DownloaderRunnable(task, request, tempFile));
    }

    private void resumeInternal(SavedDownloadsManager.SavedState state) {
        try {
            DownloadStartConfig config = DownloadStartConfig.createForSavedState(this, state);
            startDownload(config);
        } catch (DownloadStartConfig.CannotCreateStartConfigException | DownloaderUtils.InvalidPathException ex) {
            Bundle bundle = new Bundle();
            bundle.putSerializable("ex", ex);
            sendBroadcast(DownloaderUtils.ACTION_FAILED_RESUMING, bundle);
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
        else removeDownload(id);
    }

    private void restartDownload(int id) {
        DownloadTask task = downloads.find(id);
        if (task != null) startDownload(DownloadStartConfig.recreate(this, task));
    }

    private void getDownload(int id) {
        DownloadTask task = downloads.find(id);
        Bundle bundle = new Bundle();
        if (task != null) bundle.putSerializable("task", task);
        sendBroadcast(DownloaderUtils.ACTION_GET_DOWNLOAD, bundle);
    }

    static class DownloaderException extends Exception {
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
                case DownloaderUtils.GET_DOWNLOAD:
                    service.getDownload(msg.arg1);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private class DownloadTasks extends ArrayList<DownloadTask> implements Serializable {

        @Override
        public void add(int index, DownloadTask element) {
            synchronized (this) {
                if (!contains(element)) {
                    super.add(index, element);
                    notifyItemAdded(element);
                }
            }
        }

        @Override
        public boolean add(DownloadTask element) {
            synchronized (this) {
                if (!contains(element)) {
                    boolean a = super.add(element);
                    notifyItemAdded(element);
                    return a;
                } else {
                    return false;
                }
            }
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

            if (activeRunnables.size() == 1)
                wifiLock.release(); // activeRunnables still contains the runnable after that call
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
            synchronized (this) {
                DownloadTask a = super.remove(index);
                notifyItemRemoved(index);

                if (a.task.resumable)
                    savedDownloadsManager.removeState(a.task.id);

                return a;
            }
        }

        @Override
        public boolean remove(Object o) {
            synchronized (this) {
                int pos = indexOf(o);
                boolean a = super.remove(o);
                notifyItemRemoved(pos);

                if (o instanceof DownloadTask) {
                    DownloadTask task = (DownloadTask) o;
                    if (task.task.resumable)
                        savedDownloadsManager.removeState(task.task.id);
                }

                return a;
            }
        }

        @Nullable
        DownloadTask find(int id) {
            synchronized (this) {
                for (DownloadTask task : this)
                    if (task.task.id == id)
                        return task;

                return null;
            }
        }

        void removeById(int id) {
            synchronized (this) {
                ListIterator<DownloadTask> iterator = listIterator();
                while (iterator.hasNext())
                    if (iterator.next().task.id == id)
                        iterator.remove(); // Calls #remove(int)
            }
        }
    }

    private class DownloaderRunnable implements Runnable {
        private final Request.Builder request;
        private final File tempFile;
        private final DownloadStartConfig.Task singleTask;
        private final AtomicBoolean shouldStop = new AtomicBoolean(false);
        private final AtomicBoolean saveState = new AtomicBoolean(false);

        private DownloaderRunnable(DownloadStartConfig.Task task, Request.Builder request, File tempFile) {
            this.singleTask = task;
            this.request = request;
            this.tempFile = tempFile;
        }

        private void saveState() {
            savedDownloadsManager.saveState(singleTask.id, singleTask.url, tempFile, singleTask.destFile, singleTask.getProfileId());
        }

        private void pause() {
            shouldStop.set(true);
            saveState.set(true);
        }

        private void stop() {
            shouldStop.set(true);
            saveState.set(false);
        }

        @Override
        public void run() {
            DownloadTask task = downloads.find(singleTask.id);
            if (task == null) return; // What?

            task.status = DownloadTask.Status.STARTED;
            downloads.notifyItemChanged(task);

            try {
                long downloaded = 0;
                if (singleTask.resumable && tempFile.exists()) {
                    long toSkip = tempFile.length();
                    if (toSkip <= 0) {
                        removeState();

                        task.status = DownloadTask.Status.FAILED;
                        task.ex = new DownloaderException("File length is lower than 0: " + toSkip);
                        downloads.notifyItemChanged(task);
                        return;
                    }

                    request.header("Range", "bytes=" + toSkip + "-");
                    downloaded = toSkip;
                }

                try (Response resp = client.newCall(request.build()).execute()) {
                    if (singleTask.resumable) {
                        if (resp.code() != 206) {
                            removeState();

                            task.status = DownloadTask.Status.FAILED;
                            task.ex = new DownloaderException("Server doesn't support partial content.");
                            downloads.notifyItemChanged(task);
                            return;
                        }
                    } else {
                        if (resp.code() != 200) {
                            removeState();

                            task.status = DownloadTask.Status.FAILED;
                            task.ex = new DownloaderException(new StatusCodeException(resp));
                            downloads.notifyItemChanged(task);
                            return;
                        }
                    }

                    DownloaderService.this.activeRunnables.add(this);

                    ResponseBody body = resp.body();
                    if (body == null) throw new IOException("Empty body!");

                    task.length = body.contentLength() + downloaded;
                    downloads.notifyItemChanged(task);

                    try (InputStream in = body.byteStream();
                         FileOutputStream out = new FileOutputStream(tempFile, singleTask.resumable)) {
                        byte[] buffer = new byte[4096];

                        int count;
                        long lastUpdateCount = 0;
                        long lastUpdate = System.currentTimeMillis();
                        long period;
                        while (!shouldStop.get() && (count = in.read(buffer)) != -1) {
                            out.write(buffer, 0, count);
                            out.flush();

                            downloaded += count;
                            lastUpdateCount += count;
                            task.status = DownloadTask.Status.RUNNING;
                            task.downloaded = downloaded;

                            period = System.currentTimeMillis() - lastUpdate;
                            if (period >= 500) {
                                task.speed = (float) lastUpdateCount / ((float) period / 1000f);
                                downloads.notifyItemChanged(task);
                                lastUpdate = System.currentTimeMillis();
                                lastUpdateCount = 0;
                            }
                        }
                    }
                }

                if (shouldStop.get()) {
                    if (saveState.get()) {
                        saveState();

                        task.status = DownloadTask.Status.PAUSED;
                        downloads.notifyItemChanged(task);
                        DownloaderService.this.activeRunnables.remove(this);
                        return; // IMPORTANT: Won't delete the cache file and the state
                    } else {
                        downloads.removeById(singleTask.id);
                    }
                } else {
                    if (!tempFile.renameTo(singleTask.destFile)) {
                        removeState();

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

                removeState();
                DownloaderService.this.activeRunnables.remove(this);
            } catch (IOException ex) {
                task.status = DownloadTask.Status.FAILED;
                task.ex = new DownloaderException(ex);
                downloads.notifyItemChanged(task);
            }
        }

        private void removeState() {
            savedDownloadsManager.removeState(singleTask.id);
        }
    }
}
