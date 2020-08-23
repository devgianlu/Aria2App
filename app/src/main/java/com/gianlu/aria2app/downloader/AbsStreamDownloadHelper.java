package com.gianlu.aria2app.downloader;

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

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbsStreamDownloadHelper extends DirectDownloadHelper {
    private static final String TAG = AbsStreamDownloadHelper.class.getSimpleName();
    private final ContentResolver contentResolver;
    private final ExecutorService executorService;
    private final AtomicInteger counter = new AtomicInteger(0);
    private final Map<Integer, DownloadRunnable> downloads = new ArrayMap<>(5);

    public AbsStreamDownloadHelper(@NonNull Context context, @NonNull MultiProfile.UserProfile profile) throws Aria2Helper.InitializingException {
        super(context, profile);
        this.contentResolver = context.getContentResolver();
        this.executorService = Executors.newFixedThreadPool(Prefs.getInt(PK.DD_MAX_SIMULTANEOUS_DOWNLOADS));
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
        DownloadRunnable runnable = makeRunnableFor(id, dest, global, file);
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
        DownloadRunnable download = wrap.unwrapStream();
        if (download == null || !downloads.containsKey(download.id)) return null;
        else return download;
    }

    @Override
    public final void resume(@NonNull DdDownload wrap) {
        DownloadRunnable download = unwrap(wrap);
        if (download != null) download.resume();
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
    protected abstract DownloadRunnable makeRunnableFor(int id, @NonNull DocumentFile file, @NonNull OptionsMap globalOptions, @NonNull AriaFile remoteFile);

    protected abstract class DownloadRunnable implements Runnable {
        final int id;
        final DocumentFile file;
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

        protected void updateProgress(long eta, long speed) {
            DdDownload wrap = DdDownload.wrap(this);
            wrap.progress(eta, speed);
            forEachListener(listener -> listener.onProgress(wrap));
        }

        @NonNull
        protected final OutputStream openDestination() throws IOException {
            OutputStream out = contentResolver.openOutputStream(file.getUri());
            if (out == null) throw new IOException("Failed opening file: " + file);
            else return out;
        }

        @Override
        public final void run() {
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
                    downloads.remove(id);
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

        final void resume() {
            // TODO: Resume
        }

        final void pause() {
            removed = false;
            paused = true;
            shouldStop = true;
        }

        final void remove() {
            if (terminated) {
                downloads.remove(id);
                callRemoved(this);
            } else {
                removed = true;
                paused = false;
                shouldStop = true;
            }
        }
    }
}
