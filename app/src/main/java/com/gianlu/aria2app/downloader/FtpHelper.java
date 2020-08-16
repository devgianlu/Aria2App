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
import com.gianlu.aria2app.api.NetUtils;
import com.gianlu.aria2app.api.aria2.Aria2Helper;
import com.gianlu.aria2app.api.aria2.AriaDirectory;
import com.gianlu.aria2app.api.aria2.AriaFile;
import com.gianlu.aria2app.api.aria2.OptionsMap;
import com.gianlu.aria2app.profiles.MultiProfile;
import com.gianlu.commonutils.preferences.Prefs;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class FtpHelper extends DirectDownloadHelper {
    private static final String TAG = FtpHelper.class.getSimpleName();
    private final MultiProfile.DirectDownload.Ftp dd;
    private final ContentResolver contentResolver;
    private final ExecutorService executorService;
    private final AtomicInteger counter = new AtomicInteger(0);
    private final Map<Integer, DownloadRunnable> downloads = new ArrayMap<>(5);

    FtpHelper(@NonNull Context context, @NonNull MultiProfile.UserProfile profile, @NonNull MultiProfile.DirectDownload.Ftp dd) throws Aria2Helper.InitializingException {
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
        File remote = new File(dd.path, file.getRelativePath(global));

        int id = counter.getAndIncrement();
        DownloadRunnable runnable = new DownloadRunnable(id, dest, remote.getAbsolutePath());
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
    private DownloadRunnable unwrap(@NonNull DdDownload wrap) {
        DownloadRunnable download = wrap.unwrapFtp();
        if (download == null || !downloads.containsKey(download.id)) return null;
        else return download;
    }

    @Override
    public void resume(@NonNull DdDownload wrap) {
        DownloadRunnable download = unwrap(wrap);
        if (download != null) download.resume();
    }

    @Override
    public void pause(@NonNull DdDownload wrap) {
        DownloadRunnable download = unwrap(wrap);
        if (download != null) download.pause();
    }

    @Override
    public void restart(@NonNull DdDownload wrap, @NonNull StartListener listener) {
        // TODO: Restart
    }

    @Override
    public void remove(@NonNull DdDownload wrap) {
        DownloadRunnable download = unwrap(wrap);
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

    public static class FtpException extends Exception {
        public final int replyCode;

        FtpException(int replyCode) {
            super("Code: " + replyCode);
            this.replyCode = replyCode;
        }
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

            FTPClient client = null;
            try {
                if (dd.serverSsl) {
                    FTPSClient ftps = new FTPSClient();
                    if (!dd.hostnameVerifier) ftps.setHostnameVerifier((s, sslSession) -> true);
                    if (dd.certificate != null) NetUtils.setSslSocketFactory(ftps, dd.certificate);
                    client = ftps;
                } else {
                    client = new FTPClient();
                }

                client.connect(dd.hostname, dd.port);
                int reply = client.getReplyCode();
                if (!FTPReply.isPositiveCompletion(reply))
                    throw new FtpException(reply);

                if (!client.login(dd.username, dd.password)) {
                    reply = client.getReplyCode();
                    try {
                        client.logout();
                    } catch (IOException ignored) {
                    }

                    throw new FtpException(reply);
                }

                if (!client.setFileType(FTPClient.BINARY_FILE_TYPE))
                    throw new FtpException(client.getReplyCode());

                client.enterLocalPassiveMode();

                String sizeStr = client.getSize(remotePath);
                try {
                    length = Long.parseLong(sizeStr);
                } catch (NumberFormatException ex) {
                    throw new NumberFormatException(sizeStr + " -> " + ex.getMessage());
                }

                try (OutputStream out = contentResolver.openOutputStream(file.getUri())) {
                    if (out == null) throw new IOException("Couldn't open output file.");
                    try (InputStream in = client.retrieveFileStream(remotePath)) {
                        downloaded = 0;
                        long lastTime = System.currentTimeMillis();

                        byte[] buffer = new byte[512 * 1024];
                        int read;
                        while (!shouldStop && (read = in.read(buffer)) > 0) {
                            out.write(buffer, 0, read);
                            downloaded += read;


                            float diff = ((float) (System.currentTimeMillis() - lastTime)) / 1000;
                            lastTime = System.currentTimeMillis();

                            long speed = (long) (read / diff);
                            long eta = (long) (((float) (length - downloaded) / (float) speed) * 1000);
                            DdDownload wrap = DdDownload.wrap(DownloadRunnable.this);
                            forEachListener(listener -> listener.onProgress(wrap, eta, speed));
                        }

                        if (shouldStop) {
                            status = DdDownload.Status.CANCELLED;
                            callUpdated(this);
                            return;
                        }
                    }
                }

                status = DdDownload.Status.COMPLETED;
                callUpdated(this);

                try {
                    client.logout();
                } catch (IOException ignored) {
                }
            } catch (IOException | FtpException | NumberFormatException | GeneralSecurityException ex) {
                status = DdDownload.Status.FAILED;
                callUpdated(this);

                Log.e(TAG, String.format("Download error, id: %d, url: %s", id, getUrl()), ex);
            } finally {
                try {
                    if (client != null) client.disconnect();
                } catch (IOException ignored) {
                }

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
