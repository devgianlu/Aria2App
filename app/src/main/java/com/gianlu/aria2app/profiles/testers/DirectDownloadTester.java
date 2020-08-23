package com.gianlu.aria2app.profiles.testers;


import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.gianlu.aria2app.api.NetUtils;
import com.gianlu.aria2app.api.StatusCodeException;
import com.gianlu.aria2app.profiles.MultiProfile;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.common.SMBRuntimeException;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.share.DiskShare;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

class DirectDownloadTester extends BaseTester<Boolean> {
    private static final int TIMEOUT = 5;

    DirectDownloadTester(Context context, @NonNull MultiProfile.UserProfile profile, @Nullable PublishListener<Boolean> listener) {
        super(context, profile, listener);
    }

    private void publishError(Exception ex) {
        if (ex instanceof StatusCodeException) {
            publishMessage("Server returned " + ex.getMessage(), Level.ERROR);
            if (((StatusCodeException) ex).code == 401)
                publishMessage("Your username and/or password may be wrong", Level.ERROR);
        } else {
            publishMessage(ex.getMessage(), Level.ERROR);
        }
    }

    @Nullable
    @Override
    @WorkerThread
    public Boolean call(@Nullable Object prevResult) {
        switch (profile.directDownload.type) {
            case WEB:
                return callWeb();
            case FTP:
                return callFtp();
            case SFTP:
                return callSftp();
            case SMB:
                return callSmb();
            default:
                throw new IllegalArgumentException("Unknown type: " + profile.directDownload.type);
        }
    }

    @Nullable
    @WorkerThread
    private Boolean callWeb() {
        OkHttpClient client = new OkHttpClient.Builder()
                .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                .build();

        try {
            MultiProfile.DirectDownload.Web dd = profile.directDownload.web;
            HttpUrl baseUrl = dd.getUrl();
            if (baseUrl == null) {
                publishMessage("Invalid DirectDownload url", Level.ERROR);
                return null;
            }

            Request.Builder builder = new Request.Builder().get().url(baseUrl);
            if (dd.auth) builder.header("Authorization", dd.getAuthorizationHeader());
            try (Response resp = client.newCall(builder.build()).execute()) {
                if (resp.code() == 200) {
                    publishMessage("Your DirectDownload configuration is working", Level.SUCCESS);
                    return true;
                } else {
                    publishError(new StatusCodeException(resp));
                    return null;
                }
            }
        } catch (IOException ex) {
            publishError(ex);
            return null;
        } finally {
            client.connectionPool().evictAll();
            client.dispatcher().executorService().shutdown();
        }
    }

    @Nullable
    @WorkerThread
    private Boolean callSftp() {
        MultiProfile.DirectDownload.Sftp dd = profile.directDownload.sftp;

        Session session = null;
        try {
            JSch jSch = new JSch();
            session = jSch.getSession(dd.username, dd.hostname, dd.port);
            session.setPassword(dd.password);
            session.connect();

            publishMessage("Your DirectDownload configuration is working", Level.SUCCESS);
            return true;
        } catch (JSchException ex) {
            publishError(ex);
            return null;
        } finally {
            if (session != null) session.disconnect();
        }
    }

    @Nullable
    @WorkerThread
    private Boolean callFtp() {
        MultiProfile.DirectDownload.Ftp dd = profile.directDownload.ftp;

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
            if (!FTPReply.isPositiveCompletion(reply)) {
                publishMessage("Failed connecting, code: " + reply, Level.ERROR);
                return null;
            }

            if (!client.login(dd.username, dd.password)) {
                reply = client.getReplyCode();
                publishMessage("Failed logging in, code: " + reply, Level.ERROR);
                return null;
            }

            return true;
        } catch (GeneralSecurityException | IOException ex) {
            publishError(ex);
            return null;
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

    @Nullable
    @WorkerThread
    private Boolean callSmb() {
        MultiProfile.DirectDownload.Smb dd = profile.directDownload.smb;

        try (SMBClient client = new SMBClient(); Connection connection = client.connect(dd.hostname)) {
            AuthenticationContext ac;
            if (dd.anonymous)
                ac = AuthenticationContext.anonymous();
            else
                ac = new AuthenticationContext(dd.username, dd.password.toCharArray(), dd.domain);

            com.hierynomus.smbj.session.Session session = connection.authenticate(ac);
            try (DiskShare share = (DiskShare) session.connectShare(dd.shareName)) {
                return true;
            }
        } catch (IOException | SMBRuntimeException ex) {
            publishError(ex);
            return null;
        }
    }

    @NonNull
    @Override
    public String describe() {
        return "DirectDownload test";
    }
}
