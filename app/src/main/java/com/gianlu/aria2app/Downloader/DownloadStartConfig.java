package com.gianlu.aria2app.Downloader;

import android.content.Context;

import com.gianlu.aria2app.NetIO.JTA2.ADir;
import com.gianlu.aria2app.NetIO.JTA2.AFile;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cz.msebera.android.httpclient.client.utils.URIBuilder;

public class DownloadStartConfig {
    private static final Random random = new Random();
    public final List<Task> tasks;
    public final File cacheDir;

    private DownloadStartConfig(Context context) {
        this.tasks = new ArrayList<>();
        this.cacheDir = context.getExternalCacheDir();
    }

    public static DownloadStartConfig create(Context context, Download download, MultiProfile.UserProfile profile, AFile file) throws DownloaderUtils.InvalidPathException, URISyntaxException {
        if (profile.directDownload == null) throw new IllegalArgumentException("WTF?!");

        File downloadPath = DownloaderUtils.getAndValidateDownloadPath(context);
        File destFile = new File(downloadPath, file.getName());

        MultiProfile.DirectDownload dd = profile.directDownload;
        URIBuilder builder = new URIBuilder(dd.getURLAddress());
        builder.setPath(file.getRelativePath(download.dir));

        DownloadStartConfig config = new DownloadStartConfig(context);
        config.addTask(builder.build(), destFile);
        return config;
    }

    public static DownloadStartConfig create(Download download, MultiProfile.UserProfile profile, ADir dir, boolean includeSubdirectories) {
        throw new UnsupportedOperationException(); // TODO
    }

    private void addTask(URI uri, File destFile) {
        tasks.add(new Task(uri, destFile));
    }

    public class Task {
        public final URI uri;
        public final File destFile;
        public final int id;

        public Task(URI uri, File destFile) {
            this.uri = uri;
            this.destFile = destFile;
            this.id = random.nextInt();
        }

        public File getCacheDir() {
            return cacheDir;
        }
    }
}