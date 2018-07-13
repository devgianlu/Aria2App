package com.gianlu.aria2app;

import android.os.Environment;

import com.crashlytics.android.Crashlytics;
import com.gianlu.aria2app.NetIO.ErrorHandler;
import com.gianlu.aria2app.NetIO.HttpClient;
import com.gianlu.aria2app.NetIO.Search.SearchApi;
import com.gianlu.aria2app.NetIO.WebSocketClient;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.Services.NotificationService;
import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.commonutils.Toaster;
import com.llew.huawei.verifier.LoadedApkHuaWei;

import java.util.HashSet;
import java.util.Set;

public final class ThisApplication extends AnalyticsApplication implements ErrorHandler.IErrorHandler {
    private final Set<String> checkedVersionFor = new HashSet<>();
    public static final boolean DEBUG_UPDATER = false;
    public static final boolean DEBUG_NOTIFICATION = false;

    public boolean shouldCheckVersion() {
        try {
            return !checkedVersionFor.contains(ProfilesManager.get(this).getCurrent().id);
        } catch (ProfilesManager.NoCurrentProfileException ex) {
            Logging.log(ex);
            return true;
        }
    }

    public void checkedVersion() {
        try {
            checkedVersionFor.add(ProfilesManager.get(this).getCurrent().id);
        } catch (ProfilesManager.NoCurrentProfileException ex) {
            Logging.log(ex);
        }
    }

    @Override
    protected boolean isDebug() {
        return BuildConfig.DEBUG;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LoadedApkHuaWei.hookHuaWeiVerifier(this);
        SearchApi.get().cacheSearchEngines();

        ErrorHandler.setup(Prefs.getFakeInt(this, PK.A2_UPDATE_INTERVAL, 1) * 1000, this);

        if (Prefs.getBoolean(this, PK.A2_ENABLE_NOTIFS, true))
            NotificationService.start(this);

        if (!Prefs.has(this, PK.DD_DOWNLOAD_PATH))
            Prefs.putString(this, PK.DD_DOWNLOAD_PATH,
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());

        // Backward compatibility
        if (!Prefs.has(this, PK.A2_CUSTOM_INFO)) {
            Set<String> defaultValues = new HashSet<>();
            defaultValues.add(CustomDownloadInfo.Info.DOWNLOAD_SPEED.name());
            defaultValues.add(CustomDownloadInfo.Info.REMAINING_TIME.name());
            Prefs.putSet(getApplicationContext(), PK.A2_CUSTOM_INFO, defaultValues);
        }

        deprecatedBackwardCompatibility();
    }

    @SuppressWarnings("deprecation")
    private void deprecatedBackwardCompatibility() {
        if (Prefs.has(this, PK.A2_QUICK_OPTIONS) || Prefs.has(this, PK.A2_GLOBAL_QUICK_OPTIONS)) {
            Set<String> set = new HashSet<>();
            set.addAll(Prefs.getSet(this, PK.A2_QUICK_OPTIONS, new HashSet<String>()));
            set.addAll(Prefs.getSet(this, PK.A2_GLOBAL_QUICK_OPTIONS, new HashSet<String>()));
            Prefs.putSet(this, PK.A2_QUICK_OPTIONS_MIXED, set);
            Prefs.remove(this, PK.A2_QUICK_OPTIONS);
            Prefs.remove(this, PK.A2_GLOBAL_QUICK_OPTIONS);
        }

        if (Prefs.has(this, PK.A2_TUTORIAL_DISCOVERIES)) {
            Set<String> set = Prefs.getSet(this, PK.A2_TUTORIAL_DISCOVERIES, null);
            if (set != null) Prefs.putSet(this, Prefs.Keys.TUTORIAL_DISCOVERIES, set);
            Prefs.remove(this, PK.A2_TUTORIAL_DISCOVERIES);
        }
    }

    @Override
    public void onFatal(Throwable ex) {
        WebSocketClient.clear();
        HttpClient.clear();
        Toaster.with(this).message(R.string.fatalExceptionMessage).ex(ex).show();
        LoadingActivity.startActivity(this, ex);

        Crashlytics.logException(ex);
    }

    @Override
    public void onSubsequentExceptions() {
        WebSocketClient.clear();
        HttpClient.clear();
        LoadingActivity.startActivity(this, null);
    }

    @Override
    public void onException(Throwable ex) {
        Logging.log(ex);
    }
}
