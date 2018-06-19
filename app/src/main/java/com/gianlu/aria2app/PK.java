package com.gianlu.aria2app;

import android.support.annotation.NonNull;

import com.gianlu.commonutils.Preferences.Prefs;

public enum PK implements Prefs.PrefKey {
    DD_DOWNLOAD_PATH("dd_downloadPath"),
    DD_RESUME("dd_resume"),
    DD_MAX_SIMULTANEOUS_DOWNLOADS("dd_maxSimultaneousDownloads"),
    A2_NOTIFS_AT_BOOT("a2_enableNotificationsAtBoot"),
    A2_FORCE_ACTION("a2_forceAction"),
    A2_MAIN_FILTERS("a2_mainFilters"),
    A2_ENABLE_NOTIFS("a2_enableNotifications"),
    A2_UPDATE_INTERVAL("a2_updateRate"),
    A2_HIDE_METADATA("a2_hideMetadata"),
    A2_QUICK_OPTIONS_MIXED("a2_quickOptionsMixed"),
    A2_CHECK_VERSION("a2_runVersionCheckAtStartup"),
    A2_MAIN_SORTING("a2_mainSorting"),
    A2_SELECTED_NOTIFS_TYPE("a2_selectedNotifications"),
    A2_SEARCH_ENGINES("a2_searchEngines"),
    A2_CUSTOM_INFO("a2_customInfo"),
    @Deprecated
    A2_GLOBAL_QUICK_OPTIONS("a2_globalQuickOptions"),
    @Deprecated
    A2_QUICK_OPTIONS("a2_quickOptions"),
    @Deprecated
    A2_TUTORIAL_DISCOVERIES("a2_tutorialDiscoveries");

    private final String key;

    PK(String key) {
        this.key = key;
    }

    @NonNull
    @Override
    public String getKey() {
        return key;
    }
}
