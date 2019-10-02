package com.gianlu.aria2app;

import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.aria2app.InAppAria2.Aria2ConfigProvider;
import com.gianlu.aria2app.NetIO.ConnectivityChangedReceiver;
import com.gianlu.aria2app.NetIO.ErrorHandler;
import com.gianlu.aria2app.NetIO.NetInstanceHolder;
import com.gianlu.aria2app.NetIO.Search.SearchApi;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.Services.NotificationService;
import com.gianlu.aria2lib.Aria2Ui;
import com.gianlu.aria2lib.BadEnvironmentException;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.commonutils.logging.Logging;
import com.gianlu.commonutils.preferences.CommonPK;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.preferences.PrefsStorageModule;
import com.gianlu.commonutils.ui.Toaster;
import com.yarolegovich.mp.io.MaterialPreferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ThisApplication extends AnalyticsApplication implements ErrorHandler.Listener {
    public static final boolean DEBUG_UPDATER = false;
    public static final boolean DEBUG_NOTIFICATION = false;
    private final Set<String> checkedVersionFor = new HashSet<>();
    private final SharedPreferences.OnSharedPreferenceChangeListener toggleNotificationServiceListener = (sharedPreferences, key) -> {
        if (key.equals(PK.A2_ENABLE_NOTIFS.key())) {
            if (Prefs.getBoolean(PK.A2_ENABLE_NOTIFS, true))
                NotificationService.start(ThisApplication.this);
            else
                NotificationService.stop(ThisApplication.this);
        }
    };
    private ConnectivityChangedReceiver connectivityChangedReceiver;
    private Aria2UiDispatcher aria2service;

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
        SearchApi.get().cacheSearchEngines();
        MaterialPreferences.instance().setStorageModule(new PrefsStorageModule.Factory());

        ErrorHandler.setup(Prefs.getInt(PK.A2_UPDATE_INTERVAL) * 1000, this);

        // Backward compatibility
        if (!Prefs.has(PK.A2_CUSTOM_INFO)) {
            Set<String> defaultValues = new HashSet<>();
            defaultValues.add(CustomDownloadInfo.Info.DOWNLOAD_SPEED.name());
            defaultValues.add(CustomDownloadInfo.Info.REMAINING_TIME.name());
            Prefs.putSet(PK.A2_CUSTOM_INFO, defaultValues);
        }

        deprecatedBackwardCompatibility();

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(toggleNotificationServiceListener);

        connectivityChangedReceiver = new ConnectivityChangedReceiver(this);
        getApplicationContext().registerReceiver(connectivityChangedReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        // Aria2Android integration
        Aria2Ui.provider(Aria2ConfigProvider.class);
        if (CommonUtils.isARM()) {
            try {
                aria2service = new Aria2UiDispatcher(this);
                loadAria2ServiceEnv();
            } catch (BadEnvironmentException ex) {
                Logging.log(ex);
            }
        } else {
            aria2service = null;
        }
    }

    @Override
    public void onTerminate() {
        getApplicationContext().unregisterReceiver(connectivityChangedReceiver);
        if (aria2service != null) aria2service.ui.unbind();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(toggleNotificationServiceListener);

        super.onTerminate();
    }

    @SuppressWarnings("deprecation")
    private void deprecatedBackwardCompatibility() {
        if (Prefs.has(PK.A2_QUICK_OPTIONS) || Prefs.has(PK.A2_GLOBAL_QUICK_OPTIONS)) {
            Set<String> set = new HashSet<>();
            set.addAll(Prefs.getSet(PK.A2_QUICK_OPTIONS, new HashSet<>()));
            set.addAll(Prefs.getSet(PK.A2_GLOBAL_QUICK_OPTIONS, new HashSet<>()));
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
    public void onFatal(@NonNull Throwable ex) {
        NetInstanceHolder.close();
        Toaster.with(this).message(R.string.fatalExceptionMessage).ex(ex).show();
        LoadingActivity.startActivity(this, ex);

        Logging.log(ex);
    }

    @Override
    public void onSubsequentExceptions() {
        NetInstanceHolder.close();
        LoadingActivity.startActivity(this, null);
    }

    @Override
    public void onException(@NonNull Throwable ex) {
        Logging.log(ex);
    }

    public void addAria2UiListener(@NonNull Aria2Ui.Listener listener) {
        if (aria2service == null) return;
        aria2service.listeners.add(listener);
        aria2service.ui.updateLogs(listener);
        aria2service.ui.askForStatus();
    }

    public void removeAria2UiListener(@NonNull Aria2Ui.Listener listener) {
        if (aria2service == null) return;
        aria2service.listeners.remove(listener);
    }

    public boolean getLastAria2UiState() {
        return aria2service != null && aria2service.lastUiState;
    }

    public void startAria2Service() {
        if (aria2service != null)
            aria2service.ui.startService();
    }

    public void startAria2ServiceFromReceiver() {
        if (aria2service != null)
            aria2service.ui.startServiceFromReceiver();
    }

    public void loadAria2ServiceEnv() throws BadEnvironmentException {
        if (aria2service != null && !aria2service.ui.hasEnv()) {
            aria2service.ui.loadEnv();
            aria2service.ui.bind();
            aria2service.ui.askForStatus();
        }
    }

    public boolean hasAria2ServiceEnv() {
        return aria2service != null && aria2service.ui.hasEnv();
    }

    @Nullable
    public String getInAppAria2Version() {
        if (aria2service == null) return null;

        try {
            return aria2service.ui.version();
        } catch (IOException | BadEnvironmentException ex) {
            Logging.log("Failed retrieving version!", ex);
            return null;
        }
    }

    public void stopAria2Service() {
        if (aria2service != null)
            aria2service.ui.stopService();
    }

    public boolean deleteInAppBin() {
        if (aria2service != null) return aria2service.ui.delete();
        else return false;
    }

    private class Aria2UiDispatcher implements Aria2Ui.Listener {
        private final Aria2Ui ui;
        private final Set<Aria2Ui.Listener> listeners = new HashSet<>();
        private volatile boolean lastUiState = false;

        Aria2UiDispatcher(@NonNull Context context) {
            ui = new Aria2Ui(context, this);
        }

        @Override
        public void onUpdateLogs(@NonNull List<Aria2Ui.LogMessage> msg) {
        }

        @Override
        public void onMessage(@NonNull Aria2Ui.LogMessage msg) {
            for (Aria2Ui.Listener listener : new ArrayList<>(listeners))
                listener.onMessage(msg);
        }

        @Override
        public void updateUi(boolean on) {
            if (!lastUiState && on && Prefs.getBoolean(PK.A2_ENABLE_NOTIFS))
                NotificationService.start(ThisApplication.this);

            lastUiState = on;

            for (Aria2Ui.Listener listener : new ArrayList<>(listeners))
                listener.updateUi(on);
        }
    }
}
