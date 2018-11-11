package com.gianlu.aria2app.NetIO.Downloader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import com.gianlu.aria2app.NetIO.Aria2.AriaDirectory;
import com.gianlu.aria2app.NetIO.Aria2.AriaFile;
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithUpdate;
import com.gianlu.aria2app.NetIO.NetUtils;
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
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import kotlin.Pair;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class FetchHelper {
    private static FetchHelper instance;
    private final ProfilesManager profilesManager;
    private final Fetch fetch;
    private final Handler handler;

    private FetchHelper(@NonNull Context context) throws GeneralSecurityException, IOException, ProfilesManager.NoCurrentProfileException, InitializationException {
        MultiProfile.UserProfile profile = ProfilesManager.get(context).getCurrentSpecific();
        MultiProfile.DirectDownload dd = profile.directDownload;
        if (dd == null) throw new DirectDownloadNotEnabledException();

        OkHttpClient.Builder client = new OkHttpClient.Builder();
        if (!dd.hostnameVerifier) {
            client.hostnameVerifier(new HostnameVerifier() {
                @SuppressLint("BadHostnameVerifier")
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            });
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
                instance = new FetchHelper(context);
            } catch (GeneralSecurityException | ProfilesManager.NoCurrentProfileException | IOException ex) {
                throw new InitializationException(ex);
            }
        }

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
        fetch.enqueue(requests, new Func<List<Pair<Request, Error>>>() {
            @Override
            public void call(@NotNull List<Pair<Request, Error>> result) {
                listener.onSuccess();
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
