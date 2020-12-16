package com.gianlu.aria2app.downloader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import com.gianlu.aria2app.api.aria2.Aria2Helper;
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

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

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

        loadDb(context);
    }

    @NonNull
    @Override
    protected DownloadRunnable makeRunnableFor(int id, @NonNull DocumentFile file, @NonNull RemoteFile remoteFile) {
        return new SambaRunnable(id, file, remoteFile.getRelativePath(dd.path));
    }

    @NotNull
    @Contract("_, _ -> new")
    @Override
    protected DownloadRunnable makeRunnableFor(int id, @NonNull DownloadRunnable old) {
        return new SambaRunnable(id, old.file, ((SambaRunnable) old).remotePath);
    }

    private class SambaRunnable extends AbsStreamDownloadHelper.DownloadRunnable {
        SambaRunnable(int id, @NonNull DocumentFile file, @NonNull String remotePath) {
            super(id, file, remotePath);
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
                            if (downloaded < 0) downloaded = 0;
                            long lastTime = System.currentTimeMillis();
                            long lastDownloaded = 0;

                            Log.d(TAG, "Start from " + downloaded + ", id: " + id);

                            byte[] buffer = new byte[512 * 1024];
                            int read;
                            while (!shouldStop && (read = smbFile.read(buffer, downloaded)) > 0) {
                                out.write(buffer, 0, read);
                                downloaded += read;
                                lastDownloaded += read;

                                if (updateProgress(lastTime, lastDownloaded)) {
                                    lastTime = System.currentTimeMillis();
                                    lastDownloaded = 0;
                                }
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
