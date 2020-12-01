package com.gianlu.aria2app.downloader;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;

import androidx.annotation.NonNull;
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
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.EnqueueAction;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2core.DownloadBlock;
import com.tonyodev.fetch2okhttp.OkHttpDownloader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public final class FetchHelper extends DirectDownloadHelper implements FetchListener {
    private final Fetch fetch;
    private final MultiProfile.DirectDownload.Web dd;

    FetchHelper(@NonNull Context context, @NonNull MultiProfile.UserProfile profile, @NonNull MultiProfile.DirectDownload.Web dd) throws GeneralSecurityException, IOException, Aria2Helper.InitializingException {
        super(context, profile);
        this.dd = dd;

        OkHttpClient.Builder client = new OkHttpClient.Builder();
        if (!dd.hostnameVerifier) client.hostnameVerifier((s, sslSession) -> true);
        if (dd.certificate != null) NetUtils.setSslSocketFactory(client, dd.certificate);
        if (dd.auth) client.addInterceptor(new BasicAuthInterceptor(dd.username, dd.password));

        FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(context)
                .setDownloadConcurrentLimit(Prefs.getInt(PK.DD_MAX_SIMULTANEOUS_DOWNLOADS))
                .setProgressReportingInterval(REPORTING_INTERVAL)
                .enableAutoStart(Prefs.getBoolean(PK.DD_RESUME))
                .setHttpDownloader(new OkHttpDownloader(client.build()))
                .build();

        fetch = Fetch.Impl.getInstance(fetchConfiguration);
        fetch.addListener(this);
    }

    @NonNull
    private static Request createFetchRequest(HttpUrl base, OptionsMap global, DocumentFile ddDir, @NotNull AriaFile file) throws PreparationException {
        DocumentFile dest = createDestFile(global, ddDir, new AriaRemoteFile(file));
        return createFetchRequest(file.getDownloadUrl(global, base), dest.getUri());
    }

    @NonNull
    private static Request createFetchRequest(@NonNull HttpUrl url, @NonNull Uri dest) {
        Request request = new Request(url.toString(), dest);
        request.setEnqueueAction(EnqueueAction.UPDATE_ACCORDINGLY);
        return request;
    }

    //region Download
    @Override
    public void start(@NonNull Context context, @NonNull AriaFile file, @NonNull StartListener listener) {
        HttpUrl base = dd.getUrl();
        if (base == null) {
            callFailed(listener, new PreparationException("Invalid DirectDownload url: " + dd.address));
            return;
        }

        DocumentFile ddDir;
        try {
            ddDir = getAndValidateDownloadPath(context);
        } catch (PreparationException ex) {
            callFailed(listener, ex);
            return;
        }

        aria2.request(AriaRequests.getGlobalOptions(), new AbstractClient.OnResult<OptionsMap>() {
            @Override
            public void onResult(@NonNull OptionsMap result) {
                try {
                    startInternal(createFetchRequest(base, result, ddDir, file), listener);
                } catch (PreparationException ex) {
                    listener.onFailed(ex);
                }
            }

            @Override
            public void onException(@NonNull Exception ex) {
                listener.onFailed(ex);
            }
        });
    }

    @Override
    public void start(@NonNull Context context, @NonNull AriaDirectory dir, @NonNull StartListener listener) {
        List<AriaFile> files = dir.allFiles();
        if (files.size() == 1) {
            start(context, files.get(0), listener);
            return;
        }

        HttpUrl base = dd.getUrl();
        if (base == null) {
            callFailed(listener, new PreparationException("Invalid DirectDownload url: " + dd.address));
            return;
        }

        DocumentFile downloadDir;
        try {
            downloadDir = getAndValidateDownloadPath(context);
        } catch (PreparationException ex) {
            callFailed(listener, ex);
            return;
        }

        aria2.request(AriaRequests.getGlobalOptions(), new AbstractClient.OnResult<OptionsMap>() {
            @Override
            public void onResult(@NonNull OptionsMap result) {
                int groupId = ThreadLocalRandom.current().nextInt();

                try {
                    List<Request> requests = new ArrayList<>(files.size());
                    for (AriaFile file : files) {
                        Request request = createFetchRequest(base, result, downloadDir, file);
                        request.setGroupId(groupId);
                        requests.add(request);
                    }

                    startInternal(requests, listener);
                } catch (PreparationException ex) {
                    listener.onFailed(ex);
                }
            }

            @Override
            public void onException(@NonNull Exception ex) {
                listener.onFailed(ex);
            }
        });
    }
    //endregion

    //region Internal start
    private void startInternal(@NonNull Request request, @NonNull StartListener listener) {
        if (fetch.isClosed())
            return;

        fetch.enqueue(request, result -> listener.onSuccess(), result -> {
            Throwable t = result.getThrowable();
            if (t == null)
                listener.onFailed(new IllegalStateException("Exception not specified!"));
            else
                listener.onFailed(t);
        });
    }

    private void startInternal(@NonNull List<Request> requests, @NonNull StartListener listener) {
        if (!fetch.isClosed()) fetch.enqueue(requests, result -> listener.onSuccess());
    }
    //endregion

    @Override
    public void reloadListener(@NonNull Listener listener) {
        if (!fetch.isClosed()) fetch.getDownloads(result -> {
            List<DdDownload> wrap = new ArrayList<>(result.size());
            for (Download d : result) wrap.add(DdDownload.wrap(d));
            listener.onDownloads(wrap);
        });
    }

    //region Download actions

    @Override
    public void resume(@NotNull DdDownload download) {
        Download unwrap = download.unwrapFetch();
        if (unwrap != null && !fetch.isClosed()) fetch.resume(unwrap.getId());
    }

    @Override
    public void pause(@NotNull DdDownload download) {
        Download unwrap = download.unwrapFetch();
        if (unwrap != null && !fetch.isClosed()) fetch.pause(unwrap.getId());
    }

    @Override
    public void remove(@NotNull DdDownload download) {
        Download unwrap = download.unwrapFetch();
        if (unwrap != null && !fetch.isClosed()) fetch.remove(unwrap.getId());
    }

    @Override
    public void restart(@NotNull DdDownload download, @NonNull StartListener listener) {
        startInternal(createFetchRequest(download.getUrl(), download.getUri()), listener);
    }

    //endregion

    //region Download events

    @Override
    public void onAdded(@NotNull Download download) {
        DdDownload wrap = DdDownload.wrap(download);
        forEachListener(listener -> listener.onAdded(wrap));
    }

    private void onUpdate(@NonNull Download download) {
        DdDownload wrap = DdDownload.wrap(download);
        forEachListener(listener -> listener.onUpdated(wrap));
    }

    @Override
    public void onProgress(@NotNull Download download, long eta, long speed) {
        DdDownload wrap = DdDownload.wrap(download);
        wrap.progress(eta, speed);
        forEachListener(listener -> listener.onProgress(wrap));
    }

    @Override
    public void onRemoved(@NotNull Download download) {
        DdDownload wrap = DdDownload.wrap(download);
        forEachListener(listener -> listener.onRemoved(wrap));
    }

    @Override
    public void onCancelled(@NotNull Download download) {
        onUpdate(download);
    }

    @Override
    public void onCompleted(@NotNull Download download) {
        onUpdate(download);
    }

    @Override
    public void onDeleted(@NotNull Download download) {
        onRemoved(download);
    }

    @Override
    public void onDownloadBlockUpdated(@NotNull Download download, @NotNull DownloadBlock downloadBlock, int i) {
    }

    @Override
    public void onError(@NotNull Download download, @NotNull Error error, @Nullable Throwable throwable) {
        onUpdate(download);
    }

    @Override
    public void onPaused(@NotNull Download download) {
        onUpdate(download);
    }

    @Override
    public void onQueued(@NotNull Download download, boolean b) {
        onUpdate(download);
    }

    @Override
    public void onResumed(@NotNull Download download) {
        onUpdate(download);
    }

    @Override
    public void onStarted(@NotNull Download download, @NotNull List<? extends DownloadBlock> list, int i) {
        onUpdate(download);
    }

    @Override
    public void onWaitingNetwork(@NotNull Download download) {
        onUpdate(download);
    }

    //endregion

    @Override
    public void close() {
        fetch.close();
    }

    private static class BasicAuthInterceptor implements Interceptor {
        private final String username;
        private final String password;

        BasicAuthInterceptor(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @NonNull
        @Override
        public Response intercept(Chain chain) throws IOException {
            okhttp3.Request request = chain.request();
            return chain.proceed(request.newBuilder()
                    .addHeader("Authorization", "Basic " + Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP))
                    .build());
        }
    }
}
