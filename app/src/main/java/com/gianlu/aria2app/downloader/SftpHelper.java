package com.gianlu.aria2app.downloader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.documentfile.provider.DocumentFile;

import com.gianlu.aria2app.api.aria2.Aria2Helper;
import com.gianlu.aria2app.profiles.MultiProfile;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;
import com.jcraft.jsch.UserInfo;

import java.io.IOException;
import java.io.OutputStream;

public final class SftpHelper extends AbsStreamDownloadHelper {
    private static final String TAG = SftpHelper.class.getSimpleName();
    private final MultiProfile.DirectDownload.Sftp dd;
    private final JSch jSch = new JSch();

    public SftpHelper(@NonNull Context context, @NonNull MultiProfile.UserProfile profile, @NonNull MultiProfile.DirectDownload.Sftp dd) throws Aria2Helper.InitializingException {
        super(context, profile);
        this.dd = dd;
    }

    @NonNull
    @Override
    protected DownloadRunnable makeRunnableFor(int id, @NonNull DocumentFile file, @NonNull String remotePath) {
        return new SftpRunnable(id, file, remotePath);
    }

    public static class BasicUserInfo implements UserInfo {
        private final String password;

        public BasicUserInfo(@NonNull String password) {
            this.password = password;
        }

        @Override
        public String getPassphrase() {
            return password;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public boolean promptPassword(String message) {
            return true;
        }

        @Override
        public boolean promptPassphrase(String message) {
            return true;
        }

        @Override
        public boolean promptYesNo(String message) {
            Log.d(TAG, "promptYesNo: " + message);
            return true; // FIXME
        }

        @Override
        public void showMessage(String message) {
            Log.d(TAG, "showMessage: " + message); // FIXME
        }
    }

    private class SftpRunnable extends DownloadRunnable {
        private final String remotePath;

        SftpRunnable(int id, @NonNull DocumentFile file, @NonNull String remotePath) {
            super(id, file);
            this.remotePath = remotePath;
        }

        @Override
        @WorkerThread
        public boolean runInternal() {
            Session session = null;
            try {
                session = jSch.getSession(dd.username, dd.hostname, dd.port);
                session.setUserInfo(new BasicUserInfo(dd.password));
                session.connect();

                ChannelSftp ch = (ChannelSftp) session.openChannel("sftp");
                ch.connect();

                SftpATTRS attrs = ch.stat(remotePath);
                length = attrs.getSize();
                Log.d(TAG, "File size is " + length);

                try (OutputStream out = openDestination()) {
                    ch.get(remotePath, out, new SftpProgressMonitor() {
                        long lastTime;

                        @Override
                        public void init(int op, String src, String dest, long max) {
                            downloaded = 0;
                            lastTime = System.currentTimeMillis();
                        }

                        @Override
                        public boolean count(long count) {
                            float diff = ((float) (System.currentTimeMillis() - lastTime)) / 1000;
                            lastTime = System.currentTimeMillis();

                            long speed = (long) (count / diff);
                            long eta = (long) (((float) (length - downloaded) / (float) speed) * 1000);
                            updateProgress(eta, speed);

                            return !shouldStop;
                        }

                        @Override
                        public void end() {
                        }
                    });
                }

                return true;
            } catch (JSchException | SftpException | IOException ex) {
                Log.e(TAG, String.format("Download error, id: %d, url: %s", id, getUrl()), ex);
                return false;
            } finally {
                if (session != null) session.disconnect();
            }
        }

        @SuppressLint("DefaultLocale")
        @NonNull
        @Override
        public String getUrl() {
            return String.format("sftp://%s:%d%s", dd.hostname, dd.port, remotePath);
        }
    }
}
