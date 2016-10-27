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
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

// TODO: Some dialogs to display progress and speed
public class DownloadService extends IntentService {
    private NotificationManagerCompat notificationManager;
    private Long downloaded = 0L;
    private boolean _shouldStop = false;
    private Long length = 0L;
    private int notificationId = new Random().nextInt();
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

    private NotificationCompat.Builder defaultBuilder() {
        return new NotificationCompat.Builder(this)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(this, R.color.colorAccent));
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        notificationManager = NotificationManagerCompat.from(this);

        if (intent == null || Objects.equals(intent.getAction(), "STOP")) {
            stopSelf();
        } else {
            file = (File) intent.getSerializableExtra("file");

            startForeground(notificationId, defaultBuilder()
                    .setShowWhen(true)
                    .setContentInfo(null)
                    .setCategory(Notification.CATEGORY_PROGRESS)
                    .setContentTitle(getString(R.string.downloading_file, file.getName()))
                    .setProgress(0, 0, true)
                    .build());
        }

        onHandleIntent(intent);
        return START_STICKY;
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        System.out.println("ACT: " + intent.getAction());

        if (Objects.equals(intent.getAction(), "STOP")) {
            stopSelf();
            _shouldStop = true;
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
            setFailed();
            return;
        }

        final Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (length > 0) {
                    updateNotification(downloaded.floatValue() / length.floatValue() * 100);
                } else {
                    setIndeterminate();
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
                    while ((count = in.read(buffer)) != -1 && !_shouldStop) {
                        out.write(buffer, 0, count);
                        downloaded += count;
                    }

                    out.close();
                    in.close();

                    timer.cancel();
                    timer.purge();

                    if (_shouldStop) {
                        notificationManager.cancel(notificationId);
                        //noinspection ResultOfMethodCallIgnored
                        file.delete();
                    } else {
                        setCompleted();
                    }

                    stopSelf();
                } catch (IOException ex) {
                    timer.cancel();
                    timer.purge();

                    CommonUtils.logMe(DownloadService.this, ex);
                    setFailed();
                }
            }
        }).start();
    }

    private void updateNotification(Float percentage) {
        notificationManager.notify(notificationId, defaultBuilder()
                .setShowWhen(false)
                .setContentTitle(file.getName())
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setContentInfo(String.format(Locale.getDefault(), "%.2f", percentage) + " %")
                .setProgress(100, percentage.intValue(), false)
                .addAction(new NotificationCompat.Action.Builder(
                        R.drawable.ic_clear_black_48dp,
                        getString(R.string.stopDownload),
                        PendingIntent.getService(this, new Random().nextInt(),
                                new Intent(this, DownloadService.class)
                                        .setAction("STOP"), 0)).build())
                .build());
    }

    private void setIndeterminate() {
        notificationManager.notify(notificationId, defaultBuilder()
                .setShowWhen(false)
                .setContentTitle(file.getName())
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setContentInfo(null)
                .setProgress(0, 0, true)
                .addAction(new NotificationCompat.Action.Builder(
                        R.drawable.ic_clear_black_48dp,
                        getString(R.string.stopDownload),
                        PendingIntent.getService(this, new Random().nextInt(),
                                new Intent(this, DownloadService.class)
                                        .setAction("STOP"), 0)).build())
                .build());
    }

    private void setCompleted() {
        notificationManager.notify(notificationId, defaultBuilder()
                .setShowWhen(true)
                .setAutoCancel(true)
                .setContentInfo(null)
                .setContentTitle(getString(R.string.downloaded_file, file.getName()))
                .setProgress(0, 0, false)
                .setCategory(Notification.CATEGORY_EVENT).build());
    }

    private void setFailed() {
        notificationManager.notify(notificationId, defaultBuilder()
                .setShowWhen(true)
                .setAutoCancel(true)
                .setContentInfo(null)
                .setContentTitle(getString(R.string.download_failed, file.getName()))
                .setProgress(0, 0, false)
                .setCategory(Notification.CATEGORY_EVENT)
                .build());
    }
}
