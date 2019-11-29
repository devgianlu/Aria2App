package com.gianlu.aria2app;

import android.os.Environment;

import com.gianlu.aria2app.adapters.DownloadCardsAdapter;
import com.gianlu.aria2app.api.aria2.Download;
import com.gianlu.aria2app.services.NotificationService;
import com.gianlu.commonutils.preferences.CommonPK;
import com.gianlu.commonutils.preferences.Prefs;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class PK extends CommonPK {
    public static final Prefs.Key SEARCH_ENGINES_CACHE = new Prefs.Key("searchEngineCache");
    public static final Prefs.Key SEARCH_ENGINES_CACHE_AGE = new Prefs.Key("searchEngineCacheAge");
    public static final Prefs.KeyWithDefault<String> DD_DOWNLOAD_PATH = new Prefs.KeyWithDefault<>("dd_downloadPath", () -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
    public static final Prefs.KeyWithDefault<Boolean> DD_RESUME = new Prefs.KeyWithDefault<>("dd_resume", true);
    public static final Prefs.KeyWithDefault<Boolean> DD_USE_EXTERNAL = new Prefs.KeyWithDefault<>("dd_useExternal", false);
    public static final Prefs.Key LAST_USED_PROFILE = new Prefs.Key("lastUsedProfile");
    public static final Prefs.KeyWithDefault<Integer> DD_MAX_SIMULTANEOUS_DOWNLOADS = new Prefs.KeyWithDefault<>("dd_maxSimultaneousDownloads", 3);
    public static final Prefs.KeyWithDefault<Boolean> A2_NOTIFS_AT_BOOT = new Prefs.KeyWithDefault<>("a2_enableNotificationsAtBoot", true);
    public static final Prefs.KeyWithDefault<Boolean> IN_APP_DOWNLOADER_AT_BOOT = new Prefs.KeyWithDefault<>("startInAppDownloaderAtBoot", false);
    public static final Prefs.KeyWithDefault<Set<String>> A2_MAIN_FILTERS = new Prefs.KeyWithDefault<>("a2_mainFilters", new HashSet<>(Download.Status.stringValues()));
    public static final Prefs.KeyWithDefault<Boolean> A2_ENABLE_NOTIFS = new Prefs.KeyWithDefault<>("a2_enableNotifications", true);
    public static final Prefs.KeyWithDefault<Integer> A2_UPDATE_INTERVAL = new Prefs.KeyWithDefault<>("a2_updateRate", 1);
    public static final Prefs.KeyWithDefault<Boolean> A2_HIDE_METADATA = new Prefs.KeyWithDefault<>("a2_hideMetadata", false);
    public static final Prefs.Key A2_QUICK_OPTIONS_MIXED = new Prefs.Key("a2_quickOptionsMixed");
    public static final Prefs.KeyWithDefault<Boolean> A2_CHECK_VERSION = new Prefs.KeyWithDefault<>("a2_runVersionCheckAtStartup", true);
    public static final Prefs.KeyWithDefault<String> A2_MAIN_SORTING = new Prefs.KeyWithDefault<>("a2_mainSorting", DownloadCardsAdapter.SortBy.STATUS.name());
    public static final Prefs.KeyWithDefault<Set<String>> A2_SELECTED_NOTIFS_TYPE = new Prefs.KeyWithDefault<>("a2_selectedNotifications", new HashSet<>(Arrays.asList(NotificationService.EventType.prefsValues())));
    public static final Prefs.Key A2_SEARCH_ENGINES = new Prefs.Key("a2_searchEngines");
    public static final Prefs.Key A2_CUSTOM_INFO = new Prefs.Key("a2_customInfo");
    public static final Prefs.KeyWithDefault<Boolean> A2_ADD_BEST_TRACKERS = new Prefs.KeyWithDefault<>("a2_addBestTrackers", false);
    public static final Prefs.Key WEBVIEW_HOMEPAGE = new Prefs.Key("webView_homepage");
    public static final Prefs.Key TRACKERS_LIST_CACHE = new Prefs.Key("trackersListCache");
    public static final Prefs.Key TRACKERS_LIST_CACHE_AGE = new Prefs.Key("trackersListCacheAge");
    @Deprecated
    public static final Prefs.Key A2_GLOBAL_QUICK_OPTIONS = new Prefs.Key("a2_globalQuickOptions");
    @Deprecated
    public static final Prefs.Key A2_QUICK_OPTIONS = new Prefs.Key("a2_quickOptions");
    @Deprecated
    public static final Prefs.Key A2_TUTORIAL_DISCOVERIES = new Prefs.Key("a2_tutorialDiscoveries");
}
