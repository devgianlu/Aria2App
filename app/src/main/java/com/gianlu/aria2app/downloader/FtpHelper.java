package com.gianlu.aria2app.downloader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import com.gianlu.aria2app.api.NetUtils;
import com.gianlu.aria2app.api.aria2.Aria2Helper;
import com.gianlu.aria2app.api.aria2.AriaFile;
import com.gianlu.aria2app.api.aria2.OptionsMap;
import com.gianlu.aria2app.profiles.MultiProfile;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

public final class FtpHelper extends AbsStreamDownloadHelper {
    private static final String TAG = FtpHelper.class.getSimpleName();
    private final MultiProfile.DirectDownload.Ftp dd;

    FtpHelper(@NonNull Context context, @NonNull MultiProfile.UserProfile profile, @NonNull MultiProfile.DirectDownload.Ftp dd) throws Aria2Helper.InitializingException {
        super(context, profile);
        this.dd = dd;
    }

    @NonNull
    @Override
    protected AbsStreamDownloadHelper.DownloadRunnable makeRunnableFor(int id, @NonNull DocumentFile file, @NonNull OptionsMap globalOptions, @NonNull AriaFile remoteFile) {
        return new FtpRunnable(id, file, remoteFile.getAbsolutePath());
    }

    public static class FtpException extends Exception {
        public final int replyCode;

        FtpException(int replyCode) {
            super("Code: " + replyCode);
            this.replyCode = replyCode;
        }
    }

    private class FtpRunnable extends DownloadRunnable {
        private final String remotePath;

        FtpRunnable(int id, @NonNull DocumentFile file, @NonNull String remotePath) {
            super(id, file);
            this.remotePath = remotePath;
        }

        @Override
        public boolean runInternal() {
            FTPClient client = null;
            try {
                if (dd.serverSsl) {
                    FTPSClient ftps = new FTPSClient();
                    if (!dd.hostnameVerifier) ftps.setHostnameVerifier((s, sslSession) -> true);
                    if (dd.certificate != null) NetUtils.setSslSocketFactory(ftps, dd.certificate);
                    client = ftps;
                } else {
                    client = new FTPClient();
                }

                client.connect(dd.hostname, dd.port);
                int reply = client.getReplyCode();
                if (!FTPReply.isPositiveCompletion(reply))
                    throw new FtpException(reply);

                if (!client.login(dd.username, dd.password)) {
                    reply = client.getReplyCode();
                    try {
                        client.logout();
                    } catch (IOException ignored) {
                    }

                    throw new FtpException(reply);
                }

                if (!client.setFileType(FTPClient.BINARY_FILE_TYPE))
                    throw new FtpException(client.getReplyCode());

                client.enterLocalPassiveMode();

                String sizeStr = client.getSize(remotePath);
                try {
                    length = Long.parseLong(sizeStr);
                    Log.d(TAG, "File size is " + length);
                } catch (NumberFormatException ex) {
                    throw new NumberFormatException(sizeStr + " -> " + ex.getMessage());
                }

                try (OutputStream out = openDestination()) {
                    try (InputStream in = client.retrieveFileStream(remotePath)) {
                        downloaded = 0;
                        long lastTime = System.currentTimeMillis();
                        long lastDownloaded = 0;

                        byte[] buffer = new byte[512 * 1024];
                        int read;
                        while (!shouldStop && (read = in.read(buffer)) > 0) {
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

                return true;
            } catch (IOException | FtpException | NumberFormatException | GeneralSecurityException ex) {
                Log.e(TAG, String.format("Download error, id: %d, url: %s", id, getUrl()), ex);
                return false;
            } finally {
                try {
                    if (client != null) {
                        client.logout();
                        client.disconnect();
                    }
                } catch (IOException ignored) {
                }
            }
        }

        @SuppressLint("DefaultLocale")
        @NonNull
        @Override
        public String getUrl() {
            return String.format("ftp://%s:%d%s", dd.hostname, dd.port, remotePath);
        }
    }
}
