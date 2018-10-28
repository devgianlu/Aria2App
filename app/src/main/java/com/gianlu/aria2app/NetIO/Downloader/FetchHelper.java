package com.gianlu.aria2app.NetIO.Downloader;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;

import com.gianlu.aria2app.NetIO.Aria2.AriaDirectory;
import com.gianlu.aria2app.NetIO.Aria2.AriaFile;
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithUpdate;
import com.gianlu.aria2app.PK;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.commonutils.Preferences.Prefs;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.EnqueueAction;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2core.Func;
import com.tonyodev.fetch2okhttp.OkHttpDownloader;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import okhttp3.HttpUrl;

public class FetchHelper {
    private static FetchHelper instance;
    private final ProfilesManager profilesManager;
    private final Fetch fetch;
    private final Handler handler;

    private FetchHelper(@NonNull Context context) {
        FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(context)
                .setDownloadConcurrentLimit(Prefs.getInt(PK.DD_MAX_SIMULTANEOUS_DOWNLOADS))
                .setHttpDownloader(new OkHttpDownloader(/* TODO: Select downloader type? */))
                .build();

        fetch = Fetch.Impl.getInstance(fetchConfiguration);
        profilesManager = ProfilesManager.get(context);
        handler = new Handler(Looper.getMainLooper());
    }

    public static void invalidate() { // TODO: Call when any preference changes
        if (instance != null) {
            instance.fetch.close();
            instance = null;
        }
    }

    @NonNull
    public static FetchHelper get(@NonNull Context context) {
        if (instance == null) instance = new FetchHelper(context);
        return instance;
    }

    @NonNull
    private static File getAndValidateDownloadPath() throws PreparationException {
        File path = new File(Prefs.getString(PK.DD_DOWNLOAD_PATH));
        if (!path.exists() && !path.mkdirs())
            throw new PreparationException("Path doesn't exists anc can't be created: " + path);

        if (!path.canWrite())
            throw new PreparationException("Cannot write to path: " + path);

        return path;
    }

    @NonNull
    private static Request createRequest(HttpUrl base, String dir, File downloadDir, AriaFile file) {
        return createRequest(file.getDownloadUrl(dir, base), new File(downloadDir, file.getRelativePath(dir)));
    }

    @NonNull
    private static Request createRequest(@NonNull HttpUrl url, @NonNull File dest) {
        Request request = new Request(url.toString(), dest.getAbsolutePath());
        request.setEnqueueAction(EnqueueAction.UPDATE_ACCORDINGLY); // TODO: Could be user settable
        return request;
    }

    public void updateDownloadCount(final FetchDownloadCountListener listener) {
        fetch.getDownloads(new Func<List<Download>>() {
            @Override
            @UiThread
            public void call(final @NotNull List<Download> result) {
                listener.onFetchDownloadCount(result.size());
            }
        });
    }

    private void callFailed(final StartListener listener, final Throwable ex) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                listener.onFailed(ex);
            }
        });
    }

    public void start(@NonNull MultiProfile profile, @NonNull DownloadWithUpdate download, @NonNull AriaFile file, @NonNull StartListener listener) {
        String ariaDir = download.update().dir;

        MultiProfile.DirectDownload dd = profile.getProfile(profilesManager).directDownload;
        if (dd == null) {
            callFailed(listener, new PreparationException("DirectDownload not enabled!"));
            return;
        }

        HttpUrl base = dd.getUrl();
        if (base == null) {
            callFailed(listener, new PreparationException("Invalid DirectDownload url: " + dd.address));
            return;
        }

        File downloadDir;
        try {
            downloadDir = getAndValidateDownloadPath();
        } catch (PreparationException ex) {
            callFailed(listener, ex);
            return;
        }

        startInternal(createRequest(file.getDownloadUrl(ariaDir, base), new File(downloadDir, file.getRelativePath(ariaDir))), listener);
    }

    public void start(@NonNull MultiProfile profile, @NonNull DownloadWithUpdate download, @NonNull AriaDirectory dir, @NonNull StartListener listener) {
        List<AriaFile> files = dir.allFiles();
        if (files.size() == 1) {
            start(profile, download, files.get(0), listener);
            return;
        }

        MultiProfile.DirectDownload dd = profile.getProfile(profilesManager).directDownload;
        if (dd == null) {
            callFailed(listener, new PreparationException("DirectDownload not enabled!"));
            return;
        }

        HttpUrl base = dd.getUrl();
        if (base == null) {
            callFailed(listener, new PreparationException("Invalid DirectDownload url: " + dd.address));
            return;
        }

        File downloadDir;
        try {
            downloadDir = getAndValidateDownloadPath();
        } catch (PreparationException ex) {
            callFailed(listener, ex);
            return;
        }

        int groupId = ThreadLocalRandom.current().nextInt();
        String ariaDir = download.update().dir;

        List<Request> requests = new ArrayList<>(files.size());
        for (AriaFile file : files) {
            Request request = createRequest(base, ariaDir, downloadDir, file);
            request.setGroupId(groupId);
            requests.add(request);
        }

        startInternal(requests, listener);
    }

    private void startInternal(@NonNull Request request, @NonNull final StartListener listener) {
        fetch.enqueue(request, new Func<Request>() {
            @Override
            @UiThread
            public void call(@NotNull Request result) {
                listener.onSuccess();
            }
        }, new Func<Error>() {
            @Override
            @UiThread
            public void call(@NotNull Error result) {
                listener.onFailed(result.getThrowable());
            }
        });
    }

    private void startInternal(@NonNull List<Request> requests, @NonNull final StartListener listener) {
        fetch.enqueue(requests, new Func<List<Request>>() {
            @Override
            @UiThread
            public void call(@NotNull List<Request> result) {
                listener.onSuccess();
            }
        }, new Func<Error>() {
            @Override
            @UiThread
            public void call(@NotNull Error result) {
                listener.onFailed(result.getThrowable());
            }
        });
    }

    public void addListener(FetchEventListener listener) {
        fetch.addListener(listener);
        reloadListener(listener);
    }

    public void removeListener(FetchEventListener listener) {
        fetch.removeListener(listener);
    }

    public void reloadListener(final FetchEventListener listener) {
        fetch.getDownloads(new Func<List<Download>>() {
            @Override
            @UiThread
            public void call(@NotNull List<Download> result) {
                listener.onDownloads(result);
            }
        });
    }

    public void resume(@NotNull FetchDownloadWrapper download) {
        fetch.resume(download.get().getId());
    }

    public void pause(@NotNull FetchDownloadWrapper download) {
        fetch.pause(download.get().getId());
    }

    public void remove(@NotNull FetchDownloadWrapper download) {
        fetch.remove(download.get().getId());
    }

    public void restart(@NotNull FetchDownloadWrapper download, @NonNull StartListener listener) {
        startInternal(createRequest(download.getUrl(), download.getFile()), listener);
    }

    @UiThread
    public interface FetchEventListener extends FetchListener {
        void onDownloads(List<Download> downloads);
    }

    @UiThread
    public interface FetchDownloadCountListener {
        void onFetchDownloadCount(int count);
    }

    @UiThread
    public interface StartListener {
        void onSuccess();

        void onFailed(Throwable ex);
    }

    public static class PreparationException extends Exception {
        private PreparationException(String msg) {
            super(msg);
        }
    }
}
