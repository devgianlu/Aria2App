package com.gianlu.aria2app;

import android.os.Environment;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.gianlu.aria2app.NetIO.ErrorHandler;
import com.gianlu.aria2app.NetIO.HttpClient;
import com.gianlu.aria2app.NetIO.Search.SearchApi;
import com.gianlu.aria2app.NetIO.WebSocketClient;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.commonutils.Toaster;
import com.llew.huawei.verifier.LoadedApkHuaWei;

import java.util.HashSet;
import java.util.Set;

public final class ThisApplication extends AnalyticsApplication implements ErrorHandler.IErrorHandler {
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

        ErrorHandler.setup(Prefs.getFakeInt(this, PKeys.A2_UPDATE_INTERVAL, 1) * 1000, this);

        // Backward compatibility
        if (!Prefs.has(getApplicationContext(), PKeys.A2_CUSTOM_INFO)) {
            Set<String> defaultValues = new HashSet<>();
            defaultValues.add(CustomDownloadInfo.Info.DOWNLOAD_SPEED.name());
            defaultValues.add(CustomDownloadInfo.Info.REMAINING_TIME.name());
            Prefs.putSet(getApplicationContext(), PKeys.A2_CUSTOM_INFO, defaultValues);
        }

        Logging.clearLogs(this);

        if (Prefs.getString(this, PKeys.DD_DOWNLOAD_PATH, null) == null)
            Prefs.putString(this, PKeys.DD_DOWNLOAD_PATH, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
    }

    @Override
    public void onFatal(Throwable ex) {
        WebSocketClient.clear();
        HttpClient.clear();
        Toaster.show(this, getString(R.string.fatalExceptionMessage), Toast.LENGTH_LONG, null, ex, null);
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
