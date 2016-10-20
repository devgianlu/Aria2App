package com.gianlu.aria2app.Services;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.util.Base64;

import com.gianlu.aria2app.Main.Profile.DirectDownload;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

// TODO: Custom notification view (and maybe ContentIntent and some dialogs to display progress and speed)
public class DownloadService extends IntentService {
    private static NotificationManagerCompat notificationManager;
    private Long downloaded = 0L;
    private Long length = 0L;
    private int notificationId;
    private File file;

    public DownloadService() {
        super("Aria2App download service");
    }

    public static Intent createStartIntent(Context context, File file, String remotePath) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return new Intent(context, DownloadService.class)
                .putExtra("file", file)
                .putExtra("remotePath", remotePath)
                .putExtra("directDownload",
                        new DirectDownload(
                                preferences.getString("dd_addr", "http://127.0.0.1/"),
                                preferences.getBoolean("dd_auth", false),
                                preferences.getString("dd_user", ""),
                                preferences.getString("dd_passwd", "")));
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        notificationId = new Random().nextInt(10000);
        file = (File) intent.getSerializableExtra("file");
        notificationManager = NotificationManagerCompat.from(this);

        startForeground(notificationId, new NotificationCompat.Builder(this)
                .setShowWhen(true)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setContentTitle(getString(R.string.downloading_file, file.getName()))
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setProgress(0, 0, true)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setSmallIcon(R.drawable.ic_notification)
                .addAction(new NotificationCompat.Action.Builder(
                        R.drawable.ic_clear_black_48dp, /* TODO: That button is horrible (as well as the icon) */
                        getString(R.string.stopNotificationService),
                        PendingIntent.getService(this, 0,
                                new Intent(this, NotificationService.class)
                                        .setAction("STOP"), 0)).build())
                .setColor(ContextCompat.getColor(this, R.color.colorAccent)).build());

        onHandleIntent(intent);
        return START_STICKY;
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        if (Objects.equals(intent.getAction(), "STOP")) {
            stopSelf();
            return;
        }

        final DirectDownload dd = intent.getParcelableExtra("directDownload");

        final URL url;
        try {
            URL base = dd.getURLAddress();
            URI uri = new URI(base.getProtocol(), null, base.getHost(), base.getPort(), intent.getStringExtra("remotePath"), null, null);
            url = uri.toURL();
        } catch (MalformedURLException | URISyntaxException ex) {
            CommonUtils.logMe(this, ex);
            NotificationGuy.setFailed(notificationId, DownloadService.this, file.getName());
            return;
        }

        final Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (length > 0) {
                    NotificationGuy.updateNotification(notificationId, DownloadService.this, file.getName(), downloaded.floatValue() / length.floatValue() * 100);
                } else {
                    NotificationGuy.setIndeterminate(notificationId, DownloadService.this, file.getName());
                }
            }
        }, 0, 1000);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    if (dd.isAuth()) {
                        conn.setRequestProperty("Authorization", "Basic " + Base64.encodeToString((dd.getUsername() + ":" + dd.getPassword()).getBytes(), Base64.NO_WRAP));
                    }

                    conn.connect();
                    downloaded = 0L;
                    String sLength = conn.getHeaderField("Content-Length");
                    if (sLength == null)
                        length = 0L;
                    else
                        length = Long.parseLong(sLength);

                    InputStream in = conn.getInputStream();
                    FileOutputStream out = new FileOutputStream(file);

                    byte[] buffer = new byte[4096];
                    int count;
                    while ((count = in.read(buffer)) != -1) {
                        out.write(buffer, 0, count);
                        downloaded += count;
                    }

                    out.close();
                    in.close();

                    timer.cancel();
                    timer.purge();

                    NotificationGuy.setCompleted(notificationId, DownloadService.this, file.getName());
                    stopSelf();
                } catch (IOException ex) {
                    timer.cancel();
                    timer.purge();

                    CommonUtils.logMe(DownloadService.this, ex);
                    NotificationGuy.setFailed(notificationId, DownloadService.this, file.getName());
                }
            }
        }).start();
    }

    private static class NotificationGuy {
        private static void updateNotification(int id, Context context, String fileName, Float percentage) {
            notificationManager.notify(id, new NotificationCompat.Builder(context)
                    .setShowWhen(false)
                    .setPriority(Notification.PRIORITY_DEFAULT)
                    .setContentTitle(context.getString(R.string.downloading_file, fileName))
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setProgress(100, percentage.intValue(), false)
                    .setCategory(Notification.CATEGORY_PROGRESS)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(ContextCompat.getColor(context, R.color.colorAccent)).build());
        }

        private static void setIndeterminate(int id, Context context, String fileName) {
            notificationManager.notify(id, new NotificationCompat.Builder(context)
                    .setShowWhen(false)
                    .setPriority(Notification.PRIORITY_DEFAULT)
                    .setContentTitle(context.getString(R.string.downloading_file, fileName))
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setProgress(0, 0, true)
                    .setCategory(Notification.CATEGORY_PROGRESS)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(ContextCompat.getColor(context, R.color.colorAccent)).build());
        }

        private static void setCompleted(int id, Context context, String fileName) {
            notificationManager.notify(id, new NotificationCompat.Builder(context)
                    .setShowWhen(true)
                    .setPriority(Notification.PRIORITY_DEFAULT)
                    .setContentTitle(context.getString(R.string.downloaded_file, fileName))
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setProgress(0, 0, false)
                    .setCategory(Notification.CATEGORY_EVENT)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(ContextCompat.getColor(context, R.color.colorAccent)).build());
        }

        private static void setFailed(int id, Context context, String fileName) {
            notificationManager.notify(id, new NotificationCompat.Builder(context)
                    .setShowWhen(true)
                    .setPriority(Notification.PRIORITY_DEFAULT)
                    .setContentTitle(context.getString(R.string.download_failed, fileName))
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setProgress(0, 0, false)
                    .setCategory(Notification.CATEGORY_EVENT)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(ContextCompat.getColor(context, R.color.colorAccent)).build());
        }
    }
}
