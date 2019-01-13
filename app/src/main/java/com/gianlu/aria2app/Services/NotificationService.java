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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.gianlu.aria2app.LoadingActivity;
import com.gianlu.aria2app.NetIO.NetUtils;
import com.gianlu.aria2app.PK;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.ThisApplication;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Preferences.Prefs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.collection.ArraySet;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class NotificationService extends Service {
    public static final String EVENT_STOPPED = NotificationService.class.getName() + ".STOPPED";
    public static final String EVENT_GET_MODE = NotificationService.class.getName() + ".GET_MODE";
    private static final int MESSENGER_GET_MODE = 0;
    private static final int MESSENGER_SET_MODE = 1;
    private static final int FOREGROUND_SERVICE_NOTIF_ID = 1;
    private static final String CHANNEL_FOREGROUND_SERVICE = "foreground";
    private static final String ACTION_START = NotificationService.class.getName() + ".START";
    private static final String ACTION_STOP = NotificationService.class.getName() + ".STOP";
    private static final String SERVICE_NAME = "aria2app notification service";
    private final Map<String, Integer> errorNotifications = new HashMap<>();
    private final HandlerThread serviceThread = new HandlerThread(SERVICE_NAME);
    private final Map<String, Mode> gidToMode = new HashMap<>();
    private Set<WebSocket> webSockets;
    private List<MultiProfile> profiles;
    private WifiManager wifiManager;
    private NotificationManager notificationManager;
    private Messenger messenger;
    private LocalBroadcastManager broadcastManager;
    private StartedFrom startedFrom = StartedFrom.NOT;
    private ConnectivityChangedReceiver connectivityChangedReceiver;

    private static void debug(String msg) {
        if (CommonUtils.isDebug() && ThisApplication.DEBUG_NOTIFICATION)
            System.out.println(NotificationService.class.getSimpleName() + ": " + msg);
    }

    public static void setMode(@NonNull Context context, @NonNull String gid, @NonNull Mode mode) {
        debug("Called set mode for " + gid + ", mode=" + mode);

        startInternal(context, StartedFrom.DOWNLOAD);
        context.bindService(new Intent(context, NotificationService.class), new ToggleNotificationHelper(context, gid, mode), BIND_AUTO_CREATE);
    }

    public static void getMode(@NonNull Messenger messenger, @NonNull String gid) {
        debug("Called get mode for " + gid);

        try {
            messenger.send(Message.obtain(null, NotificationService.MESSENGER_GET_MODE, new MessengerPayload(gid, null)));
        } catch (RemoteException ex) {
            Logging.log(ex);
        }
    }

    private static void startInternal(@NonNull Context context, @NonNull StartedFrom startedFrom) {
        debug("Called start service, startedFrom=" + startedFrom);
        if (ProfilesManager.get(context).hasNotificationProfiles()) {
            try {
                ContextCompat.startForegroundService(context, new Intent(context, NotificationService.class)
                        .setAction(ACTION_START).putExtra("startedFrom", startedFrom));
            } catch (SecurityException ex) {
                Logging.log("Cannot start notification service.", ex);
            }
        } else {
            Logging.log("Tried to start notification service, but there are no candidates.", false);
        }
    }

    public static void start(@NonNull Context context) {
        startInternal(context, StartedFrom.GLOBAL);
    }

    @NonNull
    private static List<MultiProfile> loadProfiles(@NonNull Context context) {
        return ProfilesManager.get(context).getNotificationProfiles();
    }

    public static void stop(@NonNull Context context) {
        debug("Called stop service");
        context.startService(new Intent(context, NotificationService.class).setAction(ACTION_STOP));
    }

    private void clearWebsockets() {
        if (webSockets != null) {
            for (WebSocket webSocket : webSockets) webSocket.close(1000, null);
            webSockets.clear();
        } else {
            webSockets = new ArraySet<>(4);
        }
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        broadcastManager = LocalBroadcastManager.getInstance(this);

        if (intent != null) {
            if (Objects.equals(intent.getAction(), ACTION_STOP)) {
                startedFrom = StartedFrom.NOT;
                stopForeground(true);
                stopSelf();
            } else if (Objects.equals(intent.getAction(), ACTION_START)) {
                if (startedFrom == StartedFrom.NOT) {
                    notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                    profiles = loadProfiles(this);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        createMainChannel();
                        createEventsChannels();
                    }

                    recreateWebsockets(ConnectivityManager.TYPE_DUMMY);
                    connectivityChangedReceiver = new ConnectivityChangedReceiver();
                    registerReceiver(connectivityChangedReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
                    startForeground(FOREGROUND_SERVICE_NOTIF_ID, createForegroundServiceNotification());
                }

                if (startedFrom == StartedFrom.NOT || startedFrom == StartedFrom.DOWNLOAD)
                    startedFrom = (StartedFrom) intent.getSerializableExtra("startedFrom");

                return super.onStartCommand(intent, flags, startId);
            }
        }

        gidToMode.clear();
        if (profiles != null) profiles.clear();
        clearWebsockets();
        broadcastManager.sendBroadcast(new Intent(EVENT_STOPPED));
        stopSelf();

        return START_NOT_STICKY; // Process will stop
    }

    @NonNull
    private List<String> getByMode(@NonNull Mode mode) {
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, Mode> entry : gidToMode.entrySet())
            if (entry.getValue() == mode) list.add(entry.getKey());
        return list;
    }

    private boolean isEmptyByMode(@NonNull Mode mode) {
        for (Map.Entry<String, Mode> entry : gidToMode.entrySet())
            if (entry.getValue() == mode) return false;

        return true;
    }

    @NonNull
    private String describeServiceStatus() {
        switch (startedFrom) {
            case GLOBAL:
                List<String> notNotify = getByMode(Mode.NOT_NOTIFY_EXCLUSIVE);
                if (notNotify.isEmpty())
                    return CommonUtils.join(profiles, ", ");
                else
                    return CommonUtils.join(profiles, ", ") + " except " + CommonUtils.join(notNotify, ", ");
            case DOWNLOAD:
                List<String> notify = getByMode(Mode.NOTIFY_EXCLUSIVE);
                if (notify.isEmpty())
                    return "Should stop, not notifying anything.";
                else
                    return CommonUtils.join(profiles, ", ") + " for " + CommonUtils.join(notify, ", ");
            default:
            case NOT:
                return "Not started";
        }
    }

    private void updateForegroundNotification() {
        if (startedFrom != StartedFrom.NOT)
            notificationManager.notify(FOREGROUND_SERVICE_NOTIF_ID, createForegroundServiceNotification());
    }

    @NonNull
    private Notification createForegroundServiceNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_FOREGROUND_SERVICE);
        builder.setShowWhen(false)
                .setContentTitle(getString(R.string.notificationService))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentText(describeServiceStatus())
                .setCategory(Notification.CATEGORY_SERVICE)
                .setGroup(CHANNEL_FOREGROUND_SERVICE)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setColor(ContextCompat.getColor(this, R.color.colorAccent))
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                .addAction(new NotificationCompat.Action(R.drawable.baseline_clear_24, getString(R.string.stopNotificationService), PendingIntent.getService(this, 1, new Intent(this, NotificationService.class).setAction(ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT)))
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

    @NonNull
    private Mode getMode(@NonNull String gid) {
        Mode mode = gidToMode.get(gid);
        return mode == null ? (startedFrom == StartedFrom.NOT ? Mode.NOT_NOTIFY_STANDARD : Mode.NOTIFY_STANDARD) : mode;
    }

    private void handleEvent(@NonNull MultiProfile.UserProfile profile, @NonNull String gid, @NonNull EventType type) {
        Mode mode = getMode(gid);
        switch (startedFrom) {
            default:
            case NOT:
                return;
            case GLOBAL:
                if (mode == Mode.NOT_NOTIFY_EXCLUSIVE) return;
                break;
            case DOWNLOAD:
                if (mode != Mode.NOTIFY_EXCLUSIVE) return;
                break;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, type.channelName());
        builder.setContentTitle(type.getFormal(this))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentText("GID#" + gid)
                .setContentInfo(profile.getPrimaryText(this))
                .setCategory(Notification.CATEGORY_EVENT)
                .setGroup(gid)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_new_releases_grey_48dp))
                .setColor(ContextCompat.getColor(this, R.color.colorAccent));

        Bundle bundle = new Bundle();
        bundle.putString("profileId", profile.getParent().id);
        bundle.putString("gid", gid);
        builder.setContentIntent(PendingIntent.getActivity(this, 1, new Intent(this, LoadingActivity.class)
                .putExtras(bundle), PendingIntent.FLAG_UPDATE_CURRENT));

        notificationManager.notify(ThreadLocalRandom.current().nextInt(), builder.build());
    }

    private void notifyError(@NonNull MultiProfile.UserProfile profile, @NonNull String title, @NonNull String message) {
        if (startedFrom == StartedFrom.NOT) return;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_FOREGROUND_SERVICE);
        builder.setContentTitle(title)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new NotificationCompat.BigTextStyle().setBigContentTitle(title).bigText(message))
                .setContentText(message)
                .setCategory(Notification.CATEGORY_ERROR)
                .setGroup(CHANNEL_FOREGROUND_SERVICE)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setColor(ContextCompat.getColor(this, R.color.colorAccent))
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_error_outline_grey_48dp));

        Integer id = errorNotifications.get(profile.getParent().id);
        if (id == null) {
            id = ThreadLocalRandom.current().nextInt();
            errorNotifications.put(profile.getParent().id, id);
        }

        notificationManager.notify(id, builder.build());
    }

    private void notifyUnsupportedConnectionMethod(@NonNull MultiProfile.UserProfile profile) {
        notifyError(profile, getString(R.string.notificationUnsupportedConnMethod, profile.getPrimaryText(this)), getString(R.string.notificationUnsupportedConnMethod_details));
    }

    private void notifyException(@NonNull MultiProfile.UserProfile profile, @NonNull Throwable ex) {
        Logging.log(ex);
        notifyError(profile, getString(R.string.notificationException, profile.getPrimaryText(this)), ex.getMessage());
    }

    private void recreateWebsockets(int networkType) {
        clearWebsockets();

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
                webSockets.add(NetUtils.buildClient(profile).newWebSocket(NetUtils.createWebsocketRequest(profile), new NotificationsHandler(profile)));
            } catch (IOException | NetUtils.InvalidUrlException | GeneralSecurityException ex) {
                notifyException(profile, ex);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createMainChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_FOREGROUND_SERVICE, "Foreground service", NotificationManager.IMPORTANCE_LOW);
        channel.setShowBadge(false);
        notificationManager.createNotificationChannel(channel);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createEventsChannels() {
        for (EventType type : EventType.values()) {
            NotificationChannel channel = new NotificationChannel(type.channelName(), type.getFormal(this), NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        startedFrom = StartedFrom.NOT;
        if (connectivityChangedReceiver != null)
            unregisterReceiver(connectivityChangedReceiver);
    }

    private enum StartedFrom {
        GLOBAL,
        DOWNLOAD,
        NOT
    }

    public enum Mode {
        /**
         * Requested to be notified
         */
        NOTIFY_EXCLUSIVE,
        /**
         * Notified by default (aka notification service enabled)
         */
        NOTIFY_STANDARD,
        /**
         * Requested not to be notified
         */
        NOT_NOTIFY_EXCLUSIVE,
        /**
         * Not notified by default (aka notification service disabled)
         */
        NOT_NOTIFY_STANDARD
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

        @NonNull
        public static List<EventType> parseFromPrefs(@NonNull Set<String> set) {
            List<EventType> types = new ArrayList<>();
            for (String name : set) {
                EventType type = parseFromPrefs(name);
                if (type != null && !types.contains(type)) types.add(type);
            }

            return types;
        }

        @Nullable
        public static EventType parseFromPrefs(@NonNull String name) {
            for (EventType type : values())
                if (Objects.equals(type.prefValue, name))
                    return type;

            return null;
        }

        @NonNull
        public static String[] prefsValues() {
            EventType[] values = values();
            String[] array = new String[values.length];
            for (int i = 0; i < values.length; i++)
                array[i] = values[i].prefValue;
            return array;
        }

        @NonNull
        public static String[] formalValues(Context context) {
            EventType[] values = values();
            String[] array = new String[values.length];
            for (int i = 0; i < values.length; i++)
                array[i] = values[i].getFormal(context);
            return array;
        }

        @Nullable
        public static EventType parse(@NonNull String method) {
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

        @NonNull
        public String channelName() {
            return name().toLowerCase();
        }

        @NonNull
        public String getFormal(@NonNull Context context) {
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

    private static class ToggleNotificationHelper implements ServiceConnection {
        private final Context context;
        private final MessengerPayload payload;

        private ToggleNotificationHelper(@NonNull Context context, @NonNull String gid, @NonNull Mode mode) {
            this.context = context;
            this.payload = new MessengerPayload(gid, mode);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Messenger messenger = new Messenger(service);

            try {
                messenger.send(Message.obtain(null, MESSENGER_SET_MODE, payload));
            } catch (RemoteException ex) {
                Logging.log(ex);
            }

            context.unbindService(this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }

    private static class MessengerPayload {
        private final String gid;
        private final Mode mode;

        private MessengerPayload(@NonNull String gid, @Nullable Mode mode) {
            this.gid = gid;
            this.mode = mode;
        }
    }

    @SuppressLint("HandlerLeak")
    private class ServiceHandler extends Handler {

        ServiceHandler() {
            super(serviceThread.getLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            MessengerPayload payload = (MessengerPayload) msg.obj;

            switch (msg.what) {
                case MESSENGER_SET_MODE:
                    switch (startedFrom) {
                        case GLOBAL:
                            if (payload.mode == Mode.NOTIFY_EXCLUSIVE) break;
                            gidToMode.put(payload.gid, payload.mode);
                            break;
                        case DOWNLOAD:
                            if (payload.mode == Mode.NOT_NOTIFY_EXCLUSIVE) break;
                            gidToMode.put(payload.gid, payload.mode);
                            if (isEmptyByMode(Mode.NOTIFY_EXCLUSIVE)) {
                                broadcastManager.sendBroadcast(new Intent(EVENT_STOPPED));
                                stopForeground(true);
                                stopSelf();
                                startedFrom = StartedFrom.NOT;
                            }
                            break;
                        case NOT:
                            break;
                    }

                    updateForegroundNotification();

                    // Purposely missing break
                case MESSENGER_GET_MODE:
                    broadcastManager.sendBroadcast(new Intent(EVENT_GET_MODE)
                            .putExtra("gid", payload.gid)
                            .putExtra("mode", getMode(payload.gid)));
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private class ConnectivityChangedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (profiles != null && wifiManager != null && Objects.equals(intent.getAction(), ConnectivityManager.CONNECTIVITY_ACTION)) {
                boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                if (!noConnectivity) {
                    int networkType = intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE, ConnectivityManager.TYPE_DUMMY);
                    if (networkType == ConnectivityManager.TYPE_DUMMY) return;

                    recreateWebsockets(networkType);
                    updateForegroundNotification();
                }
            }
        }
    }

    private class NotificationsHandler extends WebSocketListener {
        private final MultiProfile.UserProfile profile;
        private final List<EventType> enabledNotifs;

        NotificationsHandler(@NonNull MultiProfile.UserProfile profile) {
            this.profile = profile;
            this.enabledNotifs = EventType.parseFromPrefs(Prefs.getSet(PK.A2_SELECTED_NOTIFS_TYPE));
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            try {
                JSONObject json = new JSONObject(text);
                EventType type = EventType.parse(json.getString("method"));
                if (type != null && enabledNotifs.contains(type)) {
                    JSONArray events = json.getJSONArray("params");
                    for (int i = 0; i < events.length(); i++) {
                        JSONObject event = events.getJSONObject(i);
                        handleEvent(profile, event.getString("gid"), type);
                    }
                }
            } catch (JSONException ex) {
                Logging.log(ex);
            }
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable throwable, Response response) {
            notifyException(profile, throwable);
        }
    }
}
