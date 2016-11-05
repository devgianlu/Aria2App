package com.gianlu.aria2app.Google;

import android.app.Application;
import android.content.Context;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.R;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

public class Analytics {
    public static final String CATEGORY_USER_INPUT = "User input";
    public static final String ACTION_DOWNLOAD_FILE = "Downloading file";
    public static final String ACTION_NEW_PROFILE = "New profile";
    public static final String ACTION_DELETE_PROFILE = "Profile deleted";
    public static final String ACTION_CHANGED_GLOBAL_OPTIONS = "Global options changed";
    public static final String ACTION_CHANGED_DOWNLOAD_OPTIONS = "Download options changed";
    public static final String ACTION_NEW_TORRENT = "New Torrent download";
    public static final String ACTION_NEW_METALINK = "New Metalink download";
    public static final String ACTION_NEW_URI = "New URI download";
    public static final String ACTION_DONATE_OPEN = "Opened donation dialog";
    public static final String ACTION_TERMINAL_BASIC = "Sent command in basic mode";
    public static final String ACTION_TERMINAL_ADV = "Sent command in advanced mode";
    private static Tracker tracker;

    public static Tracker getDefaultTracker(Application application) {
        if (tracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(application.getApplicationContext());
            analytics.enableAutoActivityReports(application);
            tracker = analytics.newTracker(R.xml.tracking);
            tracker.enableAdvertisingIdCollection(true);
            tracker.enableExceptionReporting(true);
        }

        return tracker;
    }

    @Nullable
    static Tracker getTracker() {
        return tracker;
    }

    public static boolean isTrackingAllowed(Context context) {
        return !PreferenceManager.getDefaultSharedPreferences(context).getBoolean("a2_trackingDisable", false) && !com.gianlu.commonutils.BuildConfig.DEBUG;
    }
}
