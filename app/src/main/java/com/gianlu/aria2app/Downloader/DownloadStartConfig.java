package com.gianlu.aria2app.Downloader;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.NetIO.Aria2.AriaDirectory;
import com.gianlu.aria2app.NetIO.Aria2.AriaFile;
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithUpdate;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.Toaster;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import okhttp3.HttpUrl;

public class DownloadStartConfig {
    public final List<Task> tasks;
    public final File cacheDir;
    public final String profileId;

    private DownloadStartConfig(@NonNull Context context, String profileId) {
        this.profileId = profileId;
        this.tasks = new ArrayList<>();
        this.cacheDir = context.getExternalCacheDir();
    }

    public static DownloadStartConfig create(@NonNull Context context, DownloadWithUpdate download, MultiProfile.UserProfile profile, AriaDirectory dir) throws DownloaderUtils.InvalidPathException, CannotCreateStartConfigException {
        if (profile.directDownload == null) throw new IllegalArgumentException("WTF?!");

        File downloadPath = DownloaderUtils.getAndValidateDownloadPath();
        DownloadStartConfig config = new DownloadStartConfig(context, profile.getParent().id);

        for (AriaFile file : dir.allFiles()) {
            String relativePath = file.getRelativePath(download.update().dir);
            File destFile = new File(downloadPath, relativePath);

            MultiProfile.DirectDownload dd = profile.directDownload;
            HttpUrl url = dd.getUrl();
            if (url == null)
                throw new CannotCreateStartConfigException(CannotCreateStartConfigException.Cause.INVALID_URL);

            HttpUrl.Builder builder = url.newBuilder();
            builder.addPathSegments(relativePath);

            config.addTask(builder.build(), destFile, dd.username, dd.password);
        }

        return config;
    }

    public static DownloadStartConfig create(@NonNull Context context, DownloadWithUpdate download, MultiProfile.UserProfile profile, AriaFile file) throws DownloaderUtils.InvalidPathException, CannotCreateStartConfigException {
        if (profile.directDownload == null) throw new IllegalArgumentException("WTF?!");

        File downloadPath = DownloaderUtils.getAndValidateDownloadPath();
        File destFile = new File(downloadPath, file.getName());

        MultiProfile.DirectDownload dd = profile.directDownload;
        HttpUrl url = dd.getUrl();
        if (url == null)
            throw new CannotCreateStartConfigException(CannotCreateStartConfigException.Cause.INVALID_URL);

        DownloadStartConfig config = new DownloadStartConfig(context, profile.getParent().id);
        config.addTask(file.getDownloadUrl(download.update().dir, url), destFile, dd.username, dd.password);
        return config;
    }

    public static DownloadStartConfig createForSavedState(@NonNull Context context, SavedDownloadsManager.SavedState state) throws CannotCreateStartConfigException, DownloaderUtils.InvalidPathException {
        ProfilesManager manager = ProfilesManager.get(context);

        MultiProfile.UserProfile profile;
        if (manager.profileExists(state.profileId)) {
            try {
                profile = manager.retrieveProfile(state.profileId).getProfile(context);
            } catch (JSONException | IOException ex) {
                throw new CannotCreateStartConfigException(ex);
            }
        } else {
            throw new CannotCreateStartConfigException(CannotCreateStartConfigException.Cause.PROFILE_DOES_NOT_EXIST);
        }

        if (profile.directDownload == null)
            throw new CannotCreateStartConfigException(CannotCreateStartConfigException.Cause.DD_NOT_ENABLED);

        File downloadPath = DownloaderUtils.getAndValidateDownloadPath();
        File destFile = new File(downloadPath, state.fileName);

        MultiProfile.DirectDownload dd = profile.directDownload;
        HttpUrl url = dd.getUrl();
        if (url == null)
            throw new CannotCreateStartConfigException(CannotCreateStartConfigException.Cause.INVALID_URL);

        HttpUrl.Builder builder = url.newBuilder();
        builder.encodedPath(state.path);

        DownloadStartConfig config = new DownloadStartConfig(context, profile.getParent().id);
        config.addResumableTask(state.id, builder.build(), destFile, dd.username, dd.password);
        return config;
    }

    public static DownloadStartConfig recreate(@NonNull Context context, DownloadTask task) {
        DownloadStartConfig config = new DownloadStartConfig(context, task.task.getProfileId());
        config.addTask(task.task.url, task.task.destFile, task.task.username, task.task.password);
        return config;
    }

    private void addTask(HttpUrl url, File destFile, String username, String password) {
        tasks.add(new Task(url, destFile, username, password));
    }

    private void addResumableTask(int id, HttpUrl url, File destFile, String username, String password) {
        tasks.add(new Task(id, url, destFile, username, password, true));
    }

    public static class CannotCreateStartConfigException extends Exception {
        final Cause cause;

        public CannotCreateStartConfigException(Exception ex) {
            super(ex);
            this.cause = Cause.INTERNAL;
        }

        CannotCreateStartConfigException(Cause cause) {
            this.cause = cause;
        }

        public void showAppropriateToast(@NonNull Context context) {
            Toaster toaster = Toaster.with(context);
            toaster.ex(this);

            switch (cause) {
                default:
                case INVALID_URL:
                case INTERNAL:
                    toaster.message(R.string.failedDownloadingFile);
                    break;
                case DD_NOT_ENABLED:
                    toaster.message(R.string.ddNotEnabled);
                    break;
                case PROFILE_DOES_NOT_EXIST:
                    toaster.message(R.string.profileDoesntExist);
                    break;
            }

            toaster.show();
        }

        public enum Cause {
            INTERNAL,
            DD_NOT_ENABLED,
            INVALID_URL,
            PROFILE_DOES_NOT_EXIST
        }
    }

    public class Task implements Serializable {
        public final HttpUrl url;
        public final File destFile;
        public final int id;
        public final String username;
        public final String password;
        public final boolean resumable;

        Task(int id, HttpUrl url, File destFile, @Nullable String username, @Nullable String password, boolean resumable) {
            this.url = url;
            this.destFile = destFile;
            this.username = username;
            this.password = password;
            this.id = id;
            this.resumable = resumable;
        }

        Task(HttpUrl url, File destFile, @Nullable String username, @Nullable String password) {
            this(ThreadLocalRandom.current().nextInt(), url, destFile, username, password, false);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Task task = (Task) o;
            return id == task.id;
        }

        public String getProfileId() {
            return profileId;
        }

        public String getName() {
            return destFile.getName();
        }

        public boolean hasAuth() {
            return username != null && password != null;
        }

        public File getCacheDir() {
            return cacheDir;
        }
    }
}