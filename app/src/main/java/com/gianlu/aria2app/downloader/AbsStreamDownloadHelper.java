package com.gianlu.aria2app.downloader;

import android.content.ContentResolver;
import android.content.Context;
import android.util.ArrayMap;
import android.util.Log;
import android.webkit.MimeTypeMap;

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
import com.gianlu.commonutils.misc.NamedThreadFactory;
import com.gianlu.commonutils.preferences.Prefs;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbsStreamDownloadHelper extends DirectDownloadHelper {
    private static final String TAG = AbsStreamDownloadHelper.class.getSimpleName();
    private final ContentResolver contentResolver;
    private final ExecutorService executorService;
    private final Map<Integer, DownloadRunnable> downloads = new ArrayMap<>(5);
    private final DdDatabase db;

    public AbsStreamDownloadHelper(@NonNull Context context, @NonNull MultiProfile.UserProfile profile) throws Aria2Helper.InitializingException {
        super(context, profile);
        this.db = new DdDatabase(context);
        this.contentResolver = context.getContentResolver();
        this.executorService = Executors.newFixedThreadPool(Prefs.getInt(PK.DD_MAX_SIMULTANEOUS_DOWNLOADS), new NamedThreadFactory("dd-runnable-"));
    }

    @NonNull
    private DdDatabase.Type getDownloaderType() {
        if (this instanceof FtpHelper) return DdDatabase.Type.FTP;
        else if (this instanceof SftpHelper) return DdDatabase.Type.SFTP;
        else if (this instanceof SambaHelper) return DdDatabase.Type.SMB;
        else throw new IllegalStateException(String.valueOf(this));
    }

    protected void loadDb(@NonNull Context context) {
        boolean autoStart = Prefs.getBoolean(PK.DD_RESUME);

        DocumentFile ddDir;
        try {
            ddDir = getAndValidateDownloadPath(context);
        } catch (FetchHelper.PreparationException ex) {
            Log.e(TAG, "Failed getting download path when loading stored downloads.", ex);
            return;
        }

        aria2.request(AriaRequests.getGlobalOptions(), new AbstractClient.OnResult<OptionsMap>() {
            @Override
            public void onResult(@NonNull OptionsMap result) {
                for (DdDatabase.Download download : db.getDownloads(getDownloaderType())) {
                    try {
                        startInternal(result, ddDir, new DbRemoteFile(download.path), !autoStart);
                    } catch (PreparationException ex) {
                        Log.e(TAG, "Failed preparing stored download.", ex);
                    }
                }
            }

            @Override
            public void onException(@NonNull Exception ex) {
                Log.e(TAG, "Failed getting global options when loading stored downloads.", ex);
            }
        });
    }

    @Override
    public final void start(@NonNull Context context, @NonNull AriaFile file, @NonNull StartListener listener) {
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
                    startInternal(result, ddDir, new AriaRemoteFile(file), false);
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

    private void startInternal(OptionsMap global, DocumentFile ddDir, @NotNull RemoteFile file, boolean paused) throws PreparationException {
        DocumentFile dest = createDestFile(global, ddDir, file);

        int id = db.addDownload(getDownloaderType(), file.getAbsolutePath());
        if (id == -1) throw new PreparationException("Failed adding download to database.");

        DownloadRunnable runnable = makeRunnableFor(id, dest, global, file);
        if (paused) runnable.pause();

        downloads.put(id, runnable);
        executorService.execute(runnable);

        DdDownload wrap = DdDownload.wrap(runnable);
        forEachListener(l -> l.onAdded(wrap));
    }

    @Override
    public final void start(@NonNull Context context, @NonNull AriaDirectory dir, @NonNull StartListener listener) {
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
                        startInternal(result, ddDir, new AriaRemoteFile(file), false);
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
        DownloadRunnable download = wrap.unwrapStream();
        if (download == null || !downloads.containsKey(download.id)) return null;
        else return download;
    }

    @Override
    public final void resume(@NonNull DdDownload wrap) {
        DownloadRunnable download = unwrap(wrap);
        if (download == null) return;

        if (download.resume()) return;

        DownloadRunnable runnable = makeRunnableFor(download);
        downloads.put(download.id, runnable);
        executorService.execute(runnable);
    }

    @Override
    public final void pause(@NonNull DdDownload wrap) {
        DownloadRunnable download = unwrap(wrap);
        if (download != null) download.pause();
    }

    @Override
    public final void restart(@NonNull DdDownload wrap, @NonNull StartListener listener) {
        // TODO: Restart
    }

    @Override
    public final void remove(@NonNull DdDownload wrap) {
        DownloadRunnable download = unwrap(wrap);
        if (download != null) download.remove();
    }

    @Override
    public final void reloadListener(@NonNull Listener listener) {
        List<DdDownload> wrapped = new ArrayList<>(downloads.size());
        for (DownloadRunnable download : new ArrayList<>(downloads.values()))
            wrapped.add(DdDownload.wrap(download));

        listener.onDownloads(wrapped);
    }

    @Override
    public final void close() {
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

    @NonNull
    protected abstract DownloadRunnable makeRunnableFor(int id, @NonNull DocumentFile file, @NonNull OptionsMap globalOptions, @NonNull RemoteFile remoteFile);

    @NonNull
    protected abstract DownloadRunnable makeRunnableFor(@NonNull DownloadRunnable old);

    private void removeDownload(int id) {
        downloads.remove(id);
        db.removeDownload(id);
    }

    public static class DbRemoteFile implements RemoteFile {
        private final String path;
        private String name;
        private String mime;

        DbRemoteFile(@NonNull String path) {
            this.path = path;
        }

        @Nullable
        @Override
        public String getMimeType() {
            if (mime == null) {
                int dot = path.lastIndexOf('.');
                if (dot >= 0)
                    mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(path.substring(dot + 1));
            }

            return mime;
        }

        @NonNull
        @Override
        public String getName() {
            if (name == null) {
                int last = path.lastIndexOf(AriaDirectory.SEPARATOR);
                name = path.substring(last + 1);
            }

            return name;
        }

        @NonNull
        @Override
        public String getRelativePath(@NonNull OptionsMap global) {
            OptionsMap.OptionValue dir = global.get("dir");
            String dirStr = dir == null ? null : dir.string();
            if (dirStr == null) dirStr = "";
            return path.substring(dirStr.length() + 1);
        }

        @NonNull
        @Override
        public String getAbsolutePath() {
            return path;
        }
    }

    protected abstract class DownloadRunnable implements Runnable {
        final int id;
        final DocumentFile file;
        private final Object pauseLock = new Object();
        volatile long length = -1;
        volatile long downloaded = -1;
        volatile DdDownload.Status status;
        volatile boolean shouldStop = false;
        private volatile boolean removed = false;
        private volatile boolean paused = false;
        private volatile boolean terminated = false;

        public DownloadRunnable(int id, @NonNull DocumentFile file) {
            this.id = id;
            this.file = file;

            this.status = DdDownload.Status.QUEUED;
        }

        protected void updateStatus(@NonNull DdDownload.Status status) {
            this.status = status;
            callUpdated(this);
        }

        protected boolean updateProgress(long lastTime, long lastDownloaded) {
            long diff;
            if ((diff = System.currentTimeMillis() - lastTime) >= REPORTING_INTERVAL) {
                long speed = (lastDownloaded / diff) * 1000;
                long eta = (long) (((float) (length - downloaded) / (float) speed) * 1000);

                DdDownload wrap = DdDownload.wrap(this);
                wrap.progress(eta, speed);
                forEachListener(listener -> listener.onProgress(wrap));
                return true;
            }

            return false;
        }

        @NonNull
        protected final OutputStream openDestination() throws IOException {
            OutputStream out = contentResolver.openOutputStream(file.getUri(), "wa");
            if (out == null) throw new IOException("Failed opening file: " + file);
            else return out;
        }

        @Override
        public final void run() {
            if (file.exists())
                downloaded = file.length();

            if (paused) {
                shouldStop = false;
                updateStatus(DdDownload.Status.PAUSED);

                synchronized (pauseLock) {
                    try {
                        pauseLock.wait();
                    } catch (InterruptedException ex) {
                        terminated = true;
                        return;
                    }
                }
            }

            updateStatus(DdDownload.Status.DOWNLOADING);

            boolean result = runInternal();
            if (!result) {
                updateStatus(DdDownload.Status.FAILED);
                return;
            }

            if (shouldStop) {
                if (paused) {
                    updateStatus(DdDownload.Status.PAUSED);
                    Log.d(TAG, String.format("Download paused, id: %d, url: %s", id, getUrl()));
                } else if (removed) {
                    removeDownload(id);
                    callRemoved(this);
                    Log.d(TAG, String.format("Download removed, id: %d, url: %s", id, getUrl()));
                } else {
                    throw new IllegalStateException();
                }
            } else {
                updateStatus(DdDownload.Status.COMPLETED);
                Log.d(TAG, String.format("Download completed, id: %d, url: %s", id, getUrl()));
            }

            terminated = true;
        }

        /**
         * @return Whether the termination was graceful. If {@code false} then status is set to {@link com.gianlu.aria2app.downloader.DdDownload.Status#FAILED}.
         */
        protected abstract boolean runInternal();

        /**
         * Gets a simple representation of the resource being downloaded.
         */
        @NonNull
        public abstract String getUrl();

        public final int getProgress() {
            if (length == -1 || downloaded == -1) return -1;
            else return (int) (((float) downloaded / (float) length) * 100);
        }

        final boolean resume() {
            if (paused && !terminated) {
                paused = false;
                removed = false;
                shouldStop = false;

                synchronized (pauseLock) {
                    pauseLock.notifyAll();
                }

                return true;
            }

            return false;
        }

        final void pause() {
            removed = false;
            paused = true;
            shouldStop = true;
        }

        final void remove() {
            if (terminated) {
                removeDownload(id);
                callRemoved(this);
            } else {
                removed = true;
                paused = false;
                shouldStop = true;
            }
        }
    }
}
