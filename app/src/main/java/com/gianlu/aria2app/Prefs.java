package com.gianlu.aria2app;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

public class Prefs {
    private static SharedPreferences prefs;

    public static boolean getBoolean(Context context, Keys key, boolean fallback) {
        init(context);
        return prefs.getBoolean(key.key, fallback);
    }

    public static String getString(Context context, Keys key, String fallback) {
        init(context);
        return prefs.getString(key.key, fallback);
    }

    public static void putString(Context context, Keys key, String value) {
        init(context);
        prefs.edit().putString(key.key, value).apply();
    }

    private static void init(Context context) {
        if (prefs != null) return;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static int getInt(Context context, Keys key, int fallback) {
        init(context);
        return prefs.getInt(key.key, fallback);
    }

    public static int getFakeInt(Context context, Keys key, int fallback) {
        init(context);
        return Integer.parseInt(prefs.getString(key.key, String.valueOf(fallback)));
    }

    public static void removeFromSet(Context context, Keys key, String value) {
        init(context);
        Set<String> set = new HashSet<>(getSet(context, key, new HashSet<String>()));
        set.remove(value);
        prefs.edit().putStringSet(key.key, set).apply();
    }

    public static void addToSet(Context context, Keys key, String value) {
        init(context);
        Set<String> set = new HashSet<>(getSet(context, key, new HashSet<String>()));
        if (!set.contains(value)) set.add(value);
        prefs.edit().putStringSet(key.key, set).apply();
    }

    public static Set<String> getSet(Context context, Keys key, Set<String> fallback) {
        init(context);
        return new HashSet<>(prefs.getStringSet(key.key, fallback));
    }

    public static void putSet(Context context, Keys key, Set<String> set) {
        init(context);
        prefs.edit().putStringSet(key.key, set).apply();
    }

    public static void remove(Context context, Keys key) {
        init(context);
        prefs.edit().remove(key.key).apply();
    }

    public enum Keys {
        DD_DOWNLOAD_PATH("dd_downloadPath"),
        A2_PERSISTENT_NOTIFS("a2_enablePersistent"),
        A2_NOTIFS_AT_BOOT("a2_enableNotificationsAtBoot"),
        A2_FORCE_ACTION("a2_forceAction"),
        A2_MAIN_FILTERS("a2_mainFilters"),
        A2_ENABLE_NOTIFS("a2_enableNotifications"),
        A2_UPDATE_INTERVAL("a2_updateRate"),
        A2_HIDE_METADATA("a2_hideMetadata"),
        A2_GLOBAL_QUICK_OPTIONS("a2_globalQuickOptions"),
        A2_CHECK_VERSION("a2_runVersionCheckAtStartup"),
        A2_QUICK_OPTIONS("a2_quickOptions"),
        A2_MAIN_SORTING("a2_mainSorting"),
        A2_NOTIFS_SOUND("a2_enableSound"),
        A2_SELECTED_NOTIFS_TYPE("a2_selectedNotifications"),
        TRACKING_DISABLE("trackingDisable"),
        LAST_USED_PROFILE("lastUsedProfile"),
        A2_TUTORIAL_DISCOVERIES("a2_tutorialDiscoveries");
        public final String key;

        Keys(String key) {
            this.key = key;
        }
    }
}
