package com.gianlu.aria2app.Services;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;

import com.gianlu.aria2app.LoadingActivity;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.NetUtils;
import com.gianlu.aria2app.Prefs;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
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

    private static Intent createStartIntent(Context context) {
        ArrayList<MultiProfile> profiles = new ArrayList<>();
        for (MultiProfile profile : ProfilesManager.get(context).getProfiles())
            if (profile.notificationsEnabled) profiles.add(profile);

        return new Intent(context, NotificationService.class)
                .putExtra("foreground", Prefs.getBoolean(context, Prefs.Keys.A2_PERSISTENT_NOTIFS, true))
                .putExtra("profiles", profiles);
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, NotificationService.class).setAction("STOP"));
    }

    public static void start(Context context) {
        context.startService(NotificationService.createStartIntent(context));
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
                    .setContentText(CommonUtils.join((ArrayList<MultiProfile>) intent.getSerializableExtra("profiles"), ", "))
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setSmallIcon(R.drawable.ic_notification)
                    .addAction(new NotificationCompat.Action.Builder(
                            R.drawable.ic_clear_black_48dp,
                            getApplicationContext().getString(R.string.stopNotificationService),
                            PendingIntent.getService(getApplicationContext(), 0,
                                    new Intent(getApplicationContext(), NotificationService.class)
                                            .setAction("STOP"), 0)).build())
                    .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent))
                    .setContentIntent(PendingIntent.getActivity(this, 1, new Intent(this, LoadingActivity.class), PendingIntent.FLAG_UPDATE_CURRENT)).build());
        }

        if (intent != null) onHandleIntent(intent);
        return START_STICKY;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onHandleIntent(Intent intent) {
        List<MultiProfile> profiles = (ArrayList<MultiProfile>) intent.getSerializableExtra("profiles");
        if (Objects.equals(intent.getAction(), "STOP") || profiles == null || profiles.isEmpty()) {
            stopSelf();
            return;
        }

        for (MultiProfile multi : profiles) {
            MultiProfile.UserProfile profile = multi.getProfile(getApplicationContext());
            if (profile.connectionMethod == MultiProfile.ConnectionMethod.HTTP) continue;

            WebSocket webSocket;
            try {
                if (profile.authMethod.equals(JTA2.AuthMethod.HTTP) && profile.serverUsername != null && profile.serverPassword != null)
                    webSocket = NetUtils.readyWebSocket(profile.buildWebSocketUrl(), profile.serverUsername, profile.serverPassword, NetUtils.readyCertificate(getApplicationContext(), profile));
                else
                    webSocket = NetUtils.readyWebSocket(profile.buildWebSocketUrl(), NetUtils.readyCertificate(getApplicationContext(), profile));
            } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException | KeyManagementException ex) {
                stopSelf();
                return;
            }

            webSocket.addListener(new NotificationHandler(profile, multi.id)).connectAsynchronously();
        }
    }

    private enum Event {
        START,
        PAUSE,
        STOP,
        COMPLETE,
        ERROR,
        BTCOMPLETE,
        UNKNOWN;

        private static Event parseEvent(String event) {
            switch (event.replace("aria2.", "")) {
                case "onDownloadStart":
                    return Event.START;
                case "onDownloadPause":
                    return Event.PAUSE;
                case "onDownloadStop":
                    return Event.STOP;
                case "onDownloadComplete":
                    return Event.COMPLETE;
                case "onDownloadError":
                    return Event.ERROR;
                case "onBtDownloadComplete":
                    return Event.BTCOMPLETE;
                default:
                    return Event.UNKNOWN;
            }
        }
    }

    private class NotificationHandler extends WebSocketAdapter {
        private final MultiProfile.UserProfile profile;
        private final String profileId;
        private final boolean soundEnabled;
        private final Set<String> selectedNotifications;

        NotificationHandler(MultiProfile.UserProfile profile, String profileId) {
            this.profile = profile;
            this.profileId = profileId;

            selectedNotifications = Prefs.getSet(getApplicationContext(), Prefs.Keys.A2_SELECTED_NOTIFS_TYPE, new HashSet<String>());
            soundEnabled = Prefs.getBoolean(getApplicationContext(), Prefs.Keys.A2_NOTIFS_SOUND, true);
        }

        @Override
        public void onTextMessage(WebSocket websocket, String text) throws Exception {
            JSONObject eventBody = new JSONObject(text);
            String gid = eventBody.getJSONArray("params").getJSONObject(0).getString("gid");

            int reqCode = new Random().nextInt(10000);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(NotificationService.this)
                    .setContentIntent(
                            PendingIntent.getActivity(getApplicationContext(), reqCode, new Intent(NotificationService.this, LoadingActivity.class)
                                    .putExtra("fromNotification", true)
                                    .putExtra("profileId", profileId)
                                    .putExtra("gid", gid), PendingIntent.FLAG_UPDATE_CURRENT))
                    .setContentText(profile.getProfileName(getApplicationContext()))
                    .setContentInfo("GID#" + gid)
                    .setGroup(gid)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent))
                    .setAutoCancel(true);

            if (soundEnabled)
                builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

            switch (Event.parseEvent(eventBody.getString("method"))) {
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
