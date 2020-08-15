package com.gianlu.aria2app.downloader;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.core.util.Consumer;

import com.gianlu.aria2app.Utils;
import com.gianlu.aria2app.api.aria2.Aria2Helper;
import com.gianlu.aria2app.api.aria2.AriaDirectory;
import com.gianlu.aria2app.api.aria2.AriaFile;
import com.gianlu.aria2app.api.aria2.OptionsMap;
import com.gianlu.aria2app.profiles.MultiProfile;
import com.gianlu.aria2app.profiles.MultiProfile.DirectDownload;
import com.gianlu.aria2app.profiles.ProfilesManager;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.HttpUrl;

public abstract class DirectDownloadHelper {
    private static DirectDownloadHelper instance;
    protected final Handler handler;
    protected final Aria2Helper aria2;
    protected final ProfilesManager profilesManager;
    private final List<Listener> listeners = new ArrayList<>(3);
    private final MultiProfile.UserProfile profile;

    public DirectDownloadHelper(@NonNull Context context, @NonNull MultiProfile.UserProfile profile) throws Aria2Helper.InitializingException {
        this.profile = profile;

        handler = new Handler(Looper.getMainLooper());
        aria2 = Aria2Helper.instantiate(context);
        profilesManager = ProfilesManager.get(context);
    }

    @NonNull
    public static DirectDownloadHelper get(@NonNull Context context) throws DirectDownloadHelper.InitializationException {
        if (instance != null) return instance;

        MultiProfile.UserProfile profile;
        try {
            profile = ProfilesManager.get(context).getCurrentSpecific();
        } catch (ProfilesManager.NoCurrentProfileException ex) {
            throw new InitializationException(ex);
        }

        DirectDownload dd = profile.directDownload;
        if (dd == null) throw new FetchHelper.DirectDownloadNotEnabledException(profile);

        switch (dd.type) {
            case WEB:
                try {
                    return instance = new FetchHelper(context, profile, dd.web);
                } catch (GeneralSecurityException | Aria2Helper.InitializingException | IOException ex) {
                    throw new InitializationException(ex);
                }
            case FTP:
                try {
                    return instance = new FtpHelper(context, profile, dd.ftp);
                } catch (Aria2Helper.InitializingException ex) {
                    throw new InitializationException(ex);
                }
            case SMB:
                try {
                    return instance = new SambaHelper(context, profile, dd.smb);
                } catch (Aria2Helper.InitializingException ex) {
                    throw new InitializationException(ex);
                }
            default:
                throw new IllegalArgumentException("Unknown type: " + dd.type);
        }
    }

    public static void updateDownloadCount(@NonNull Context context, @NonNull UpdateDownloadCountListener listener) {
        try {
            DirectDownloadHelper.get(context).reloadListener(new Listener() {
                @Override
                public void onDownloads(@NonNull List<DdDownload> downloads) {
                    listener.onDdDownloadCount(downloads.size());
                }

                @Override
                public void onAdded(@NonNull DdDownload download) {

                }

                @Override
                public void onUpdated(@NonNull DdDownload download) {

                }

                @Override
                public void onProgress(@NonNull DdDownload download, long eta, long speed) {

                }

                @Override
                public void onRemoved(@NonNull DdDownload download) {

                }
            });
        } catch (InitializationException ignored) {
        }
    }

    public static void invalidate() {
        // TODO
    }

    public boolean canStream(@NonNull String mime) {
        if (!Utils.isStreamable(mime))
            return false;

        return profile.directDownload != null && profile.directDownload.type == DirectDownload.Type.WEB;
    }

    @Nullable
    public Intent getStreamIntent(@NonNull OptionsMap global, @NonNull AriaFile file) {
        DirectDownload dd = profile.directDownload;
        if (dd == null || dd.type != DirectDownload.Type.WEB)
            throw new IllegalStateException("WTF?!");

        HttpUrl base = dd.web.getUrl();
        if (base == null) return null;

        HttpUrl url = file.getDownloadUrl(global, base);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(url.toString()), file.getMimeType());

        if (dd.web.auth) {
            Bundle headers = new Bundle();
            headers.putString("Authorization", dd.web.getAuthorizationHeader());
            intent.putExtra(Browser.EXTRA_HEADERS, headers);
        }

        return intent;
    }

    public abstract void start(@NonNull Context context, @NonNull AriaFile file, @NonNull StartListener listener);

    //region Download actions

    public abstract void start(@NonNull Context context, @NonNull AriaDirectory dir, @NonNull StartListener listener);

    public abstract void resume(@NonNull DdDownload download);

    public abstract void pause(@NonNull DdDownload download);

    public abstract void restart(@NonNull DdDownload download, @NonNull DirectDownloadHelper.StartListener listener);

    public abstract void remove(@NonNull DdDownload download);

    public final void addListener(@NonNull Listener listener) {
        listeners.add(listener);
        reloadListener(listener);
    }

    //endregion

    //region Listeners

    public final void removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    /**
     * Called to notify {@link Listener#onDownloads(List)} with all downloads.
     *
     * @param listener The listener
     */
    public abstract void reloadListener(@NonNull Listener listener);

    protected final void forEachListener(@NonNull Consumer<Listener> consumer) {
        for (Listener listener : new ArrayList<>(listeners))
            handler.post(() -> consumer.accept(listener));
    }

    //endregion

    public interface UpdateDownloadCountListener {
        void onDdDownloadCount(int count);
    }

    @UiThread
    public interface StartListener {
        void onSuccess();

        void onFailed(@NonNull Throwable ex);
    }

    @UiThread
    public interface Listener {
        void onDownloads(@NonNull List<DdDownload> downloads);

        void onAdded(@NonNull DdDownload download);

        void onUpdated(@NonNull DdDownload download);

        void onProgress(@NonNull DdDownload download, long eta, long speed);

        void onRemoved(@NonNull DdDownload download);
    }

    public static class DirectDownloadNotEnabledException extends InitializationException {
        DirectDownloadNotEnabledException(@NonNull MultiProfile.UserProfile profile) {
            super(profile.getParent().name + " - " + profile.connectivityCondition);
        }
    }

    public static class InitializationException extends Exception {
        InitializationException() {
        }

        InitializationException(String message) {
            super(message);
        }

        InitializationException(Throwable cause) {
            super(cause);
        }
    }
}
