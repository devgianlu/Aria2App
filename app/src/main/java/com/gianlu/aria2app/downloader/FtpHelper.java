package com.gianlu.aria2app.downloader;

import android.content.Context;

import androidx.annotation.NonNull;

import com.gianlu.aria2app.api.aria2.Aria2Helper;
import com.gianlu.aria2app.api.aria2.AriaDirectory;
import com.gianlu.aria2app.api.aria2.AriaFile;
import com.gianlu.aria2app.profiles.MultiProfile;

public final class FtpHelper extends DirectDownloadHelper { // TODO
    FtpHelper(@NonNull Context context, @NonNull MultiProfile.UserProfile profile, @NonNull MultiProfile.DirectDownload.Ftp dd) throws Aria2Helper.InitializingException {
        super(context, profile);
    }

    @Override
    public void start(@NonNull Context context, @NonNull AriaFile file, @NonNull StartListener listener) {

    }

    @Override
    public void start(@NonNull Context context, @NonNull AriaDirectory dir, @NonNull StartListener listener) {

    }

    @Override
    public void resume(@NonNull DdDownload download) {

    }

    @Override
    public void pause(@NonNull DdDownload download) {

    }

    @Override
    public void restart(@NonNull DdDownload download, @NonNull StartListener listener) {

    }

    @Override
    public void remove(@NonNull DdDownload download) {

    }

    @Override
    public void reloadListener(@NonNull Listener listener) {

    }
}
