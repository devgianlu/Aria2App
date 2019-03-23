package com.gianlu.aria2app.NetIO.Downloader;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.AriaDirectory;
import com.gianlu.aria2app.NetIO.Aria2.AriaFile;
import com.gianlu.aria2app.NetIO.Aria2.OptionsMap;
import com.gianlu.aria2app.NetIO.AriaRequests;
import com.gianlu.aria2app.NetIO.NetUtils;
import com.gianlu.aria2app.PK;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.commonutils.Preferences.Prefs;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.EnqueueAction;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2core.Downloader;
import com.tonyodev.fetch2core.OutputResourceWrapper;
import com.tonyodev.fetch2core.StorageResolver;
import com.tonyodev.fetch2okhttp.OkHttpDownloader;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.documentfile.provider.DocumentFile;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class FetchHelper {
    private static FetchHelper instance;
    private final ProfilesManager profilesManager;
    private final Fetch fetch;
    private final Handler handler;
    private final Aria2Helper aria2;

    private FetchHelper(@NonNull Context context) throws GeneralSecurityException, IOException, ProfilesManager.NoCurrentProfileException, InitializationException, Aria2Helper.InitializingException {
        MultiProfile.UserProfile profile = ProfilesManager.get(context).getCurrentSpecific();
        MultiProfile.DirectDownload dd = profile.directDownload;
        if (dd == null) throw new DirectDownloadNotEnabledException();

        OkHttpClient.Builder client = new OkHttpClient.Builder();
        if (!dd.hostnameVerifier) {
            client.hostnameVerifier((s, sslSession) -> true);
        }

        if (dd.certificate != null) NetUtils.setSslSocketFactory(client, dd.certificate);
        if (dd.auth) client.addInterceptor(new BasicAuthInterceptor(dd.username, dd.password));

        FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(context)
                .setDownloadConcurrentLimit(Prefs.getInt(PK.DD_MAX_SIMULTANEOUS_DOWNLOADS))
                .setProgressReportingInterval(1000)
                .enableAutoStart(Prefs.getBoolean(PK.DD_RESUME))
                .setHttpDownloader(new OkHttpDownloader(client.build()))
                .build();

        fetch = Fetch.Impl.getInstance(fetchConfiguration);
        profilesManager = ProfilesManager.get(context);
        handler = new Handler(Looper.getMainLooper());
        aria2 = Aria2Helper.instantiate(context);
    }

    public static void invalidate() {
        if (instance != null) {
            instance.fetch.close();
            instance = null;
        }
    }

    @NonNull
    public static FetchHelper get(@NonNull Context context) throws InitializationException {
        if (instance == null) {
            try {
                instance = new FetchHelper(context.getApplicationContext());
            } catch (GeneralSecurityException | ProfilesManager.NoCurrentProfileException | IOException | Aria2Helper.InitializingException ex) {
                throw new InitializationException(ex);
            }
        }

        return instance;
    }

    @NonNull
    private static DocumentFile getAndValidateDownloadPath(@NonNull Context context) throws PreparationException {
        String uriStr = Prefs.getString(PK.DD_DOWNLOAD_PATH);
        Uri uri = Uri.parse(uriStr);
        if (Objects.equals(uri.getScheme(), "content")) {
            DocumentFile doc = DocumentFile.fromTreeUri(context, uri);
            if (doc == null)
                throw new PreparationException("Invalid uri path: " + uriStr);

            if (!doc.exists())
                throw new PreparationException("Uri path doesn't exists: " + uriStr);

            if (!doc.canWrite())
                throw new PreparationException("Cannot write to uri path: " + uriStr);

            return doc;
        } else {
            File path = new File(uriStr);
            if (!path.exists() && !path.mkdirs())
                throw new PreparationException("Path doesn't exists and can't be created: " + path);

            if (!path.canWrite())
                throw new PreparationException("Cannot write to path: " + path);

            return DocumentFile.fromFile(path);
        }
    }

    @NonNull
    private static Request createRequest(HttpUrl base, OptionsMap global, DocumentFile downloadDir, AriaFile file) {
        DocumentFile dest = downloadDir.createFile(file.getMimeType(), );

        new StorageResolver() {

            @Override
            public boolean renameFile(@NotNull String s, @NotNull String s1) {
                return false;
            }

            @NotNull
            @Override
            public OutputResourceWrapper getRequestOutputResourceWrapper(@NotNull Downloader.ServerRequest serverRequest) {

                return new OutputResourceWrapper() {
                    @Override
                    public void write(@NotNull byte[] bytes, int i, int i1) {

                    }

                    @Override
                    public void setWriteOffset(long l) {

                    }

                    @Override
                    public void flush() {

                    }

                    @Override
                    public void close() {

                    }
                };
            }

            @NotNull
            @Override
            public String getDirectoryForFileDownloaderTypeParallel(@NotNull Downloader.ServerRequest serverRequest) {
                return null;
            }

            @Override
            public boolean fileExists(@NotNull String s) {
                return false;
            }

            @Override
            public boolean deleteFile(@NotNull String s) {
                return false;
            }

            @NotNull
            @Override
            public String createFile(@NotNull String s, boolean b) {
                return null;
            }
        }

        return createRequest(file.getDownloadUrl(global, base), new File(downloadDir, file.getRelativePath(global)));
    }

    @NonNull
    private static Request createRequest(@NonNull HttpUrl url, @NonNull File dest) {
        Request request = new Request(url.toString(), dest.getAbsolutePath());
        request.setEnqueueAction(EnqueueAction.UPDATE_ACCORDINGLY);
        return request;
    }

    @UiThread
    public static void updateDownloadCount(@NonNull Context context, @NonNull FetchDownloadCountListener listener) {
        try {
            FetchHelper.get(context).updateDownloadCountInternal(listener);
        } catch (InitializationException ex) {
            listener.onFetchDownloadCount(0);
        }
    }

    private void updateDownloadCountInternal(final FetchDownloadCountListener listener) {
        if (!fetch.isClosed())
            fetch.getDownloads(result -> listener.onFetchDownloadCount(result.size()));
    }

    private void callFailed(final StartListener listener, final Throwable ex) {
        handler.post(() -> listener.onFailed(ex));
    }

    public void start(@NonNull Context context, @NonNull MultiProfile profile, @NonNull AriaFile file, @NonNull StartListener listener) {
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
                downloadDir.c

                startInternal(createRequest(file.getDownloadUrl(result, base), new File(downloadDir, file.getRelativePath(result))), listener);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                listener.onFailed(ex);
            }
        });
    }

    public void start(@NonNull Context context, @NonNull MultiProfile profile, @NonNull AriaDirectory dir, @NonNull StartListener listener) {
        List<AriaFile> files = dir.allFiles();
        if (files.size() == 1) {
            start(context, profile, files.get(0), listener);
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
                List<Request> requests = new ArrayList<>(files.size());
                for (AriaFile file : files) {
                    Request request = createRequest(base, result, downloadDir, file);
                    request.setGroupId(groupId);
                    requests.add(request);
                }

                startInternal(requests, listener);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                listener.onFailed(ex);
            }
        });
    }

    private void startInternal(@NonNull Request request, @NonNull StartListener listener) {
        if (!fetch.isClosed())
            fetch.enqueue(request, result -> listener.onSuccess(), result -> listener.onFailed(result.getThrowable()));
    }

    private void startInternal(@NonNull List<Request> requests, @NonNull StartListener listener) {
        if (!fetch.isClosed()) fetch.enqueue(requests, result -> listener.onSuccess());
    }

    public void addListener(FetchEventListener listener) {
        if (!fetch.isClosed()) fetch.addListener(listener);
        reloadListener(listener);
    }

    public void removeListener(FetchEventListener listener) {
        if (!fetch.isClosed()) fetch.removeListener(listener);
    }

    public void reloadListener(final FetchEventListener listener) {
        if (!fetch.isClosed()) fetch.getDownloads(listener::onDownloads);
    }

    public void resume(@NotNull FetchDownloadWrapper download) {
        if (!fetch.isClosed()) fetch.resume(download.get().getId());
    }

    public void pause(@NotNull FetchDownloadWrapper download) {
        if (!fetch.isClosed()) fetch.pause(download.get().getId());
    }

    public void remove(@NotNull FetchDownloadWrapper download) {
        if (!fetch.isClosed()) fetch.remove(download.get().getId());
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

    public static class DirectDownloadNotEnabledException extends InitializationException {
    }

    private static class BasicAuthInterceptor implements Interceptor {
        private final String username;
        private final String password;

        BasicAuthInterceptor(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            okhttp3.Request request = chain.request();
            return chain.proceed(request.newBuilder()
                    .addHeader("Authorization", "Basic " + Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP))
                    .build());
        }
    }

    public static class InitializationException extends Exception {
        InitializationException() {
        }

        InitializationException(Throwable cause) {
            super(cause);
        }
    }

    private static class PreparationException extends Exception {
        private PreparationException(String msg) {
            super(msg);
        }
    }
}
