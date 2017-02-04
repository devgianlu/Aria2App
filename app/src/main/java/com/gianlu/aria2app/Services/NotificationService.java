package com.gianlu.aria2app.Services;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;

import com.gianlu.aria2app.MainActivity;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.WebSocketing;
import com.gianlu.aria2app.Profile.MultiModeProfileItem;
import com.gianlu.aria2app.Profile.ProfileItem;
import com.gianlu.aria2app.Profile.SingleModeProfileItem;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;

import org.json.JSONObject;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public class NotificationService extends IntentService {
    public NotificationService() {
        super("Aria2App notification service");
    }

    public static Intent createStartIntent(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        ArrayList<SingleModeProfileItem> profiles = new ArrayList<>();
        for (ProfileItem profile : ProfileItem.getProfiles(context)) {
            if (!profile.notificationsEnabled) continue;

            if (profile.singleMode)
                profiles.add((SingleModeProfileItem) profile);
            else
                profiles.add(((MultiModeProfileItem) profile).getCurrentProfile(context));
        }

        return new Intent(context, NotificationService.class)
                .putExtra("foreground", sharedPreferences.getBoolean("a2_enablePersistent", true))
                .putExtra("profiles", profiles);
    }

    private static String expandProfileList(List<SingleModeProfileItem> profiles) {
        String expanded = "";

        boolean first = true;
        for (SingleModeProfileItem profile : profiles) {
            if (!first) {
                expanded += ", ";
            }

            expanded += profile.globalProfileName;
            first = false;
        }

        return expanded;
    }

    @SuppressWarnings("unchecked")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && !Objects.equals(intent.getAction(), "STOP") && intent.getBooleanExtra("foreground", true)) {
            startForeground(new Random().nextInt(10000), new NotificationCompat.Builder(this)
                    .setShowWhen(false)
                    .setPriority(Notification.PRIORITY_MIN)
                    .setContentTitle(getString(R.string.notificationService))
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setContentText(expandProfileList((List<SingleModeProfileItem>) intent.getSerializableExtra("profiles")))
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setSmallIcon(R.drawable.ic_notification)
                    .addAction(new NotificationCompat.Action.Builder(
                            R.drawable.ic_clear_black_48dp,
                            getApplicationContext().getString(R.string.stopNotificationService),
                            PendingIntent.getService(getApplicationContext(), 0,
                                    new Intent(getApplicationContext(), NotificationService.class)
                                            .setAction("STOP"), 0)).build())
                    .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent))
                    .setContentIntent(PendingIntent.getActivity(this, 1, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT)).build());
        }

        if (intent != null)
            onHandleIntent(intent);
        return START_STICKY;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onHandleIntent(Intent intent) {
        if (Objects.equals(intent.getAction(), "STOP") || ((List<SingleModeProfileItem>) intent.getSerializableExtra("profiles")).size() == 0) {
            stopSelf();
            return;
        }

        for (SingleModeProfileItem profile : (List<SingleModeProfileItem>) intent.getSerializableExtra("profiles")) {
            if (profile.connectionMethod == SingleModeProfileItem.ConnectionMethod.HTTP)
                continue;

            String scheme;
            if (profile.serverSSL)
                scheme = "wss://";
            else
                scheme = "ws://";

            WebSocket webSocket;
            try {
                if (profile.authMethod.equals(JTA2.AuthMethod.HTTP))
                    webSocket = WebSocketing.readyWebSocket(scheme + profile.serverAddr + ":" + profile.serverPort + profile.serverEndpoint, profile.serverUsername, profile.serverPassword, Utils.readyCertificate(getApplicationContext(), profile));
                else
                    webSocket = WebSocketing.readyWebSocket(scheme + profile.serverAddr + ":" + profile.serverPort + profile.serverEndpoint, Utils.readyCertificate(getApplicationContext(), profile));
            } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException | KeyManagementException ex) {
                stopSelf();
                return;
            }

            webSocket.addListener(new NotificationHandler(profile))
                    .connectAsynchronously();
        }
    }

    private enum EVENT {
        START,
        PAUSE,
        STOP,
        COMPLETE,
        ERROR,
        BTCOMPLETE,
        UNKNOWN;

        private static EVENT parseEvent(String event) {
            switch (event.replace("aria2.", "")) {
                case "onDownloadStart":
                    return EVENT.START;
                case "onDownloadPause":
                    return EVENT.PAUSE;
                case "onDownloadStop":
                    return EVENT.STOP;
                case "onDownloadComplete":
                    return EVENT.COMPLETE;
                case "onDownloadError":
                    return EVENT.ERROR;
                case "onBtDownloadComplete":
                    return EVENT.BTCOMPLETE;
                default:
                    return EVENT.UNKNOWN;
            }
        }
    }

    private class NotificationHandler extends WebSocketAdapter {
        private final SingleModeProfileItem profile;
        private final boolean soundEnabled;
        private final Set<String> selectedNotifications;

        NotificationHandler(SingleModeProfileItem profile) {
            this.profile = profile;
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(NotificationService.this);

            selectedNotifications = sharedPreferences.getStringSet("a2_selectedNotifications", new HashSet<String>());
            soundEnabled = sharedPreferences.getBoolean("a2_enableSound", true);
        }

        @Override
        public void onTextMessage(WebSocket websocket, String text) throws Exception {
            JSONObject jResponse = new JSONObject(text);

            String gid = jResponse.getJSONArray("params").getJSONObject(0).getString("gid");

            int reqCode = new Random().nextInt(10000);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(NotificationService.this)
                    .setContentIntent(
                            PendingIntent.getActivity(getApplicationContext(), reqCode, new Intent(NotificationService.this, MainActivity.class)
                                    .putExtra("fromNotification", true)
                                    .putExtra("fileName", profile.fileName)
                                    .putExtra("gid", gid), PendingIntent.FLAG_UPDATE_CURRENT))
                    .setContentText(profile.globalProfileName)
                    .setContentInfo("GID#" + gid)
                    .setGroup(gid)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent))
                    .setAutoCancel(true);

            if (soundEnabled)
                builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

            switch (EVENT.parseEvent(jResponse.getString("method"))) {
                case START:
                    if (!selectedNotifications.contains("START")) return;
                    builder.setContentTitle(getString(R.string.notificationStarted));
                    break;
                case STOP:
                    if (!selectedNotifications.contains("STOP")) return;
                    builder.setContentTitle(getString(R.string.notificationStopped));
                    break;
                case PAUSE:
                    if (!selectedNotifications.contains("PAUSE")) return;
                    builder.setContentTitle(getString(R.string.notificationPaused));
                    break;
                case COMPLETE:
                    if (!selectedNotifications.contains("COMPLETE")) return;
                    builder.setContentTitle(getString(R.string.notificationComplete));
                    break;
                case BTCOMPLETE:
                    if (!selectedNotifications.contains("BTCOMPLETE")) return;
                    builder.setContentTitle(getString(R.string.notificationBTComplete));
                    break;
                case ERROR:
                    if (!selectedNotifications.contains("ERROR")) return;
                    builder.setContentTitle(getString(R.string.notificationError));
                    break;
            }

            NotificationManagerCompat.from(NotificationService.this).notify(reqCode, builder.build());
        }
    }
}
