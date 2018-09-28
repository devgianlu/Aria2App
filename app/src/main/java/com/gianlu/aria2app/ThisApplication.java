package com.gianlu.aria2app;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.crashlytics.android.Crashlytics;
import com.gianlu.aria2app.NetIO.ErrorHandler;
import com.gianlu.aria2app.NetIO.HttpClient;
import com.gianlu.aria2app.NetIO.Search.SearchApi;
import com.gianlu.aria2app.NetIO.WebSocketClient;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.Services.NotificationService;
import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.CommonPK;
import com.gianlu.commonutils.FossUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.commonutils.Preferences.PrefsStorageModule;
import com.gianlu.commonutils.Toaster;
import com.llew.huawei.verifier.LoadedApkHuaWei;
import com.yarolegovich.mp.io.MaterialPreferences;

import java.util.HashSet;
import java.util.Set;

public final class ThisApplication extends AnalyticsApplication implements ErrorHandler.IErrorHandler {
    public static final boolean DEBUG_UPDATER = false;
    public static final boolean DEBUG_NOTIFICATION = false;
    private final Set<String> checkedVersionFor = new HashSet<>();

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
        MaterialPreferences.instance().setStorageModule(new PrefsStorageModule.Factory());

        ErrorHandler.setup(Prefs.getInt(PK.A2_UPDATE_INTERVAL) * 1000, this);

        if (Prefs.getBoolean(PK.A2_ENABLE_NOTIFS, true))
            NotificationService.start(this);

        // Backward compatibility
        if (!Prefs.has(PK.A2_CUSTOM_INFO)) {
            Set<String> defaultValues = new HashSet<>();
            defaultValues.add(CustomDownloadInfo.Info.DOWNLOAD_SPEED.name());
            defaultValues.add(CustomDownloadInfo.Info.REMAINING_TIME.name());
            Prefs.putSet(PK.A2_CUSTOM_INFO, defaultValues);
        }

        deprecatedBackwardCompatibility();

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
                    @Override
                    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                        if (key.equals(PK.A2_ENABLE_NOTIFS.key())) {
                            if (Prefs.getBoolean(PK.A2_ENABLE_NOTIFS, true))
                                NotificationService.start(ThisApplication.this);
                            else
                                NotificationService.stop(ThisApplication.this);
                        }
                    }
                });
    }

    @SuppressWarnings("deprecation")
    private void deprecatedBackwardCompatibility() {
        if (Prefs.has(PK.A2_QUICK_OPTIONS) || Prefs.has(PK.A2_GLOBAL_QUICK_OPTIONS)) {
            Set<String> set = new HashSet<>();
            set.addAll(Prefs.getSet(PK.A2_QUICK_OPTIONS, new HashSet<String>()));
            set.addAll(Prefs.getSet(PK.A2_GLOBAL_QUICK_OPTIONS, new HashSet<String>()));
            Prefs.putSet(PK.A2_QUICK_OPTIONS_MIXED, set);
            Prefs.remove(PK.A2_QUICK_OPTIONS);
            Prefs.remove(PK.A2_GLOBAL_QUICK_OPTIONS);
        }

        if (Prefs.has(PK.A2_TUTORIAL_DISCOVERIES)) {
            Set<String> set = Prefs.getSet(PK.A2_TUTORIAL_DISCOVERIES, null);
            if (set != null) Prefs.putSet(CommonPK.TUTORIAL_DISCOVERIES, set);
            Prefs.remove(PK.A2_TUTORIAL_DISCOVERIES);
        }
    }

    @Override
    public void onFatal(Throwable ex) {
        WebSocketClient.clear();
        HttpClient.clear();
        Toaster.with(this).message(R.string.fatalExceptionMessage).ex(ex).show();
        LoadingActivity.startActivity(this, ex);

        if (FossUtils.hasCrashlytics())
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
