package com.gianlu.aria2app.Services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.gianlu.aria2app.LoadingActivity;
import com.gianlu.aria2app.NetIO.NetUtils;
import com.gianlu.aria2app.PKeys;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Preferences.Prefs;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class NotificationService extends Service {
    public static final String ACTION_STOPPED = "com.gianlu.aria2app.notifs.STOPPED";
    public static final String ACTION_IS_NOTIFICABLE = "com.gianlu.aria2app.notifs.IS_NOTIFICABLE";
    public static final String ACTION_TOGGLE_NOTIFICABLE = "com.gianlu.aria2app.notifs.TOGGLE_NOTIFICABLE";
    public static final int MESSENGER_IS_NOTIFICABLE = 0;
    public static final int MESSENGER_TOGGLE_NOTIFICABLE = 1;
    private static final int FOREGROUND_SERVICE_NOTIF_ID = 1;
    private static final String CHANNEL_FOREGROUND_SERVICE = "foreground";
    private static final String ACTION_STOP = "com.gianlu.aria2app.notifs.STOP";
    private static final java.lang.String SERVICE_NAME = "aria2app notification service";
    private final Map<String, Integer> errorNotifications = new HashMap<>();
    private final HandlerThread serviceThread = new HandlerThread(SERVICE_NAME);
    private final List<String> notificableDownloads = new ArrayList<>();
    private List<WebSocket> webSockets;
    private ArrayList<MultiProfile> profiles;
    private WifiManager wifiManager;
    private NotificationManager notificationManager;
    private Messenger messenger;
    private LocalBroadcastManager broadcastManager;
    private Boolean startedNotificable = null;

    public static void toggleNotification(final Context context, final LocalBroadcastManager broadcastManager, final String gid, final OnIsNotificable listener) {
        final Object waitToUnbind = new Object();

        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Objects.equals(intent.getAction(), ACTION_TOGGLE_NOTIFICABLE)) {
                    boolean has = intent.getBooleanExtra("has", false);
                    listener.onResult(has);

                    if (has) {
                        start(context, true);
                    } else {
                        if (intent.getBooleanExtra("shouldStop", false))
                            stop(context);
                    }

                    broadcastManager.unregisterReceiver(this);

                    synchronized (waitToUnbind) {
                        waitToUnbind.notify();
                    }
                }
            }
        };

        context.bindService(new Intent(context, NotificationService.class), new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Messenger messenger = new Messenger(service);

                broadcastManager.registerReceiver(receiver, new IntentFilter(ACTION_TOGGLE_NOTIFICABLE));

                try {
                    messenger.send(Message.obtain(null, MESSENGER_TOGGLE_NOTIFICABLE, gid));
                } catch (RemoteException ex) {
                    Logging.log(ex);
                    listener.onResult(false);
                }

                synchronized (waitToUnbind) {
                    try {
                        waitToUnbind.wait();
                        context.unbindService(this);
                    } catch (InterruptedException ex) {
                        Logging.log(ex);
                    }
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                broadcastManager.unregisterReceiver(receiver);
            }
        }, BIND_AUTO_CREATE);
    }

    public static void start(Context context, boolean notificable) {
        ArrayList<MultiProfile> profiles = new ArrayList<>();
        for (MultiProfile profile : ProfilesManager.get(context).getProfiles())
            if (profile.notificationsEnabled) profiles.add(profile);

        if (profiles.isEmpty()) return;

        context.startService(new Intent(context, NotificationService.class)
                .putExtra("notificable", notificable)
                .putExtra("profiles", profiles));
    }

    public static void stop(Context context) {
        context.startService(new Intent(context, NotificationService.class).setAction(ACTION_STOP));
    }

    @Override
    @SuppressWarnings("unchecked")
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        broadcastManager = LocalBroadcastManager.getInstance(this);

        if (intent != null) {
            if (startedNotificable == null)
                startedNotificable = intent.getBooleanExtra("notificable", false);

            if (Objects.equals(intent.getAction(), ACTION_STOP)) {
                stopForeground(true);
                stopSelf();
            } else if (intent.hasExtra("profiles")) {
                notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                profiles = (ArrayList<MultiProfile>) intent.getSerializableExtra("profiles");

                createMainChannel();
                createEventsChannels();

                recreateWebsockets(ConnectivityManager.TYPE_DUMMY);
                getApplicationContext().registerReceiver(new ConnectivityChangedReceiver(), new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
                startForeground(FOREGROUND_SERVICE_NOTIF_ID, createForegroundServiceNotification());

                return super.onStartCommand(intent, flags, startId);
            }
        }

        notificableDownloads.clear();
        if (profiles != null) profiles.clear();
        if (webSockets != null) {
            for (WebSocket webSocket : webSockets) {
                webSocket.clearListeners();
                webSocket.disconnect();
            }

            webSockets.clear();
        }

        broadcastManager.sendBroadcastSync(new Intent(ACTION_STOPPED));

        return START_NOT_STICKY; // Process will stop
    }

    private String describeServiceStatus() {
        if (startedNotificable)
            return CommonUtils.join(profiles, ", ") + " for " + CommonUtils.join(notificableDownloads, ", ");
        else
            return CommonUtils.join(profiles, ", ");
    }

    private Notification createForegroundServiceNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_FOREGROUND_SERVICE);
        builder.setShowWhen(false)
                .setContentTitle(getString(R.string.notificationService))
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentText(describeServiceStatus())
                .setCategory(Notification.CATEGORY_SERVICE)
                .setGroup(CHANNEL_FOREGROUND_SERVICE)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent))
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.logo))
                .addAction(new NotificationCompat.Action(R.drawable.ic_clear_black_48dp, getString(R.string.stopNotificationService), PendingIntent.getService(this, 1, new Intent(this, NotificationService.class).setAction(ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT)))
                .setContentIntent(PendingIntent.getActivity(this, 1, new Intent(this, LoadingActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));

        return builder.build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (messenger == null) {
            serviceThread.start();
            broadcastManager = LocalBroadcastManager.getInstance(this);
            messenger = new Messenger(new ServiceHandler());
        }

        return messenger.getBinder();
    }

    private void handleEvent(MultiProfile.UserProfile profile, String gid, EventType type) {
        if (startedNotificable && !notificableDownloads.contains(gid)) return;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, type.channelName());
        builder.setContentTitle(type.getFormal(this))
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentText("GID#" + gid)
                .setContentInfo(profile.getProfileName(this))
                .setCategory(Notification.CATEGORY_EVENT)
                .setGroup(gid)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_new_releases_grey_48dp))
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));

        Bundle bundle = new Bundle();
        bundle.putBoolean("fromNotification", true);
        bundle.putString("profileId", profile.getParent().id);
        bundle.putString("gid", gid);
        builder.setContentIntent(PendingIntent.getActivity(this, 1, new Intent(this, LoadingActivity.class)
                .putExtras(bundle), PendingIntent.FLAG_UPDATE_CURRENT));

        notificationManager.notify(ThreadLocalRandom.current().nextInt(), builder.build());
    }

    private void notifyError(MultiProfile.UserProfile profile, String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_FOREGROUND_SERVICE);
        builder.setContentTitle(title)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setStyle(new NotificationCompat.BigTextStyle().setBigContentTitle(title).bigText(message))
                .setContentText(message)
                .setCategory(Notification.CATEGORY_ERROR)
                .setGroup(CHANNEL_FOREGROUND_SERVICE)
                .setSmallIcon(R.drawable.ic_notification) // FIXME: "Notification icons must be entirely white"
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent))
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_error_outline_grey_48dp));

        Integer id = errorNotifications.get(profile.getParent().id);
        if (id == null) {
            id = ThreadLocalRandom.current().nextInt();
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
        if (webSockets != null) {
            for (WebSocket webSocket : webSockets) {
                webSocket.clearListeners();
                webSocket.disconnect();
            }

            webSockets.clear();
        } else {
            webSockets = new ArrayList<>();
        }

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
            } catch (IOException | NoSuchAlgorithmException | NetUtils.InvalidUrlException | CertificateException | KeyManagementException | KeyStoreException ex) {
                notifyException(profile, ex);
            }
        }
    }

    private void createMainChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_FOREGROUND_SERVICE, "Foreground service", NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(false);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void createEventsChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            for (EventType type : EventType.values()) {
                NotificationChannel channel = new NotificationChannel(type.channelName(), type.getFormal(this), NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public enum EventType {
        DOWNLOAD_START("START"),
        DOWNLOAD_PAUSE("PAUSE"),
        DOWNLOAD_STOP("STOP"),
        DOWNLOAD_COMPLETE("COMPLETE"),
        DOWNLOAD_ERROR("ERROR"),
        DOWNLOAD_BT_COMPLETE("BTCOMPLETE");

        private final String prefValue;

        EventType(String prefValue) {
            this.prefValue = prefValue;
        }

        public static List<EventType> parseFromPrefs(Set<String> set) {
            List<EventType> types = new ArrayList<>();
            for (String name : set) {
                EventType type = parseFromPrefs(name);
                if (type != null && !types.contains(type)) types.add(type);
            }

            return types;
        }

        @Nullable
        public static EventType parseFromPrefs(String name) {
            for (EventType type : values())
                if (Objects.equals(type.prefValue, name))
                    return type;

            return null;
        }

        public static Set<String> prefsValues() {
            Set<String> set = new HashSet<>();
            for (EventType type : values()) set.add(type.prefValue);
            return set;
        }

        @Nullable
        public static EventType parse(String method) {
            switch (method) {
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
                default:
                    return null;
            }
        }

        public String channelName() {
            return name().toLowerCase();
        }

        public String getFormal(Context context) {
            switch (this) {
                case DOWNLOAD_START:
                    return context.getString(R.string.notificationStarted);
                case DOWNLOAD_PAUSE:
                    return context.getString(R.string.notificationPaused);
                case DOWNLOAD_STOP:
                    return context.getString(R.string.notificationStopped);
                case DOWNLOAD_COMPLETE:
                    return context.getString(R.string.notificationComplete);
                case DOWNLOAD_ERROR:
                    return context.getString(R.string.notificationError);
                case DOWNLOAD_BT_COMPLETE:
                    return context.getString(R.string.notificationBTComplete);
                default:
                    return context.getString(R.string.unknown);
            }
        }
    }

    public interface OnIsNotificable {
        void onResult(boolean notificable);
    }

    @SuppressLint("HandlerLeak")
    private class ServiceHandler extends Handler {

        ServiceHandler() {
            super(serviceThread.getLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            String gid = (String) msg.obj;

            switch (msg.what) {
                case MESSENGER_TOGGLE_NOTIFICABLE:
                    boolean hasBefore = notificableDownloads.contains(gid);
                    if (hasBefore) {
                        notificableDownloads.remove(gid);
                        broadcastManager.sendBroadcastSync(new Intent(ACTION_TOGGLE_NOTIFICABLE)
                                .putExtra("has", startedNotificable != null && !startedNotificable) // || false
                                .putExtra("shouldStop", notificableDownloads.isEmpty() && startedNotificable != null && startedNotificable));
                    } else {
                        notificableDownloads.add(gid);
                        broadcastManager.sendBroadcastSync(new Intent(ACTION_TOGGLE_NOTIFICABLE)
                                .putExtra("has", true));
                    }
                    break;
                case MESSENGER_IS_NOTIFICABLE:
                    broadcastManager.sendBroadcastSync(new Intent(ACTION_IS_NOTIFICABLE)
                            .putExtra("gid", gid)
                            .putExtra("notificable", (startedNotificable != null && !startedNotificable) || notificableDownloads.contains(gid)));
                    break;
                default:
                    super.handleMessage(msg);
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

                    recreateWebsockets(networkType);
                    notificationManager.notify(FOREGROUND_SERVICE_NOTIF_ID, createForegroundServiceNotification());
                }
            }
        }
    }

    private class NotificationsHandler extends WebSocketAdapter {
        private final MultiProfile.UserProfile profile;
        private final List<EventType> enabledNotifs;

        NotificationsHandler(MultiProfile.UserProfile profile) {
            this.profile = profile;
            this.enabledNotifs = EventType.parseFromPrefs(Prefs.getSet(NotificationService.this, PKeys.A2_SELECTED_NOTIFS_TYPE, EventType.prefsValues()));
        }

        @Override
        public void onConnectError(WebSocket websocket, WebSocketException exception) {
            notifyException(profile, exception);
        }

        @Override
        public void handleCallbackError(WebSocket websocket, Throwable cause) {
            Logging.log(cause);
        }

        @Override
        public void onTextMessage(WebSocket websocket, String text) throws Exception {
            JSONObject json = new JSONObject(text);
            EventType type = EventType.parse(json.getString("method"));
            if (enabledNotifs.contains(type)) {
                JSONArray events = json.getJSONArray("params");
                for (int i = 0; i < events.length(); i++) {
                    JSONObject event = events.getJSONObject(i);
                    handleEvent(profile, event.getString("gid"), type);
                }
            }
        }
    }
}
