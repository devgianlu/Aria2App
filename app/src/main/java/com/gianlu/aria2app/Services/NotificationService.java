package com.gianlu.aria2app.Services;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.gianlu.aria2app.LoadingActivity;
import com.gianlu.aria2app.NetIO.NetUtils;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NotificationService extends Service {
    private static final int FOREGROUND_SERVICE_NOTIF_ID = 1;
    private static final String CHANNEL_FOREGROUND_SERVICE = "foreground";
    private final Map<String, Integer> errorNotifications = new HashMap<>();
    private List<WebSocket> webSockets;
    private ArrayList<MultiProfile> profiles;
    private WifiManager wifiManager;
    private NotificationManager notificationManager;

    public static void start(Context context) {
        ArrayList<MultiProfile> profiles = new ArrayList<>();
        for (MultiProfile profile : ProfilesManager.get(context).getProfiles())
            if (profile.notificationsEnabled) profiles.add(profile);

        if (profiles.isEmpty()) return;

        context.startService(new Intent(context, NotificationService.class)
                .putExtra("profiles", profiles));
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, NotificationService.class));
    }

    @Override
    @SuppressWarnings("unchecked")
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("profiles")) {
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            profiles = (ArrayList<MultiProfile>) intent.getSerializableExtra("profiles");
            webSockets = new ArrayList<>();

            getApplicationContext().registerReceiver(new ConnectivityChangedReceiver(), new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            recreateWebsockets(ConnectivityManager.TYPE_DUMMY);
            startForeground(FOREGROUND_SERVICE_NOTIF_ID, createForegroundServiceNotification());

            return super.onStartCommand(intent, flags, startId);
        }

        if (profiles != null) profiles.clear();
        if (webSockets != null) webSockets.clear();

        return START_NOT_STICKY; // Process will stop
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createMainChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_FOREGROUND_SERVICE, "Foreground service", NotificationManager.IMPORTANCE_LOW);
        channel.setShowBadge(false);
        notificationManager.createNotificationChannel(channel);
    }

    private Notification createForegroundServiceNotification() {
        createMainChannel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_FOREGROUND_SERVICE);
        builder.setShowWhen(false)
                .setContentTitle(getString(R.string.notificationService))
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentText(CommonUtils.join(profiles, ", "))
                .setCategory(Notification.CATEGORY_SERVICE)
                .setGroup(CHANNEL_FOREGROUND_SERVICE)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent))
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.logo))
                .setContentIntent(PendingIntent.getActivity(this, 1, new Intent(this, LoadingActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));

        // TODO: Stop service from notification

        return builder.build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void handleEvent(MultiProfile.UserProfile profile, String gid, EventType type) {
        // TODO
    }

    private void notifyError(MultiProfile.UserProfile profile, String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_FOREGROUND_SERVICE);
        builder.setContentTitle(title)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setStyle(new NotificationCompat.BigTextStyle().setBigContentTitle(title).bigText(message))
                .setContentText(message)
                .setCategory(Notification.CATEGORY_ERROR)
                .setGroup(CHANNEL_FOREGROUND_SERVICE)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent))
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_error_outline_grey_48dp));

        Integer id = errorNotifications.get(profile.getParent().id);
        if (id == null) {
            id = Utils.random.nextInt();
            errorNotifications.put(profile.getParent().id, id);
        }

        notificationManager.notify(id, builder.build());
    }

    private void notifyUnsupportedConnectionMethod(MultiProfile.UserProfile profile) {
        notifyError(profile, getString(R.string.notificationUnsupportedConnMethod, profile.getProfileName(this)), getString(R.string.notificationUnsupportedConnMethod_details));
    }

    private void notifyException(MultiProfile.UserProfile profile, Exception ex) {
        notifyError(profile, getString(R.string.notificationException, profile.getProfileName(this)), ex.getMessage());
    }

    private void recreateWebsockets(int networkType) {
        for (MultiProfile multi : profiles) {
            MultiProfile.UserProfile profile;
            if (networkType == ConnectivityManager.TYPE_DUMMY)
                profile = multi.getProfile(this);
            else
                profile = multi.getProfile(networkType, wifiManager);

            if (profile.connectionMethod == MultiProfile.ConnectionMethod.HTTP) {
                notifyUnsupportedConnectionMethod(profile);
                continue;
            }

            try {
                WebSocket webSocket = NetUtils.readyWebSocket(profile);
                webSockets.add(webSocket);
                webSocket.addListener(new NotificationsHandler(profile))
                        .connectAsynchronously();
            } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyManagementException | KeyStoreException ex) {
                notifyException(profile, ex);
            }
        }
    }

    public enum EventType {
        DOWNLOAD_START,
        DOWNLOAD_PAUSE,
        DOWNLOAD_STOP,
        DOWNLOAD_COMPLETE,
        DOWNLOAD_ERROR,
        DOWNLOAD_BT_COMPLETE;


        public static EventType parse(String method) {
            switch (method) {
                default: // Shouldn't happen
                case "aria2.onDownloadStart":
                    return DOWNLOAD_START;
                case "aria2.onDownloadPause":
                    return DOWNLOAD_PAUSE;
                case "aria2.onDownloadStop":
                    return DOWNLOAD_STOP;
                case "aria2.onDownloadComplete":
                    return DOWNLOAD_COMPLETE;
                case "aria2.onDownloadError":
                    return DOWNLOAD_ERROR;
                case "aria2.onBtDownloadComplete":
                    return DOWNLOAD_BT_COMPLETE;
            }
        }
    }

    private class ConnectivityChangedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (profiles != null && wifiManager != null && Objects.equals(intent.getAction(), ConnectivityManager.CONNECTIVITY_ACTION)) {
                boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                if (!noConnectivity) {
                    int networkType = intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE, ConnectivityManager.TYPE_DUMMY);
                    if (networkType == ConnectivityManager.TYPE_DUMMY) return;

                    if (webSockets != null) {
                        for (WebSocket webSocket : webSockets) webSocket.disconnect();
                        webSockets.clear();
                    } else {
                        webSockets = new ArrayList<>();
                    }

                    recreateWebsockets(networkType);
                    notificationManager.notify(FOREGROUND_SERVICE_NOTIF_ID, createForegroundServiceNotification());
                }
            }
        }
    }

    private class NotificationsHandler extends WebSocketAdapter {
        private final MultiProfile.UserProfile profile;

        NotificationsHandler(MultiProfile.UserProfile profile) {
            this.profile = profile;
        }

        @Override
        public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
            notifyException(profile, exception);
        }

        @Override
        public void onTextMessage(WebSocket websocket, String text) throws Exception {
            JSONObject json = new JSONObject(text);
            JSONArray events = json.getJSONArray("params");

            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.getJSONObject(i);
                handleEvent(profile, event.getString("gid"), EventType.parse(event.getString("method")));
            }
        }
    }
}
