package com.gianlu.aria2app.Services;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.util.Base64;

import com.gianlu.aria2app.Main.Profile.DirectDownload;
import com.gianlu.aria2app.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadService extends IntentService {
    private AtomicLong downloaded = new AtomicLong(0);
    private AtomicLong length = new AtomicLong(0);
    private NotificationManagerCompat notificationManager;
    private int notificationId;
    private File file;

    public DownloadService() {
        super("DownloadService");
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
                .setContentTitle("Downloading " + file.getName())
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setProgress(100, 0, false)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent)).build());

        onHandleIntent(intent);
        return START_STICKY;
    }

    private void updateNotification(Float percentage) {
        notificationManager.notify(notificationId, new NotificationCompat.Builder(this)
                .setShowWhen(false)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setContentTitle("Downloading: " + file.getName())
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setProgress(100, percentage.intValue(), false)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent)).build());
    }

    private void setFinished() {
        notificationManager.notify(notificationId, new NotificationCompat.Builder(this)
                .setShowWhen(true)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setContentTitle("Download finished: " + file.getName())
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setProgress(0, 0, true)
                .setCategory(Notification.CATEGORY_EVENT)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(PendingIntent.getActivity(this,
                        new Random().nextInt(100),
                        new Intent(Intent.ACTION_VIEW)
                                .setDataAndType(
                                        Uri.fromFile(file),
                                        URLConnection.guessContentTypeFromName(file.getName()))
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent)).build());
    }

    private void setFailed(Exception ex) {
        notificationManager.notify(notificationId, new NotificationCompat.Builder(this)
                .setShowWhen(true)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setContentTitle("Download failed: " + file.getName())
                .setContentText(ex.getMessage())
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setProgress(0, 0, true)
                .setCategory(Notification.CATEGORY_EVENT)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent)).build());
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        final DirectDownload dd = intent.getParcelableExtra("directDownload");

        final URL url;
        try {
            URL base = dd.getURLAddress();
            URI uri = new URI(base.getProtocol(), null, base.getHost(), base.getPort(), intent.getStringExtra("remotePath"), null, null);
            url = uri.toURL();
        } catch (MalformedURLException | URISyntaxException ex) {
            setFailed(ex);
            return;
        }

        final Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (length.get() != 0) {
                    updateNotification(downloaded.floatValue() / length.floatValue() * 100);
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
                    downloaded.set(0);
                    length.set(Long.parseLong(conn.getHeaderField("Content-Length")));

                    InputStream in = conn.getInputStream();
                    FileOutputStream out = new FileOutputStream(file);

                    byte[] buffer = new byte[4096];
                    int count;
                    while ((count = in.read(buffer)) != -1) {
                        out.write(buffer, 0, count);
                        downloaded.getAndAdd(count);
                    }

                    out.close();
                    in.close();

                    timer.purge();
                    setFinished();
                    stopSelf();
                } catch (IOException ex) {
                    setFailed(ex);
                }
            }
        }).start();
    }
}
