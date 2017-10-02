package com.gianlu.aria2app.Downloader;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URI;
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
    private Messenger messenger;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (messenger == null) messenger = new Messenger(new ServiceHandler(this));
        return messenger.getBinder();
    }

    private void startDownload(DownloadStartConfig config) {
        if (config.tasks.isEmpty()) return;

        if (config.tasks.size() == 1) {
            executorService.execute(DownloaderRunnable.start(config.tasks.get(0)));
        } else {

        }
    }

    private static class DownloaderRunnable implements Runnable {
        private final URI uri;
        private final File tempFile;
        private final File destFile;

        private DownloaderRunnable(URI uri, File tempFile, File destFile) {
            this.uri = uri;
            this.tempFile = tempFile;
            this.destFile = destFile;
        }

        public static DownloaderRunnable resume() {
            throw new UnsupportedOperationException(); // TODO
        }

        @NonNull
        public static DownloaderRunnable start(DownloadStartConfig.Task task) {
            File tempFile = new File(task.getCacheDir(), String.valueOf(task.id));
            return new DownloaderRunnable(task.uri, tempFile, task.destFile);
        }

        @Override
        public void run() {
            HttpGet get = new HttpGet(uri);

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpResponse resp = client.execute(get);

                StatusLine sl = resp.getStatusLine();
                if (sl.getStatusCode() != HttpStatus.SC_OK) {
                    throw new RuntimeException("FUCK!!"); // FIXME
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

                EntityUtils.consume(entity);

                if (!tempFile.renameTo(destFile)) {
                    throw new RuntimeException("FUCK!!"); // FIXME
                }

                if (!tempFile.delete()) tempFile.deleteOnExit();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static class ServiceHandler extends Handler {
        private final WeakReference<DownloaderService> service;

        ServiceHandler(DownloaderService service) {
            this.service = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DownloaderUtils.START_DOWNLOAD:
                    DownloaderService service = this.service.get();
                    if (service != null)
                        service.startDownload((DownloadStartConfig) msg.obj);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
