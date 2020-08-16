package com.gianlu.aria2app.downloader;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Browser;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.core.util.Consumer;
import androidx.documentfile.provider.DocumentFile;

import com.gianlu.aria2app.PK;
import com.gianlu.aria2app.Utils;
import com.gianlu.aria2app.api.aria2.Aria2Helper;
import com.gianlu.aria2app.api.aria2.AriaDirectory;
import com.gianlu.aria2app.api.aria2.AriaFile;
import com.gianlu.aria2app.api.aria2.OptionsMap;
import com.gianlu.aria2app.profiles.MultiProfile;
import com.gianlu.aria2app.profiles.MultiProfile.DirectDownload;
import com.gianlu.aria2app.profiles.ProfilesManager;
import com.gianlu.commonutils.preferences.Prefs;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import okhttp3.HttpUrl;

public abstract class DirectDownloadHelper implements Closeable {
    private static final String TAG = DirectDownloadHelper.class.getSimpleName();
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
            case SFTP:
                try {
                    return instance = new SftpHelper(context, profile, dd.sftp);
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
        try {
            instance.close();
        } catch (IOException ex) {
            Log.e(TAG, "Failed closing DirectDownload helper instance.", ex);
        } finally {
            instance = null;
        }
    }

    //region Prepare local storage

    @NonNull
    protected static DocumentFile getAndValidateDownloadPath(@NonNull Context context) throws PreparationException {
        String uriStr = Prefs.getString(PK.DD_DOWNLOAD_PATH);
        Uri uri = Uri.parse(uriStr);
        if (Objects.equals(uri.getScheme(), "content")) {
            DocumentFile doc;
            try {
                doc = DocumentFile.fromTreeUri(context, uri);
            } catch (RuntimeException ex) {
                throw new FetchHelper.PreparationException(ex);
            }

            if (doc == null)
                throw new FetchHelper.PreparationException("Invalid uri path: " + uriStr);

            if (!doc.exists())
                throw new FetchHelper.PreparationException("Uri path doesn't exists: " + uriStr);

            if (!doc.canWrite())
                throw new FetchHelper.PreparationException("Cannot write to uri path: " + uriStr);

            return doc;
        } else {
            if (Objects.equals(uri.getScheme(), "file") && uri.getPath() != null)
                uriStr = uri.getPath();

            File path = new File(uriStr);
            if (!path.exists() && !path.mkdirs())
                throw new FetchHelper.PreparationException("Path doesn't exists and can't be created: " + path);

            if (!path.canWrite())
                throw new FetchHelper.PreparationException("Cannot write to path: " + path);

            DocumentFile doc = DocumentFile.fromFile(path);
            Prefs.putString(PK.DD_DOWNLOAD_PATH, doc.getUri().toString());
            return doc;
        }
    }

    @NonNull
    protected static DocumentFile createAllDirs(@NonNull DocumentFile parent, @NonNull String filePath) throws PreparationException {
        String[] split = filePath.split(Pattern.quote(File.separator));
        for (int i = 0; i < split.length - 1; i++) { // Skip last segment
            DocumentFile doc = parent.findFile(split[i]);
            if (doc == null || !doc.isDirectory()) {
                parent = parent.createDirectory(split[i]);
                if (parent == null)
                    throw new FetchHelper.PreparationException("Couldn't create directory: " + split[i]);
            } else {
                parent = doc;
            }
        }

        return parent;
    }

    @NonNull
    protected static DocumentFile createDestFile(@NotNull OptionsMap global, @NotNull DocumentFile ddDir, @NotNull AriaFile file) throws PreparationException {
        String mime = file.getMimeType();
        String fileName = file.getName();
        if (mime == null) {
            mime = "";
        } else {
            int index = fileName.lastIndexOf('.');
            if (index != -1) fileName = fileName.substring(0, index);
        }

        DocumentFile parent = createAllDirs(ddDir, file.getRelativePath(global));
        DocumentFile dest = parent.createFile(mime, fileName);
        if (dest == null)
            throw new PreparationException("Couldn't create file inside directory: " + parent);

        return dest;
    }

    //endregion

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

    protected void callFailed(StartListener listener, Throwable ex) {
        handler.post(() -> listener.onFailed(ex));
    }

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

        /**
         * Notifies that the download has progressed.
         *
         * @param download The download object
         * @param eta      Remaining time in milliseconds
         * @param speed    Download speed in bytes per second
         */
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

    public static class PreparationException extends Exception {
        PreparationException(String msg) {
            super(msg);
        }

        PreparationException(Throwable ex) {
            super(ex);
        }
    }
}
