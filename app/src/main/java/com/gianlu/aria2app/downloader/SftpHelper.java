package com.gianlu.aria2app.downloader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.documentfile.provider.DocumentFile;

import com.gianlu.aria2app.R;
import com.gianlu.aria2app.api.aria2.Aria2Helper;
import com.gianlu.aria2app.api.aria2.OptionsMap;
import com.gianlu.aria2app.profiles.MultiProfile;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;
import com.jcraft.jsch.UserInfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SftpHelper extends AbsStreamDownloadHelper {
    private static final String TAG = SftpHelper.class.getSimpleName();
    private static final String[] sftpNames = {"ssh-dss", "ssh-rsa", "ecdsa-sha2-nistp256", "ecdsa-sha2-nistp384", "ecdsa-sha2-nistp521"};
    private final MultiProfile.DirectDownload.Sftp dd;
    private final JSch jSch;

    public SftpHelper(@NonNull Context context, @NonNull MultiProfile.UserProfile profile, @NonNull MultiProfile.DirectDownload.Sftp dd) throws Aria2Helper.InitializingException {
        super(context, profile);
        this.dd = dd;

        try {
            this.jSch = createJsch(dd);
        } catch (JSchException ex) {
            throw new Aria2Helper.InitializingException(ex);
        }

        loadDb(context);
    }

    private static int sftpNameToType(@NonNull String name) {
        for (int i = 0; i < sftpNames.length; i++)
            if (sftpNames[i].equals(name))
                return i + 1;

        return 6;
    }

    @NonNull
    public static String toString(@Nullable HostKey hk) {
        if (hk == null) return "";

        StringBuilder builder = new StringBuilder();
        builder.append(hk.getHost());
        builder.append(' ');
        builder.append(hk.getType());
        builder.append(' ');
        builder.append(hk.getKey());

        String comment = hk.getComment();
        if (comment != null) {
            builder.append(' ');
            builder.append(comment);
        }

        return builder.toString();
    }

    @Nullable
    public static HostKey parseHostKey(@NonNull String str) {
        if (str.isEmpty()) return null;

        String[] split = str.split("\\s");
        String type = split[1];
        String key = split[2];

        String comment = null;
        if (split.length >= 4)
            comment = split[3];

        try {
            return new HostKey("", split[0], sftpNameToType(type), Base64.decode(key, Base64.NO_WRAP), comment);
        } catch (JSchException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @NonNull
    public static JSch createJsch(@NonNull MultiProfile.DirectDownload.Sftp dd) throws JSchException {
        JSch jSch = new JSch();
        jSch.setKnownHosts(new ByteArrayInputStream(dd.hostKey.getBytes()));
        return jSch;
    }

    public static void firstConnection(@NonNull Activity activity, @NonNull MultiProfile.DirectDownload.Sftp dd, @NonNull FirstConnectionListener listener) {
        Handler handler = new Handler(Looper.getMainLooper());
        UserInfo info = new UserInfo() {
            @Override
            public String getPassphrase() {
                return null;
            }

            @Override
            public String getPassword() {
                return null;
            }

            @Override
            public boolean promptPassword(String message) {
                return false;
            }

            @Override
            public boolean promptPassphrase(String message) {
                return false;
            }

            @Override
            public boolean promptYesNo(String message) {
                AtomicBoolean result = new AtomicBoolean(false);

                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.sftpPrompt)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                            synchronized (result) {
                                result.set(true);
                                result.notify();
                            }
                        })
                        .setNegativeButton(android.R.string.no, (dialog, which) -> {
                            synchronized (result) {
                                result.set(false);
                                result.notify();
                            }
                        });

                DialogUtils.showDialog(activity, builder);

                synchronized (result) {
                    try {
                        result.wait();
                        return result.get();
                    } catch (InterruptedException ex) {
                        return false;
                    }
                }
            }

            @Override
            public void showMessage(String message) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.sftpPrompt)
                        .setMessage(message)
                        .setNeutralButton(android.R.string.ok, null);

                DialogUtils.showDialog(activity, builder);
            }
        };

        Thread thread = new Thread(() -> {
            Session session = null;
            try {
                JSch jSch = createJsch(dd);
                session = jSch.getSession(dd.username, dd.hostname, dd.port);
                session.setUserInfo(info);
                session.setPassword(dd.password);
                session.connect();

                Session finalSession = session;
                handler.post(() -> listener.onDone(finalSession.getHostKey()));
            } catch (JSchException ex) {
                Log.e(TAG, "Failed connecting.", ex);
                handler.post(() -> listener.onFailed(ex));
            } finally {
                if (session != null) session.disconnect();
            }
        });
        thread.setName("sftp-first-connection");
        thread.start();
    }

    @NonNull
    @Override
    protected DownloadRunnable makeRunnableFor(int id, @NonNull DocumentFile file, @NonNull OptionsMap globalOptions, @NonNull RemoteFile remoteFile) {
        java.io.File remote = new java.io.File(dd.path, remoteFile.getRelativePath(globalOptions));
        return new SftpRunnable(id, file, remote.getAbsolutePath().substring(1));
    }

    @NonNull
    @Override
    protected DownloadRunnable makeRunnableFor(int id, @NonNull DownloadRunnable old) {
        return new SftpRunnable(id, old.file, ((SftpRunnable) old).remotePath);
    }

    @UiThread
    public interface FirstConnectionListener {
        void onDone(@NonNull HostKey hostKey);

        void onFailed(@NonNull JSchException ex);
    }

    private class SftpRunnable extends DownloadRunnable {
        SftpRunnable(int id, @NonNull DocumentFile file, @NonNull String remotePath) {
            super(id, file, remotePath);
        }

        @Override
        @WorkerThread
        public boolean runInternal() {
            Session session = null;
            try {
                session = jSch.getSession(dd.username, dd.hostname, dd.port);
                session.setPassword(dd.password);
                session.connect();

                ChannelSftp ch = (ChannelSftp) session.openChannel("sftp");
                ch.connect();

                SftpATTRS attrs = ch.stat(remotePath);
                length = attrs.getSize();
                Log.d(TAG, "File size is " + length);

                try (OutputStream out = openDestination()) {
                    SftpProgressMonitor monitor = new SftpProgressMonitor() {
                        long lastTime;
                        long lastDownloaded;

                        @Override
                        public void init(int op, String src, String dest, long max) {
                            downloaded = 0;
                            lastTime = System.currentTimeMillis();
                            lastDownloaded = 0;
                        }

                        @Override
                        public boolean count(long count) {
                            downloaded += count;
                            lastDownloaded += count;

                            if (updateProgress(lastTime, lastDownloaded)) {
                                lastTime = System.currentTimeMillis();
                                lastDownloaded = 0;
                            }

                            return !shouldStop;
                        }

                        @Override
                        public void end() {
                        }
                    };

                    Log.d(TAG, "Start from " + downloaded + ", id: " + id);
                    if (downloaded > 0)
                        ch.get(remotePath, out, monitor, ChannelSftp.RESUME, downloaded);
                    else
                        ch.get(remotePath, out, monitor);
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
