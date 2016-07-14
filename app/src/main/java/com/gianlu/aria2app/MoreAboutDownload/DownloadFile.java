package com.gianlu.aria2app.MoreAboutDownload;

import android.app.Activity;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Base64;

import com.gianlu.aria2app.Google.UncaughtExceptionHandler;
import com.gianlu.aria2app.MainActivity;
import com.gianlu.aria2app.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;


public class DownloadFile extends IntentService {
    public static volatile boolean _shouldStop;
    public static volatile boolean _shouldPause;
    public static volatile boolean isRunning;
    private static Activity context;
    private static IDownloading handler;
    private int nId = new Random().nextInt(50);

    public DownloadFile() {
        super("Downloading file");
    }

    public static void setContext(Activity context) {
        DownloadFile.context = context;
    }

    public static void setHandler(IDownloading handler) {
        DownloadFile.handler = handler;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        isRunning = true;
        _shouldPause = false;
        _shouldStop = false;

        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(context));

        NotificationManager manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        Notification.Builder builder = new Notification.Builder(context);
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setOngoing(true)
                .setAutoCancel(false)
                .setProgress(0, 0, true)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(context, R.color.colorAccent))
                .setContentTitle("Downloading " + intent.getStringExtra("fileName"));
        manager.notify(nId, builder.build());

        startForeground(nId, builder.build());

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "File download");
        mWakeLock.acquire();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        try {
            URL url = new URL(intent.getStringExtra("url"));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            try {
                connection.setRequestProperty("User-Agent", "Aria2App " + context.getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
            } catch (PackageManager.NameNotFoundException ex) {
                connection.setRequestProperty("User-Agent", "Aria2App > 1.3");
            }
            connection.setRequestProperty("Connection", "keep-alive");

            if (sharedPreferences.getBoolean("dd_auth", false)) {
                String basicAuth = "Basic " + Base64.encodeToString((sharedPreferences.getString("dd_user", "myusername") + ":" + sharedPreferences.getString("dd_passwd", "mypassword")).getBytes(), Base64.NO_WRAP);
                connection.setRequestProperty("Authorization", basicAuth);
            }

            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                handler.onConnectionError(connection.getResponseCode(), connection.getResponseMessage());
                mWakeLock.release();
                return;
            }

            Long fileLength = Long.parseLong(connection.getHeaderField("Content-Length"));

            InputStream in = connection.getInputStream();
            String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + intent.getStringExtra("fileName");
            OutputStream out = new FileOutputStream(filePath);

            handler.onStart(fileLength);

            byte data[] = new byte[4096];
            Long downloaded = 0L;
            int count;
            while ((count = in.read(data)) != -1) {
                if (_shouldStop) {
                    out.close();
                    in.close();
                    manager.cancel(nId);
                    handler.onUserStopped(new File(filePath).delete());
                    mWakeLock.release();
                    stopSelf();
                    return;
                }

                if (_shouldPause) {
                    return;
                }

                downloaded += count;

                builder.setProgress(100, (int) (downloaded.floatValue() / fileLength.floatValue() * 100), false);
                manager.notify(nId, builder.build());
                handler.publishProgress(downloaded);

                out.write(data, 0, count);
            }

            out.close();
            in.close();

            handler.onComplete();
            NotificationCompat.Builder endBuilder = new NotificationCompat.Builder(context);
            endBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setCategory(NotificationCompat.CATEGORY_EVENT)
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(ContextCompat.getColor(context, R.color.colorAccent))
                    .setContentTitle(getString(R.string.downloadCompleted))
                    .setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), MainActivity.class).putExtra("gid", intent.getStringExtra("gid")), PendingIntent.FLAG_UPDATE_CURRENT));
            manager.notify(new Random().nextInt(1000), endBuilder.build());
        } catch (IOException ex) {
            handler.onException(ex);
            NotificationCompat.Builder endBuilder = new NotificationCompat.Builder(context);
            endBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setCategory(NotificationCompat.CATEGORY_EVENT)
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(ContextCompat.getColor(context, R.color.colorAccent))
                    .setContentTitle(getString(R.string.downloadFailed))
                    .setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), MainActivity.class).putExtra("gid", intent.getStringExtra("gid")), PendingIntent.FLAG_UPDATE_CURRENT));
            manager.notify(new Random().nextInt(1000), endBuilder.build());
        } finally {
            isRunning = false;
            if (mWakeLock.isHeld())
                mWakeLock.release();
        }
    }

    public interface IDownloading {
        void onStart(Long fileLength);

        void onComplete();

        void onUserStopped(boolean fileDeleted);

        void publishProgress(long downloaded);

        void onException(Exception ex);

        void onConnectionError(int respCode, String respMessage);
    }
}
