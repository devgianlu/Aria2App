package com.gianlu.aria2app.downloader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import com.gianlu.aria2app.api.aria2.Aria2Helper;
import com.gianlu.aria2app.api.aria2.AriaFile;
import com.gianlu.aria2app.api.aria2.OptionsMap;
import com.gianlu.aria2app.profiles.MultiProfile;
import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.common.SMBRuntimeException;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;

import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumSet;

public final class SambaHelper extends AbsStreamDownloadHelper {
    private static final String TAG = SambaHelper.class.getSimpleName();
    private final MultiProfile.DirectDownload.Smb dd;
    private final SMBClient client = new SMBClient();

    SambaHelper(@NonNull Context context, @NonNull MultiProfile.UserProfile profile, @NonNull MultiProfile.DirectDownload.Smb dd) throws Aria2Helper.InitializingException {
        super(context, profile);
        this.dd = dd;
    }

    @NonNull
    @Override
    protected DownloadRunnable makeRunnableFor(int id, @NonNull DocumentFile file, @NonNull OptionsMap globalOptions, @NonNull AriaFile remoteFile) {
        java.io.File remote = new java.io.File(dd.path, remoteFile.getRelativePath(globalOptions));
        return new SambaRunnable(id, file, remote.getAbsolutePath().substring(1));
    }

    private class SambaRunnable extends AbsStreamDownloadHelper.DownloadRunnable {
        private final String remotePath;

        SambaRunnable(int id, @NonNull DocumentFile file, @NonNull String remotePath) {
            super(id, file);
            this.remotePath = remotePath;
        }

        @Override
        protected boolean runInternal() {
            try (Connection connection = client.connect(dd.hostname)) {
                AuthenticationContext ac;
                if (dd.anonymous)
                    ac = AuthenticationContext.anonymous();
                else
                    ac = new AuthenticationContext(dd.username, dd.password.toCharArray(), dd.domain);

                Session session = connection.authenticate(ac);
                try (DiskShare share = (DiskShare) session.connectShare(dd.shareName)) {
                    try (File smbFile = share.openFile(remotePath, EnumSet.of(AccessMask.GENERIC_READ),
                            EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                            SMB2ShareAccess.ALL,
                            SMB2CreateDisposition.FILE_OPEN,
                            EnumSet.noneOf(SMB2CreateOptions.class))) {
                        try (OutputStream out = openDestination()) {
                            downloaded = 0;
                            long lastTime = System.currentTimeMillis();

                            byte[] buffer = new byte[512 * 1024];
                            int read;
                            while (!shouldStop && (read = smbFile.read(buffer, downloaded)) > 0) {
                                out.write(buffer, 0, read);
                                downloaded += read;

                                float diff = ((float) (System.currentTimeMillis() - lastTime)) / 1000;
                                lastTime = System.currentTimeMillis();

                                long speed = (long) (read / diff);
                                long eta = (long) (((float) (length - downloaded) / (float) speed) * 1000);
                                updateProgress(eta, speed);
                            }

                            if (shouldStop)
                                return true;
                        }
                    }
                }

                return true;
            } catch (IOException | SMBRuntimeException ex) {
                Log.e(TAG, String.format("Download error, id: %d, url: %s", id, getUrl()), ex);
                return false;
            }
        }

        @SuppressLint("DefaultLocale")
        @NonNull
        @Override
        public String getUrl() {
            return String.format("smb://%s/%s/%s", dd.hostname, dd.shareName, remotePath);
        }
    }
}
