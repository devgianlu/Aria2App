package com.gianlu.aria2app.Downloader;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Environment;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.NonNull;

import com.gianlu.aria2app.PKeys;
import com.gianlu.commonutils.Prefs;

import java.io.File;

public class DownloaderUtils {
    final static int START_DOWNLOAD = 0;

    public static void bindService(Context context, ServiceConnection conn) {
        context.getApplicationContext().bindService(new Intent(context, DownloaderService.class), conn, 0);
    }

    public static void addDownload(@NonNull Messenger messenger, DownloadStartConfig config) throws RemoteException {
        messenger.send(Message.obtain(null, START_DOWNLOAD, 0, 0, config));
    }

    public static File getAndValidateDownloadPath(Context context) throws InvalidPathException {
        File path = new File(Prefs.getString(context, PKeys.DD_DOWNLOAD_PATH, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()));
        if (!path.exists()) {
            if (!path.mkdirs()) throw new InvalidPathException();
        }

        if (!path.canWrite()) throw new InvalidPathException();

        return path;
    }

    public static void startService(Context context) {
        context.startService(new Intent(context, DownloaderService.class));
    }

    public static class InvalidPathException extends Exception {
    }
}
