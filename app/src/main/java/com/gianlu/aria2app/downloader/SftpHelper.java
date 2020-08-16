package com.gianlu.aria2app.downloader;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.gianlu.aria2app.PK;
import com.gianlu.aria2app.api.AbstractClient;
import com.gianlu.aria2app.api.AriaRequests;
import com.gianlu.aria2app.api.aria2.Aria2Helper;
import com.gianlu.aria2app.api.aria2.AriaDirectory;
import com.gianlu.aria2app.api.aria2.AriaFile;
import com.gianlu.aria2app.api.aria2.OptionsMap;
import com.gianlu.aria2app.profiles.MultiProfile;
import com.gianlu.commonutils.preferences.Prefs;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class SftpHelper extends DirectDownloadHelper {
    private static final String TAG = SftpHelper.class.getSimpleName();
    private final MultiProfile.DirectDownload.Sftp dd;
    private final JSch jSch = new JSch();
    private final ContentResolver contentResolver;
    private final ExecutorService executorService;
    private final AtomicInteger counter = new AtomicInteger(0);
    private final Map<Integer, DownloadRunnable> downloads = new ArrayMap<>(5);

    public SftpHelper(@NonNull Context context, @NonNull MultiProfile.UserProfile profile, @NonNull MultiProfile.DirectDownload.Sftp dd) throws Aria2Helper.InitializingException {
        super(context, profile);
        this.dd = dd;
        this.contentResolver = context.getContentResolver();
        this.executorService = Executors.newFixedThreadPool(Prefs.getInt(PK.DD_MAX_SIMULTANEOUS_DOWNLOADS));
    }

    @Override
    public void start(@NonNull Context context, @NonNull AriaFile file, @NonNull StartListener listener) {
        DocumentFile ddDir;
        try {
            ddDir = getAndValidateDownloadPath(context);
        } catch (FetchHelper.PreparationException ex) {
            callFailed(listener, ex);
            return;
        }

        aria2.request(AriaRequests.getGlobalOptions(), new AbstractClient.OnResult<OptionsMap>() {
            @Override
            public void onResult(@NonNull OptionsMap result) {
                try {
                    startInternal(result, ddDir, file);
                    handler.post(listener::onSuccess);
                } catch (PreparationException ex) {
                    callFailed(listener, ex);
                }
            }

            @Override
            public void onException(@NonNull Exception ex) {
                listener.onFailed(ex);
            }
        });
    }

    private void startInternal(OptionsMap global, DocumentFile ddDir, @NotNull AriaFile file) throws PreparationException {
        DocumentFile dest = createDestFile(global, ddDir, file);

        int id = counter.getAndIncrement();
        DownloadRunnable runnable = new DownloadRunnable(id, dest, file.getAbsolutePath());
        downloads.put(id, runnable);
        executorService.execute(runnable);

        DdDownload wrap = DdDownload.wrap(runnable);
        forEachListener(l -> l.onAdded(wrap));
    }

    @Override
    public void start(@NonNull Context context, @NonNull AriaDirectory dir, @NonNull StartListener listener) {
        DocumentFile ddDir;
        try {
            ddDir = getAndValidateDownloadPath(context);
        } catch (FetchHelper.PreparationException ex) {
            callFailed(listener, ex);
            return;
        }

        aria2.request(AriaRequests.getGlobalOptions(), new AbstractClient.OnResult<OptionsMap>() {
            @Override
            public void onResult(@NonNull OptionsMap result) {
                for (AriaFile file : dir.allFiles()) {
                    try {
                        startInternal(result, ddDir, file);
                    } catch (PreparationException ex) {
                        Log.e(TAG, "Failed preparing download: " + file.path, ex);
                    }
                }

                handler.post(listener::onSuccess);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                listener.onFailed(ex);
            }
        });
    }

    @Nullable
    private FtpHelper.DownloadRunnable unwrap(@NonNull DdDownload wrap) {
        FtpHelper.DownloadRunnable download = wrap.unwrapFtp();
        if (download == null || !downloads.containsKey(download.id)) return null;
        else return download;
    }

    @Override
    public void resume(@NonNull DdDownload wrap) {
        FtpHelper.DownloadRunnable download = unwrap(wrap);
        if (download != null) download.resume();
    }

    @Override
    public void pause(@NonNull DdDownload wrap) {
        FtpHelper.DownloadRunnable download = unwrap(wrap);
        if (download != null) download.pause();
    }

    @Override
    public void restart(@NonNull DdDownload wrap, @NonNull StartListener listener) {
        // TODO: Restart
    }

    @Override
    public void remove(@NonNull DdDownload wrap) {
        FtpHelper.DownloadRunnable download = unwrap(wrap);
        if (download != null) download.remove();
    }

    @Override
    public void reloadListener(@NonNull Listener listener) {
        List<DdDownload> wrapped = new ArrayList<>(downloads.size());
        for (DownloadRunnable download : new ArrayList<>(downloads.values()))
            wrapped.add(DdDownload.wrap(download));

        listener.onDownloads(wrapped);
    }

    @Override
    public void close() {
        for (DownloadRunnable download : downloads.values())
            download.remove();

        executorService.shutdownNow();
    }

    private void callUpdated(@NonNull DownloadRunnable download) {
        DdDownload wrap = DdDownload.wrap(download);
        forEachListener(listener -> listener.onUpdated(wrap));
    }

    private void callRemoved(@NonNull DownloadRunnable download) {
        DdDownload wrap = DdDownload.wrap(download);
        forEachListener(listener -> listener.onRemoved(wrap));
    }

    class DownloadRunnable implements Runnable {
        final int id;
        final DocumentFile file;
        final String remotePath;
        volatile long length = -1;
        volatile long downloaded = -1;
        volatile DdDownload.Status status;
        volatile boolean shouldStop = false;

        DownloadRunnable(int id, @NonNull DocumentFile file, @NonNull String remotePath) {
            this.id = id;
            this.file = file;
            this.remotePath = remotePath;
            this.status = DdDownload.Status.QUEUED;
        }

        @Override
        public void run() {
            status = DdDownload.Status.DOWNLOADING;
            callUpdated(this);

            Session session = null;
            try {
                session = jSch.getSession(dd.username, dd.hostname, dd.port);
                session.setUserInfo(null /* FIXME */);
                session.connect();

                ChannelSftp ch = (ChannelSftp) session.openChannel("sftp");
                ch.connect();

                try (OutputStream out = contentResolver.openOutputStream(file.getUri())) {
                    ch.get(remotePath, out, new SftpProgressMonitor() {
                        long lastTime;

                        @Override
                        public void init(int op, String src, String dest, long max) {
                            downloaded = 0;
                            lastTime = System.currentTimeMillis();
                        }

                        @Override
                        public boolean count(long count) {
                            float diff = ((float) (System.currentTimeMillis() - lastTime)) / 1000;
                            lastTime = System.currentTimeMillis();

                            long speed = (long) (count / diff);
                            long eta = (long) (((float) (length - downloaded) / (float) speed) * 1000);
                            DdDownload wrap = DdDownload.wrap(DownloadRunnable.this);
                            forEachListener(listener -> listener.onProgress(wrap, eta, speed));

                            return !shouldStop;
                        }

                        @Override
                        public void end() {
                        }
                    });
                }

                status = DdDownload.Status.COMPLETED;
                callUpdated(this);
            } catch (JSchException | SftpException | IOException ex) {
                status = DdDownload.Status.FAILED;
                callUpdated(this);

                Log.e(TAG, String.format("Download error, id: %d, url: %s", id, getUrl()), ex);
            } finally {
                if (session != null) session.disconnect();

                downloads.remove(id);
                callRemoved(this);
            }
        }

        /**
         * Gets a simple representation of the resource being downloaded.
         *
         * @return An FTP URL
         */
        @SuppressLint("DefaultLocale")
        @NonNull
        public String getUrl() {
            return String.format("ftp://%s:%d%s", dd.hostname, dd.port, remotePath);
        }

        public int getProgress() {
            if (length == -1 || downloaded == -1) return -1;
            else return (int) (((float) downloaded / (float) length) * 100);
        }

        void resume() {
            // TODO: Resume
        }

        void pause() {
            // TODO: Pause
        }

        void remove() {
            shouldStop = true;
        }
    }
}
