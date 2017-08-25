package com.gianlu.aria2app;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.gianlu.aria2app.NetIO.DownloadsManager.DownloadsManager;
import com.gianlu.aria2app.NetIO.ErrorHandler;
import com.gianlu.aria2app.NetIO.WebSocketing;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Prefs;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.liulishuo.filedownloader.FileDownloader;

import java.util.Map;

public class ThisApplication extends Application implements ErrorHandler.IErrorHandler, DownloadsManager.IGlobalMonitor {
    public static final String CATEGORY_USER_INPUT = "User input";
    public static final String ACTION_DOWNLOAD_FILE = "Download file";
    public static final String ACTION_NEW_PROFILE = "New profile";
    public static final String ACTION_DELETE_PROFILE = "Profile deleted";
    public static final String ACTION_CHANGED_GLOBAL_OPTIONS = "Global options changed";
    public static final String ACTION_CHANGED_DOWNLOAD_OPTIONS = "Download options changed";
    public static final String ACTION_NEW_TORRENT = "New Torrent download";
    public static final String ACTION_NEW_METALINK = "New Metalink download";
    public static final String ACTION_NEW_URI = "New URI download";
    public static final String ACTION_NEW_URI_SEARCH = "New URI download with torrent search";
    public static final String ACTION_DONATE_OPEN = "Opened donation dialog";
    public static final String ACTION_SHARE = "Shared something with the app";
    public static final String ACTION_DOWNLOAD_DIRECTORY = "Download directory";
    public static final String ACTION_SEARCH = "Search torrent";
    public static final String ACTION_STARTED_TEST = "Started profile test";
    private static Tracker tracker;
    private boolean checkedVersion = false;

    @NonNull
    private static Tracker getTracker(Application application) {
        if (tracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(application.getApplicationContext());
            analytics.enableAutoActivityReports(application);
            tracker = analytics.newTracker(R.xml.tracking);
            tracker.enableAdvertisingIdCollection(true);
            tracker.enableExceptionReporting(true);
        }

        return tracker;
    }

    public static void sendAnalytics(Context context, @Nullable Map<String, String> map) {
        if (!Prefs.getBoolean(context, Prefs.Keys.TRACKING_DISABLE, false) && !BuildConfig.DEBUG)
            if (tracker != null)
                tracker.send(map);
    }

    public boolean hasCheckedVersion() {
        return checkedVersion;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        CommonUtils.setDebug(BuildConfig.DEBUG);
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(this));
        FileDownloader.setup(getApplicationContext());
        ErrorHandler.setup(Prefs.getFakeInt(this, PKeys.A2_UPDATE_INTERVAL, 1000), this);

        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(!BuildConfig.DEBUG);
        tracker = getTracker(this);

        DownloadsManager.setGlobalMonitor(this);
    }

    @Override
    public void onFatal(Throwable ex) {
        WebSocketing.clear();
        Toast.makeText(this, R.string.fatalExceptionMessage, Toast.LENGTH_LONG).show();
        LoadingActivity.startActivity(this, ex);

        sendAnalytics(getApplicationContext(), new HitBuilders.ExceptionBuilder()
                .setDescription(Logging.getStackTrace(ex))
                .setFatal(false)
                .build());
    }

    @Override
    public void onSubsequentExceptions() {
        WebSocketing.clear();
        LoadingActivity.startActivity(this, null);
    }

    public void setHasCheckedVersion(boolean checkedVersion) {
        this.checkedVersion = checkedVersion;
    }

    @Override
    public void onException(Throwable ex) {
        Logging.logMe(this, ex);

        /*
        sendAnalytics(getApplicationContext(), new HitBuilders.ExceptionBuilder()
                .setDescription(Logging.getStackTrace(ex))
                .setFatal(false)
                .build());
                */
    }
}
