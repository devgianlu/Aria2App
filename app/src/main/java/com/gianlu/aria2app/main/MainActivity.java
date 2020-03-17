package com.gianlu.aria2app.main;

import android.Manifest;
import android.app.SearchManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.gianlu.aria2app.LoadingActivity;
import com.gianlu.aria2app.PK;
import com.gianlu.aria2app.PreferenceActivity;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.ThisApplication;
import com.gianlu.aria2app.Utils;
import com.gianlu.aria2app.activities.AddMetalinkActivity;
import com.gianlu.aria2app.activities.AddTorrentActivity;
import com.gianlu.aria2app.activities.AddUriActivity;
import com.gianlu.aria2app.activities.BatchAddActivity;
import com.gianlu.aria2app.activities.DirectDownloadActivity;
import com.gianlu.aria2app.activities.EditProfileActivity;
import com.gianlu.aria2app.activities.MoreAboutDownloadActivity;
import com.gianlu.aria2app.activities.SearchActivity;
import com.gianlu.aria2app.activities.adddownload.AddBase64Bundle;
import com.gianlu.aria2app.activities.adddownload.AddDownloadBundle;
import com.gianlu.aria2app.activities.moreabout.OnBackPressed;
import com.gianlu.aria2app.activities.moreabout.files.FilesFragment;
import com.gianlu.aria2app.activities.moreabout.info.InfoFragment;
import com.gianlu.aria2app.activities.moreabout.peers.PeersFragment;
import com.gianlu.aria2app.activities.moreabout.servers.ServersFragment;
import com.gianlu.aria2app.adapters.DownloadCardsAdapter;
import com.gianlu.aria2app.adapters.PagerAdapter;
import com.gianlu.aria2app.api.AbstractClient;
import com.gianlu.aria2app.api.AriaRequests;
import com.gianlu.aria2app.api.GitHubApi;
import com.gianlu.aria2app.api.NetInstanceHolder;
import com.gianlu.aria2app.api.OnRefresh;
import com.gianlu.aria2app.api.aria2.Aria2Helper;
import com.gianlu.aria2app.api.aria2.Download;
import com.gianlu.aria2app.api.aria2.DownloadWithUpdate;
import com.gianlu.aria2app.api.aria2.DownloadsAndGlobalStats;
import com.gianlu.aria2app.api.aria2.VersionInfo;
import com.gianlu.aria2app.api.updater.PayloadProvider;
import com.gianlu.aria2app.api.updater.Receiver;
import com.gianlu.aria2app.api.updater.UpdaterActivity;
import com.gianlu.aria2app.api.updater.Wants;
import com.gianlu.aria2app.downloader.FetchHelper;
import com.gianlu.aria2app.inappdownloader.InAppAria2ConfActivity;
import com.gianlu.aria2app.options.OptionsDialog;
import com.gianlu.aria2app.profiles.CustomProfilesAdapter;
import com.gianlu.aria2app.profiles.MultiProfile;
import com.gianlu.aria2app.profiles.ProfilesManager;
import com.gianlu.aria2app.tutorial.Discovery;
import com.gianlu.aria2app.tutorial.DownloadCardsTutorial;
import com.gianlu.aria2app.tutorial.DownloadsToolbarTutorial;
import com.gianlu.aria2app.webview.WebViewActivity;
import com.gianlu.aria2lib.Aria2Ui;
import com.gianlu.aria2lib.BadEnvironmentException;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.drawer.BaseDrawerItem;
import com.gianlu.commonutils.drawer.DrawerManager;
import com.gianlu.commonutils.logs.LogsHelper;
import com.gianlu.commonutils.misc.MessageView;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.commonutils.permissions.AskPermission;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.tutorial.BaseTutorial;
import com.gianlu.commonutils.tutorial.TutorialManager;
import com.gianlu.commonutils.ui.Toaster;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class MainActivity extends UpdaterActivity implements FloatingActionsMenu.OnFloatingActionsMenuUpdateListener, TutorialManager.Listener, HideSecondSpace, DrawerManager.ProfilesDrawerListener<MultiProfile>, DownloadCardsAdapter.Listener, SearchView.OnQueryTextListener, SearchView.OnCloseListener, MenuItem.OnActionExpandListener, OnRefresh, DrawerManager.MenuDrawerListener<DrawerItem>, FetchHelper.FetchDownloadCountListener, Aria2Ui.Listener {
    private static final int REQUEST_READ_CODE = 12;
    private final static Wants<DownloadsAndGlobalStats> MAIN_WANTS = Wants.downloadsAndStats();
    private static final String TAG = MainActivity.class.getSimpleName();
    private DrawerManager<MultiProfile, DrawerItem> drawerManager;
    private FloatingActionsMenu fabMenu;
    private DownloadCardsAdapter adapter;
    private SearchView searchView;
    private Uri _sharedUri;
    private Toolbar toolbar;
    private TextView active;
    private TextView paused;
    private ImageButton toggleChart;
    private LineChart overallChart;
    private TextView stopped;
    private RecyclerMessageView recyclerViewLayout;
    private Aria2Helper helper;
    private FrameLayout secondSpace = null;
    private ViewPager secondSpacePager = null;
    private PagerAdapter<? extends OnBackPressed> secondSpaceAdapter = null;
    private TabLayout secondSpaceTabs = null;
    private LinearLayout secondSpaceContainer = null;
    private MessageView secondSpaceMessage;
    private ProfilesManager profilesManager;
    private TutorialManager tutorialManager;
    private TextView filtersVerbose;

    @Override
    protected void onRestart() {
        super.onRestart();

        if (drawerManager != null && drawerManager.isOpen())
            drawerManager.refreshProfiles(profilesManager.getProfiles());
    }

    @Override
    public void onDrawerProfileSelected(@NonNull MultiProfile profile) {
        profilesManager.setLastProfile(profile);
        LoadingActivity.startActivity(this);
    }

    @Override
    public boolean onDrawerProfileLongClick(@NonNull MultiProfile profile) {
        if (profile.isInAppDownloader()) {
            try {
                ((ThisApplication) getApplicationContext()).loadAria2ServiceEnv();
            } catch (BadEnvironmentException ex) {
                return false;
            }

            startActivity(new Intent(this, InAppAria2ConfActivity.class));
        } else {
            EditProfileActivity.start(this, profile.id);
        }

        return true;
    }

    private void setupAdapterFiltersAndSorting() {
        if (adapter == null) return;

        List<Download.Status> filters = new ArrayList<>(Arrays.asList(Download.Status.values()));
        Set<String> checkedFiltersSet = Prefs.getSet(PK.A2_MAIN_FILTERS);
        for (String filter : checkedFiltersSet) filters.remove(Download.Status.valueOf(filter));
        adapter.setFilters(filters);
        adapter.sort(DownloadCardsAdapter.SortBy.valueOf(Prefs.getString(PK.A2_MAIN_SORTING)));

        updateFiltersVerbose();
    }

    private void processFileUri(@NonNull Uri uri) {
        String mimeType;
        try {
            mimeType = AddBase64Bundle.extractMimeType(this, uri);
        } catch (AddDownloadBundle.CannotReadException ex) {
            Log.e(TAG, "Cannot read file.", ex);
            Toaster.with(this).message(R.string.invalidFile).show();
            return;
        }

        if (Objects.equals(mimeType, "application/x-bittorrent")) {
            AddTorrentActivity.startAndAdd(this, uri);
        } else if (Objects.equals(mimeType, "application/metalink4+xml") || Objects.equals(mimeType, "application/metalink+xml")) {
            AddMetalinkActivity.startAndAdd(this, uri);
        } else {
            Log.w(TAG, "File type not supported: " + mimeType);
            Toaster.with(this).message(R.string.invalidFile).show();
        }
    }

    @Override
    public boolean onDrawerMenuItemSelected(@NonNull BaseDrawerItem<DrawerItem> which) {
        switch (which.id) {
            case HOME:
                refresh(MAIN_WANTS, this);
                return true;
            case DIRECT_DOWNLOAD:
                startActivity(new Intent(this, DirectDownloadActivity.class));
                return false;
            case QUICK_OPTIONS:
                showDialog(OptionsDialog.getGlobal(true));
                return true;
            case GLOBAL_OPTIONS:
                showDialog(OptionsDialog.getGlobal(false));
                return true;
            case PREFERENCES:
                startActivity(new Intent(this, PreferenceActivity.class));
                return false;
            case SUPPORT:
                LogsHelper.sendEmail(this, null);
                return true;
            case ABOUT_ARIA2:
                AboutAria2Dialog.get().show(getSupportFragmentManager(), null);
                return true;
            case ADD_PROFILE:
                EditProfileActivity.start(this, false);
                return true;
            default:
                return true;
        }
    }

    @Override
    protected void onPreCreate(@Nullable Bundle savedInstanceState) {
        attachReceiver(this, new Receiver<DownloadsAndGlobalStats>() {
            @Override
            public void onUpdateUi(@NonNull DownloadsAndGlobalStats payload) {
                if (adapter != null) {
                    adapter.itemsChanged(payload.downloads);
                    recyclerViewLayout.stopLoading();

                    if (adapter.getItemCount() == 0)
                        FloatingActionsMenuBehavior.scaleTo(fabMenu, true);
                }

                String gid = getIntent().getStringExtra("gid");
                if (gid != null && !payload.downloads.isEmpty()) {
                    for (DownloadWithUpdate download : payload.downloads) {
                        if (Objects.equals(download.gid, gid)) {
                            onMoreClick(download);
                            getIntent().removeExtra("gid");
                        }
                    }
                }

                active.setText(String.valueOf(payload.stats.numActive));
                paused.setText(String.valueOf(payload.stats.numWaiting));
                stopped.setText(String.format(Locale.getDefault(), "%d (%d)", payload.stats.numStopped, payload.stats.numStoppedTotal));

                LineData data = overallChart.getData();
                if (data == null) {
                    Utils.setupChart(overallChart, true, R.color.white);
                    data = overallChart.getData();
                }

                if (data != null) {
                    int pos = data.getEntryCount() / 2 + 1;
                    data.addEntry(new Entry(pos, payload.stats.downloadSpeed), Utils.CHART_DOWNLOAD_SET);
                    data.addEntry(new Entry(pos, payload.stats.uploadSpeed), Utils.CHART_UPLOAD_SET);
                    data.notifyDataChanged();
                    overallChart.notifyDataSetChanged();

                    overallChart.setVisibleXRangeMaximum(90);
                    overallChart.moveViewToX(pos - 91);
                }
            }

            @Override
            public void onLoad(@NonNull DownloadsAndGlobalStats payload) {
                adapter = new DownloadCardsAdapter(MainActivity.this, payload.downloads, MainActivity.this);
                recyclerViewLayout.loadListData(adapter);
                setupAdapterFiltersAndSorting();
            }

            @Override
            public boolean onCouldntLoad(@NonNull Exception ex) {
                Log.e(TAG, "Failed loading info.", ex);

                if (recyclerViewLayout != null)
                    recyclerViewLayout.showError(R.string.failedLoadingDownloads);

                return false;
            }

            @Override
            public boolean onUpdateException(@NonNull Exception ex) {
                return false;
            }

            @NonNull
            @Override
            public Wants<DownloadsAndGlobalStats> wants() {
                return MAIN_WANTS;
            }

            @NonNull
            @Override
            public PayloadProvider<DownloadsAndGlobalStats> requireProvider() throws Aria2Helper.InitializingException {
                return new MainProvider(MainActivity.this);
            }
        });
    }

    @Override
    protected void onPostCreate() {
        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name);

        toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        setRequestedOrientation(getResources().getBoolean(R.bool.isTablet) ? ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        profilesManager = ProfilesManager.get(this);
        drawerManager = new DrawerManager.Config<MultiProfile, DrawerItem>(this)
                .addMenuItem(new BaseDrawerItem<>(DrawerItem.HOME, R.drawable.baseline_home_24, getString(R.string.home)))
                .addMenuItem(new BaseDrawerItem<>(DrawerItem.DIRECT_DOWNLOAD, R.drawable.baseline_cloud_download_24, getString(R.string.directDownload)))
                .addMenuItem(new BaseDrawerItem<>(DrawerItem.QUICK_OPTIONS, R.drawable.baseline_favorite_24, getString(R.string.quickGlobalOptions)))
                .addMenuItem(new BaseDrawerItem<>(DrawerItem.GLOBAL_OPTIONS, R.drawable.baseline_list_24, getString(R.string.globalOptions)))
                .addMenuItem(new BaseDrawerItem<>(DrawerItem.ABOUT_ARIA2, R.drawable.baseline_cloud_24, getString(R.string.about_aria2)))
                .addMenuItemSeparator()
                .addMenuItem(new BaseDrawerItem<>(DrawerItem.PREFERENCES, R.drawable.baseline_settings_24, getString(R.string.preferences)))
                .addMenuItem(new BaseDrawerItem<>(DrawerItem.SUPPORT, R.drawable.baseline_report_problem_24, getString(R.string.support)))
                .addProfiles(profilesManager.getProfiles(), this, (context, profiles, listener) -> new CustomProfilesAdapter(context, profiles, 0, listener))
                .addProfilesMenuItem(new BaseDrawerItem<>(DrawerItem.ADD_PROFILE, R.drawable.baseline_add_24, getString(R.string.addProfile)))
                .build(this, findViewById(R.id.main_drawer), toolbar);

        MultiProfile currentProfile;
        try {
            currentProfile = profilesManager.getCurrent();
            helper = Aria2Helper.instantiate(this);
        } catch (ProfilesManager.NoCurrentProfileException | Aria2Helper.InitializingException ex) {
            Log.e(TAG, "Failed initialising.", ex);
            NetInstanceHolder.close();
            profilesManager.unsetLastProfile();
            LoadingActivity.startActivity(this, ex);
            return;
        }

        drawerManager.setCurrentProfile(currentProfile);
        setTitle(currentProfile.getPrimaryText(this) + " - " + getString(R.string.app_name));
        drawerManager.setActiveItem(DrawerItem.HOME);

        filtersVerbose = findViewById(R.id.main_filtersVerbose);
        active = findViewById(R.id.main_active);
        paused = findViewById(R.id.main_paused);
        stopped = findViewById(R.id.main_stopped);
        overallChart = findViewById(R.id.main_overallChart);
        FrameLayout overallChartContainer = findViewById(R.id.main_overallChartContainer);
        toggleChart = findViewById(R.id.main_toggleChart);
        toggleChart.setOnClickListener(view -> CommonUtils.handleCollapseClick(toggleChart, overallChartContainer));

        recyclerViewLayout = findViewById(R.id.main_rmv);
        recyclerViewLayout.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));

        recyclerViewLayout.enableSwipeRefresh(() -> refresh(MAIN_WANTS, MainActivity.this), R.color.colorAccent, R.color.colorMetalink, R.color.colorTorrent);

        fabMenu = findViewById(R.id.main_fab);
        fabMenu.setOnFloatingActionsMenuUpdateListener(this);

        FloatingActionButton fabSearch = findViewById(R.id.mainFab_search);
        fabSearch.setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
        FloatingActionButton fabAddURI = findViewById(R.id.mainFab_addURI);
        fabAddURI.setOnClickListener(view -> startActivity(new Intent(this, AddUriActivity.class)));
        FloatingActionButton fabAddTorrent = findViewById(R.id.mainFab_addTorrent);
        fabAddTorrent.setOnClickListener(view -> startActivity(new Intent(this, AddTorrentActivity.class)));
        FloatingActionButton fabAddMetalink = findViewById(R.id.mainFab_addMetalink);
        fabAddMetalink.setOnClickListener(view -> startActivity(new Intent(this, AddMetalinkActivity.class)));
        FloatingActionButton fabBatchAdd = findViewById(R.id.mainFab_batchAdd);
        fabBatchAdd.setOnClickListener(v -> startActivity(new Intent(this, BatchAddActivity.class)));
        FloatingActionButton fabWebView = findViewById(R.id.mainFab_webView);
        fabWebView.setOnClickListener(v -> startActivity(new Intent(this, WebViewActivity.class)));
        if (!Utils.hasWebView(this)) fabWebView.setVisibility(View.GONE);

        recyclerViewLayout.startLoading();

        if (Prefs.getBoolean(PK.A2_CHECK_VERSION) && ((ThisApplication) getApplication()).shouldCheckVersion()) {
            ((ThisApplication) getApplication()).checkedVersion();
            doVersionCheck();
        }

        String shortcutAction = getIntent().getStringExtra("shortcutAction");
        Uri shareData = getIntent().getParcelableExtra("shareData");
        if (shortcutAction != null) {
            switch (shortcutAction) {
                case LoadingActivity.SHORTCUT_ADD_URI:
                    startActivity(new Intent(this, AddUriActivity.class));
                    break;
                case LoadingActivity.SHORTCUT_ADD_TORRENT:
                    startActivity(new Intent(this, AddTorrentActivity.class));
                    break;
                case LoadingActivity.SHORTCUT_ADD_METALINK:
                    startActivity(new Intent(this, AddMetalinkActivity.class));
                    break;
                case LoadingActivity.SHORTCUT_SEARCH:
                    startActivity(new Intent(this, SearchActivity.class));
                    break;
                case LoadingActivity.SHORTCUT_WEB_VIEW:
                    startActivity(new Intent(this, WebViewActivity.class));
                    break;
                case LoadingActivity.SHORTCUT_BATCH_ADD:
                    startActivity(new Intent(this, BatchAddActivity.class));
                    break;
            }
        } else if (shareData != null) {
            String scheme = shareData.getScheme();
            if (scheme != null) {
                if ("magnet".equals(scheme) || "http".equals(scheme) || "https".equals(scheme) || "ftp".equals(scheme) || "sftp".equals(scheme)) {
                    processUrl(shareData);
                } else {
                    AskPermission.ask(this, Manifest.permission.READ_EXTERNAL_STORAGE, new AskPermission.Listener() {
                        @Override
                        public void permissionGranted(@NonNull String permission) {
                            processFileUri(shareData);
                        }

                        @Override
                        public void permissionDenied(@NonNull String permission) {
                            Toaster.with(MainActivity.this).message(R.string.readPermissionDenied).show();
                        }

                        @Override
                        public void askRationale(@NonNull AlertDialog.Builder builder) {
                            _sharedUri = shareData;

                            builder.setTitle(R.string.readExternalStorageRequest_title)
                                    .setMessage(R.string.readExternalStorageRequest_base64Message);
                        }
                    });
                }
            }
        }

        secondSpace = findViewById(R.id.main_secondSpace); // Tablet layout stuff (sw600dp)
        if (secondSpace != null) {
            secondSpaceMessage = secondSpace.findViewById(R.id.mainSecondSpace_message);
            secondSpaceContainer = secondSpace.findViewById(R.id.mainSecondSpace_container);
            secondSpaceTabs = secondSpace.findViewById(R.id.mainSecondSpace_tabs);
            secondSpacePager = secondSpace.findViewById(R.id.mainSecondSpace_pager);
            secondSpacePager.setOffscreenPageLimit(3);
            secondSpaceTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    secondSpacePager.setCurrentItem(tab.getPosition());
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                }
            });
            hideSecondSpace();
        }

        tutorialManager = new TutorialManager(this,
                Discovery.DOWNLOADS_CARDS, Discovery.DOWNLOADS_TOOLBAR);
    }

    private void doVersionCheck() {
        GitHubApi.getLatestVersion(new GitHubApi.OnRelease() {
            @Override
            public void onRelease(@NonNull final String latestVersion) {
                helper.request(AriaRequests.getVersion(), new AbstractClient.OnResult<VersionInfo>() {
                    @Override
                    public void onResult(@NonNull VersionInfo result) {
                        try {
                            String skipVersion = profilesManager.getCurrent().shouldSkipVersionCheck(MainActivity.this);
                            if (!Objects.equals(skipVersion, latestVersion) && !Objects.equals(result.version, latestVersion))
                                showOutdatedDialog(latestVersion, result.version);
                        } catch (ProfilesManager.NoCurrentProfileException ignored) {
                        }
                    }

                    @Override
                    public void onException(@NonNull @NotNull Exception ex) {
                        Log.e(TAG, "Cannot get aria2 version.", ex);
                    }
                });
            }

            @Override
            public void onException(@NonNull Exception ex) {
                Log.e(TAG, "Cannot get latest aria2 version.", ex);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (adapter != null) adapter.activityDestroying(this);
        super.onDestroy();

        ((ThisApplication) getApplication()).removeAria2UiListener(this);
    }

    private void processUrl(@NonNull Uri shareData) {
        URI uri;
        try {
            uri = new URI(shareData.toString());
        } catch (URISyntaxException ex) {
            Log.e(TAG, "Invalid URL: " + shareData, ex);
            Toaster.with(this).message(R.string.invalidFile).show();
            return;
        }

        AddUriActivity.startAndAdd(this, uri);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && _sharedUri != null)
            processFileUri(_sharedUri);
        else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void showOutdatedDialog(@NonNull String latest, @NonNull String current) {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.outdated_aria2)
                .setMessage(getString(R.string.outdated_aria2_message, current, latest))
                .setNeutralButton(R.string.skipThisVersion, (dialogInterface, i) -> {
                    try {
                        profilesManager.getCurrent().skipVersionCheck(MainActivity.this, latest);
                    } catch (ProfilesManager.NoCurrentProfileException ignored) {
                    }
                })
                .setPositiveButton(android.R.string.ok, null);

        showDialog(builder);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (drawerManager != null) drawerManager.onTogglerConfigurationChanged(newConfig);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (drawerManager != null) drawerManager.syncTogglerState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (drawerManager != null) {
            drawerManager.syncTogglerState();
            if (profilesManager != null)
                drawerManager.refreshProfiles(profilesManager.getProfiles());
        }

        if (fabMenu != null) fabMenu.collapseImmediately();

        try {
            if (profilesManager != null) profilesManager.reloadCurrentProfile();
        } catch (IOException | JSONException | ProfilesManager.NoCurrentProfileException ex) {
            Log.e(TAG, "Failed reloading profile.", ex);
            NetInstanceHolder.close();
            profilesManager.unsetLastProfile();
            LoadingActivity.startActivity(this, ex);
            return;
        }

        FetchHelper.updateDownloadCount(this, this);

        ((ThisApplication) getApplication()).addAria2UiListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        getMenuInflater().inflate(R.menu.main_sorting, menu.findItem(R.id.main_sort).getSubMenu());

        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        MenuItem searchItem = menu.findItem(R.id.main_search);
        searchItem.setOnActionExpandListener(this);
        searchView = (SearchView) searchItem.getActionView();

        if (searchManager != null && searchView != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconifiedByDefault(false);
            searchView.setOnCloseListener(this);
            searchView.setOnQueryTextListener(this);
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        SubMenu sortingMenu = menu.findItem(R.id.main_sort).getSubMenu();
        if (sortingMenu != null) {
            sortingMenu.setGroupCheckable(0, true, true);

            DownloadCardsAdapter.SortBy sorting = DownloadCardsAdapter.SortBy.valueOf(Prefs.getString(PK.A2_MAIN_SORTING));
            MenuItem item;
            switch (sorting) {
                case NAME:
                    item = sortingMenu.findItem(R.id.mainSort_name);
                    break;
                default:
                case STATUS:
                    item = sortingMenu.findItem(R.id.mainSort_status);
                    break;
                case PROGRESS:
                    item = sortingMenu.findItem(R.id.mainSort_progress);
                    break;
                case DOWNLOAD_SPEED:
                    item = sortingMenu.findItem(R.id.mainSort_downloadSpeed);
                    break;
                case UPLOAD_SPEED:
                    item = sortingMenu.findItem(R.id.mainSort_uploadSpeed);
                    break;
                case COMPLETED_LENGTH:
                    item = sortingMenu.findItem(R.id.mainSort_completedLength);
                    break;
                case LENGTH:
                    item = sortingMenu.findItem(R.id.mainSort_length);
                    break;
            }

            item.setChecked(true);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (fabMenu != null && fabMenu.isExpanded()) {
            fabMenu.collapse();
            return;
        } else if (secondSpace != null && secondSpaceAdapter != null) {
            OnBackPressed visible = secondSpaceAdapter.getFragments().get(secondSpacePager.getCurrentItem());
            if (!visible.canGoBack(-1)) return;
            hideSecondSpace();
            return;
        }

        super.onBackPressed();
    }

    private void showFilteringDialog() {
        final Download.Status[] filters = new Download.Status[]{Download.Status.ACTIVE, Download.Status.PAUSED, Download.Status.WAITING, Download.Status.ERROR, Download.Status.REMOVED, Download.Status.COMPLETE};
        CharSequence[] stringFilters = new CharSequence[filters.length];

        for (int i = 0; i < filters.length; i++)
            stringFilters[i] = filters[i].getFormal(this, true);

        final boolean[] checkedFilters = new boolean[filters.length];
        Set<String> checkedFiltersSet = Prefs.getSet(PK.A2_MAIN_FILTERS);

        for (String checkedFilter : checkedFiltersSet) {
            Download.Status filter = Download.Status.valueOf(checkedFilter);
            int pos = CommonUtils.indexOf(filters, filter);
            if (pos != -1) checkedFilters[pos] = true;
        }

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.filters)
                .setMultiChoiceItems(stringFilters, checkedFilters, (dialog, which, isChecked) -> checkedFilters[which] = isChecked)
                .setPositiveButton(R.string.apply, (dialog, which) -> {
                    List<Download.Status> toApplyFilters = new ArrayList<>();
                    for (int i = 0; i < checkedFilters.length; i++)
                        if (!checkedFilters[i]) toApplyFilters.add(filters[i]);

                    if (adapter != null) adapter.setFilters(toApplyFilters);
                    Set<String> set = new HashSet<>();
                    for (int i = 0; i < checkedFilters.length; i++)
                        if (checkedFilters[i]) set.add(filters[i].name());

                    Prefs.putSet(PK.A2_MAIN_FILTERS, set);

                    updateFiltersVerbose();
                })
                .setNegativeButton(android.R.string.cancel, null);

        showDialog(builder);
    }

    private void updateFiltersVerbose() {
        Set<String> filters = Prefs.getSet(PK.A2_MAIN_FILTERS);
        if (filters.size() == Download.Status.values().length) {
            filtersVerbose.setVisibility(View.GONE);
        } else {
            filtersVerbose.setVisibility(View.VISIBLE);
            filtersVerbose.setText(getString(R.string.filtersShowingOnly, CommonUtils.join(filters, ", ",
                    obj -> Download.Status.valueOf(obj).getFormal(MainActivity.this, true)),
                    adapter == null ? 0 : adapter.getItemCount()));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Filters
            case R.id.main_filter:
                showFilteringDialog();
                return true;
            case R.id.main_pauseAll:
                pauseAll();
                return true;
            case R.id.main_unpauseAll:
                unpauseAll();
                return true;
            case R.id.main_purgeDownloadResult:
                purgeDownloadResult();
                return true;
            // Sorting
            default:
                return handleSorting(item);
        }
    }

    private void pauseAll() {
        helper.request(AriaRequests.pauseAll(), new AbstractClient.OnSuccess() {
            @Override
            public void onSuccess() {
                Toaster.with(MainActivity.this).message(R.string.pausedAll).show();
            }

            @Override
            public void onException(@NonNull @NotNull Exception ex) {
                Log.e(TAG, "Failed pausing all.", ex);
                Toaster.with(MainActivity.this).message(R.string.failedAction).show();
            }
        });
    }

    private void unpauseAll() {
        helper.request(AriaRequests.unpauseAll(), new AbstractClient.OnSuccess() {
            @Override
            public void onSuccess() {
                Toaster.with(MainActivity.this).message(R.string.resumedAll).show();
            }

            @Override
            public void onException(@NonNull @NotNull Exception ex) {
                Log.e(TAG, "Failed unpausing all.", ex);
                Toaster.with(MainActivity.this).message(R.string.failedAction).show();
            }
        });
    }

    private void purgeDownloadResult() {
        helper.request(AriaRequests.purgeDownloadResults(), new AbstractClient.OnSuccess() {
            @Override
            public void onSuccess() {
                Toaster.with(MainActivity.this).message(R.string.purgedDownloadResult).show();
            }

            @Override
            public void onException(@NonNull Exception ex) {
                Log.e(TAG, "Failed purging download results.", ex);
                Toaster.with(MainActivity.this).message(R.string.failedAction).show();
            }
        });
    }

    private boolean handleSorting(MenuItem clicked) {
        clicked.setChecked(true);
        switch (clicked.getItemId()) {
            case R.id.mainSort_name:
                handleSortingReal(DownloadCardsAdapter.SortBy.NAME);
                return true;
            case R.id.mainSort_status:
                handleSortingReal(DownloadCardsAdapter.SortBy.STATUS);
                return true;
            case R.id.mainSort_progress:
                handleSortingReal(DownloadCardsAdapter.SortBy.PROGRESS);
                return true;
            case R.id.mainSort_downloadSpeed:
                handleSortingReal(DownloadCardsAdapter.SortBy.DOWNLOAD_SPEED);
                return true;
            case R.id.mainSort_uploadSpeed:
                handleSortingReal(DownloadCardsAdapter.SortBy.UPLOAD_SPEED);
                return true;
            case R.id.mainSort_length:
                handleSortingReal(DownloadCardsAdapter.SortBy.LENGTH);
                return true;
            case R.id.mainSort_completedLength:
                handleSortingReal(DownloadCardsAdapter.SortBy.COMPLETED_LENGTH);
                return true;
            default:
                return false;
        }
    }

    private void handleSortingReal(DownloadCardsAdapter.SortBy sorting) {
        if (adapter != null) adapter.sort(sorting);
        Prefs.putString(PK.A2_MAIN_SORTING, sorting.name());
    }

    @Override
    public void onMenuExpanded() {
        final View mask = findViewById(R.id.main_mask);
        mask.setVisibility(View.VISIBLE);
        mask.setClickable(true);
        mask.setOnClickListener(view -> fabMenu.collapse());
    }

    @Override
    public void onMenuCollapsed() {
        final View mask = findViewById(R.id.main_mask);
        mask.setVisibility(View.GONE);
        mask.setClickable(false);
    }

    private void showSecondSpace(@NonNull DownloadWithUpdate download) {
        secondSpaceAdapter = new PagerAdapter<>(getSupportFragmentManager(),
                InfoFragment.getInstance(this, download.gid),
                download.update().isTorrent() ? PeersFragment.getInstance(this, download.gid) : ServersFragment.getInstance(this, download.gid),
                FilesFragment.getInstance(this, download.gid));

        secondSpacePager.setAdapter(secondSpaceAdapter);
        secondSpaceTabs.setupWithViewPager(secondSpacePager);

        secondSpaceContainer.setVisibility(View.VISIBLE);
        secondSpaceMessage.hide();
    }

    @Override
    public void hideSecondSpace() {
        secondSpaceMessage.info(R.string.secondSpace_selectDownload);
        secondSpaceContainer.setVisibility(View.GONE);
        secondSpaceAdapter = null;
    }

    @Override
    public void onMoreClick(@NonNull DownloadWithUpdate item) {
        if (secondSpace != null) showSecondSpace(item);
        else MoreAboutDownloadActivity.start(this, item);
    }

    @Override
    public void onItemCountUpdated(int count) {
        if (drawerManager != null) drawerManager.updateBadge(DrawerItem.HOME, count);

        if (count == 0) {
            if (Prefs.getSet(PK.A2_MAIN_FILTERS).size() == Download.Status.values().length
                    && (searchView == null || searchView.getQuery() == null || searchView.getQuery().length() == 0))
                recyclerViewLayout.showInfo(R.string.noDownloads_addOne);
            else
                recyclerViewLayout.showInfo(R.string.noDownloads_changeFilters);
        } else {
            recyclerViewLayout.showList();
        }

        tutorialManager.tryShowingTutorials(this);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (adapter != null) adapter.filterWithQuery(query);
        updateFiltersVerbose();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return onQueryTextSubmit(newText);
    }

    @Override
    public boolean onClose() {
        searchView.setQuery(null, true);
        return false;
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        onClose();
        return true;
    }

    @Override
    public void refreshed() {
        adapter = new DownloadCardsAdapter(this, new ArrayList<>(), this);
        recyclerViewLayout.loadListData(adapter);
        recyclerViewLayout.startLoading();
        setupAdapterFiltersAndSorting();
    }

    @Override
    public boolean canShow(@NonNull BaseTutorial tutorial) {
        if (tutorial instanceof DownloadsToolbarTutorial)
            return ((DownloadsToolbarTutorial) tutorial).canShow(toolbar, adapter);
        else if (tutorial instanceof DownloadCardsTutorial)
            return ((DownloadCardsTutorial) tutorial).canShow(adapter);

        return false;
    }

    @Override
    public boolean buildSequence(@NonNull BaseTutorial tutorial) {
        if (tutorial instanceof DownloadsToolbarTutorial)
            ((DownloadsToolbarTutorial) tutorial).buildSequence(toolbar);
        else if (tutorial instanceof DownloadCardsTutorial)
            return ((DownloadCardsTutorial) tutorial).buildSequence(recyclerViewLayout.list());

        return true;
    }

    @Override
    public void onFetchDownloadCount(int count) {
        if (drawerManager != null) drawerManager.updateBadge(DrawerItem.DIRECT_DOWNLOAD, count);
    }

    @Override
    public void onUpdateLogs(@NonNull List<Aria2Ui.LogMessage> msg) {
    }

    @Override
    public void onMessage(@NonNull Aria2Ui.LogMessage msg) {
    }

    @Override
    public void updateUi(boolean on) {
        MultiProfile current;
        try {
            current = profilesManager.getCurrent();
        } catch (ProfilesManager.NoCurrentProfileException ex) {
            return;
        }

        if (!on && current.isInAppDownloader()) {
            ((ThisApplication) getApplication()).removeAria2UiListener(this);
            NetInstanceHolder.close();
            profilesManager.unsetLastProfile();
            LoadingActivity.startActivity(this);
        }
    }
}