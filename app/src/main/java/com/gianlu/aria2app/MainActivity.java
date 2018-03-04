package com.gianlu.aria2app;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.os.TransactionTooLargeException;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.gianlu.aria2app.Activities.AddMetalinkActivity;
import com.gianlu.aria2app.Activities.AddTorrentActivity;
import com.gianlu.aria2app.Activities.AddUriActivity;
import com.gianlu.aria2app.Activities.DirectDownloadActivity;
import com.gianlu.aria2app.Activities.EditProfileActivity;
import com.gianlu.aria2app.Activities.MoreAboutDownloadActivity;
import com.gianlu.aria2app.Activities.SearchActivity;
import com.gianlu.aria2app.Adapters.DownloadCardsAdapter;
import com.gianlu.aria2app.Downloader.DownloaderUtils;
import com.gianlu.aria2app.Main.DrawerConst;
import com.gianlu.aria2app.Main.UpdateUI;
import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.BaseUpdater;
import com.gianlu.aria2app.NetIO.ErrorHandler;
import com.gianlu.aria2app.NetIO.GitHubApi;
import com.gianlu.aria2app.NetIO.HTTPing;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.GlobalStats;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.OnRefresh;
import com.gianlu.aria2app.NetIO.Search.SearchUtils;
import com.gianlu.aria2app.NetIO.UpdaterActivity;
import com.gianlu.aria2app.NetIO.WebSocketing;
import com.gianlu.aria2app.Options.OptionsUtils;
import com.gianlu.aria2app.ProfilesManager.CustomProfilesAdapter;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.Services.NotificationService;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Drawer.BaseDrawerItem;
import com.gianlu.commonutils.Drawer.DrawerManager;
import com.gianlu.commonutils.Drawer.Initializer;
import com.gianlu.commonutils.Drawer.ProfilesAdapter;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.commonutils.SuperTextView;
import com.gianlu.commonutils.Toaster;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;

import org.json.JSONException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MainActivity extends UpdaterActivity implements FloatingActionsMenu.OnFloatingActionsMenuUpdateListener, JTA2.IUnpause, JTA2.IRemove, JTA2.IPause, DrawerManager.IDrawerListener<MultiProfile>, DrawerManager.ISetup<MultiProfile>, DownloadCardsAdapter.IAdapter, JTA2.IRestart, JTA2.IMove, SearchView.OnQueryTextListener, SearchView.OnCloseListener, MenuItem.OnActionExpandListener, AbstractClient.OnConnectivityChanged, ServiceConnection, UpdateUI.IUI, OnRefresh {
    private static final int REQUEST_READ_CODE = 12;
    private DrawerManager<MultiProfile> drawerManager;
    private FloatingActionsMenu fabMenu;
    private DownloadCardsAdapter adapter;
    private SearchView searchView;
    private Uri _sharedUri;
    private Toolbar toolbar;
    private boolean isShowingHint = false;
    private TextView active;
    private TextView paused;
    private ImageButton toggleChart;
    private LineChart overallChart;
    private TextView stopped;
    private InternalBroadcastReceiver broadcastReceiver;
    private Messenger downloaderMessenger = null;
    private RecyclerViewLayout recyclerViewLayout;

    @Override
    protected void onRestart() {
        super.onRestart();

        if (drawerManager != null && drawerManager.isOpen())
            drawerManager.refreshProfiles(ProfilesManager.get(this).getProfiles());
    }

    @Override
    public boolean onMenuItemSelected(BaseDrawerItem which) {
        switch (which.id) {
            case DrawerConst.HOME:
                refresh(this);
                return true;
            case DrawerConst.DIRECT_DOWNLOAD:
                startActivity(new Intent(MainActivity.this, DirectDownloadActivity.class));
                return false;
            case DrawerConst.QUICK_OPTIONS:
                OptionsUtils.showGlobalDialog(this, true);
                return true;
            case DrawerConst.GLOBAL_OPTIONS:
                OptionsUtils.showGlobalDialog(this, false);
                return true;
            case DrawerConst.PREFERENCES:
                startActivity(new Intent(MainActivity.this, PreferencesActivity.class));
                return false;
            case DrawerConst.SUPPORT:
                CommonUtils.sendEmail(MainActivity.this, getString(R.string.app_name), null);
                return true;
            case DrawerConst.ABOUT_ARIA2:
                showAboutDialog();
                return true;
            default:
                return true;
        }
    }

    @Override
    public void onProfileSelected(final MultiProfile profile) {
        ProfilesManager.get(this).setLastProfile(this, profile);
        LoadingActivity.startActivity(this);
    }

    @Override
    public void addProfile() {
        EditProfileActivity.start(this, false);
    }

    @Override
    public void editProfile(final List<MultiProfile> items) {
        CommonUtils.showDialog(this, new AlertDialog.Builder(this)
                .setTitle(R.string.editProfile)
                .setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditProfileActivity.start(MainActivity.this, items.get(which));
                    }
                })
                .setNegativeButton(android.R.string.cancel, null));
    }

    private void showAboutDialog() {
        final JTA2 jta2;
        try {
            jta2 = JTA2.instantiate(MainActivity.this);
        } catch (JTA2.InitializingException ex) {
            Toaster.show(MainActivity.this, Utils.Messages.FAILED_GATHERING_INFORMATION, ex);
            return;
        }

        final ProgressDialog pd = CommonUtils.fastIndeterminateProgressDialog(MainActivity.this, R.string.gathering_information);
        CommonUtils.showDialog(MainActivity.this, pd);
        jta2.getVersion(new JTA2.IVersion() {
            @Override
            public void onVersion(List<String> rawFeatures, String version) {
                final LinearLayout layout = new LinearLayout(MainActivity.this);
                layout.setOrientation(LinearLayout.VERTICAL);
                int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
                layout.setPadding(padding, padding, padding, padding);
                layout.addView(new SuperTextView(MainActivity.this, R.string.version, version));
                layout.addView(new SuperTextView(MainActivity.this, R.string.features, CommonUtils.join(rawFeatures, ", ")));

                jta2.getSessionInfo(new JTA2.ISession() {
                    @Override
                    public void onSessionInfo(String sessionID) {
                        layout.addView(new SuperTextView(MainActivity.this, R.string.sessionId, sessionID));
                        pd.dismiss();

                        CommonUtils.showDialog(MainActivity.this, new AlertDialog.Builder(MainActivity.this)
                                .setTitle(R.string.about_aria2)
                                .setView(layout)
                                .setNeutralButton(R.string.saveSession, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        jta2.saveSession(new JTA2.ISuccess() {
                                            @Override
                                            public void onSuccess() {
                                                Toaster.show(MainActivity.this, Utils.Messages.SESSION_SAVED);
                                            }

                                            @Override
                                            public void onException(Exception exception) {
                                                Toaster.show(MainActivity.this, Utils.Messages.FAILED_SAVE_SESSION, exception);
                                            }
                                        });
                                    }
                                })
                                .setPositiveButton(android.R.string.ok, null));
                    }

                    @Override
                    public void onException(Exception ex) {
                        Toaster.show(MainActivity.this, Utils.Messages.FAILED_GATHERING_INFORMATION, ex);
                        pd.dismiss();
                    }
                });
            }

            @Override
            public void onException(Exception ex) {
                Toaster.show(MainActivity.this, Utils.Messages.FAILED_GATHERING_INFORMATION, ex);
                pd.dismiss();
            }
        });
    }

    @SuppressWarnings("ConstantConditions")
    private void setupAdapterFiltersAndSorting() {
        List<Download.Status> filters = new ArrayList<>(Arrays.asList(Download.Status.values()));
        Set<String> checkedFiltersSet = Prefs.getSet(this, PKeys.A2_MAIN_FILTERS, new HashSet<>(Download.Status.stringValues()));
        for (String filter : checkedFiltersSet) filters.remove(Download.Status.valueOf(filter));
        adapter.setFilters(filters);

        adapter.sort(DownloadCardsAdapter.SortBy.valueOf(Prefs.getString(this, PKeys.A2_MAIN_SORTING, DownloadCardsAdapter.SortBy.STATUS.name())));
    }

    private void processFileUri(Uri uri) {
        String mimeType;
        if (Objects.equals(uri.getScheme(), "file")) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(uri.getPath());
            if (extension != null)
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            else
                mimeType = null;
        } else {
            try (Cursor cursor = getContentResolver().query(uri, new String[]{MediaStore.MediaColumns.MIME_TYPE}, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst() && cursor.getColumnCount() > 0) {
                    mimeType = cursor.getString(0);
                } else {
                    Toaster.show(this, Utils.Messages.INVALID_FILE, new Exception("Cursor is empty: " + uri));
                    return;
                }
            } catch (RuntimeException ex) {
                Toaster.show(this, Utils.Messages.INVALID_FILE, ex);
                return;
            }
        }

        if (mimeType != null) {
            if (Objects.equals(mimeType, "application/x-bittorrent")) {
                AddTorrentActivity.startAndAdd(this, uri);
            } else if (Objects.equals(mimeType, "application/metalink4+xml") || Objects.equals(mimeType, "application/metalink+xml")) {
                AddMetalinkActivity.startAndAdd(this, uri);
            } else {
                Toaster.show(this, Utils.Messages.INVALID_FILE, new Exception("File type not supported: " + mimeType));
            }
        } else {
            Toaster.show(this, Utils.Messages.INVALID_FILE, new Exception("Cannot determine file type: " + uri));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name);

        toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        drawerManager = new DrawerManager<>(new Initializer<>(this, (DrawerLayout) findViewById(R.id.main_drawer), toolbar, this)
                .addMenuItem(new BaseDrawerItem(DrawerConst.HOME, R.drawable.ic_home_black_48dp, getString(R.string.home)))
                .addMenuItem(new BaseDrawerItem(DrawerConst.DIRECT_DOWNLOAD, R.drawable.ic_cloud_download_black_48dp, getString(R.string.directDownload)))
                .addMenuItem(new BaseDrawerItem(DrawerConst.QUICK_OPTIONS, R.drawable.ic_favorite_black_48dp, getString(R.string.quickGlobalOptions)))
                .addMenuItem(new BaseDrawerItem(DrawerConst.GLOBAL_OPTIONS, R.drawable.ic_list_black_48dp, getString(R.string.globalOptions)))
                .addMenuItem(new BaseDrawerItem(DrawerConst.ABOUT_ARIA2, R.drawable.ic_cloud_black_48dp, getString(R.string.about_aria2)))
                .addMenuItemSeparator()
                .addMenuItem(new BaseDrawerItem(DrawerConst.PREFERENCES, R.drawable.ic_settings_black_48dp, getString(R.string.preferences)))
                .addMenuItem(new BaseDrawerItem(DrawerConst.SUPPORT, R.drawable.ic_report_problem_black_48dp, getString(R.string.support)))
                .addProfiles(ProfilesManager.get(this).getProfiles()));

        ProfilesManager manager = ProfilesManager.get(this);
        MultiProfile currentProfile;
        try {
            currentProfile = manager.getCurrent(this);
        } catch (ProfilesManager.NoCurrentProfileException ex) {
            Logging.log(ex);
            WebSocketing.clear();
            HTTPing.clear();
            manager.unsetLastProfile(this);
            LoadingActivity.startActivity(this, ex);
            return;
        }

        drawerManager.setCurrentProfile(currentProfile).setDrawerListener(this);
        AbstractClient.addConnectivityListener(this);

        setTitle(currentProfile.getProfileName(this) + " - " + getString(R.string.app_name));

        active = findViewById(R.id.main_active);
        paused = findViewById(R.id.main_paused);
        stopped = findViewById(R.id.main_stopped);
        overallChart = findViewById(R.id.main_overallChart);
        final FrameLayout overallChartContainer = findViewById(R.id.main_overallChartContainer);
        toggleChart = findViewById(R.id.main_toggleChart);
        toggleChart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CommonUtils.handleCollapseClick(toggleChart, overallChartContainer);
            }
        });

        recyclerViewLayout = findViewById(R.id.main_recyclerViewLayout);
        recyclerViewLayout.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        recyclerViewLayout.enableSwipeRefresh(R.color.colorAccent, R.color.colorMetalink, R.color.colorTorrent);
        recyclerViewLayout.setRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh(MainActivity.this);
            }
        });

        fabMenu = findViewById(R.id.main_fab);
        fabMenu.setOnFloatingActionsMenuUpdateListener(this);

        FloatingActionButton fabSearch = findViewById(R.id.mainFab_search);
        fabSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SearchActivity.class));
            }
        });
        FloatingActionButton fabAddURI = findViewById(R.id.mainFab_addURI);
        fabAddURI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AddUriActivity.class));
            }
        });
        final FloatingActionButton fabAddTorrent = findViewById(R.id.mainFab_addTorrent);
        fabAddTorrent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AddTorrentActivity.class));
            }
        });
        final FloatingActionButton fabAddMetalink = findViewById(R.id.mainFab_addMetalink);
        fabAddMetalink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AddMetalinkActivity.class));
            }
        });

        if (Prefs.getBoolean(this, PKeys.A2_ENABLE_NOTIFS, true))
            NotificationService.start(this, false);

        adapter = new DownloadCardsAdapter(this, new ArrayList<Download>(), this);
        recyclerViewLayout.loadListData(adapter);
        recyclerViewLayout.startLoading();
        setupAdapterFiltersAndSorting();

        if (((ThisApplication) getApplication()).isFirstStart()) {
            SearchUtils.get().cacheSearchEngines();
            ((ThisApplication) getApplication()).firstStarted();
            if (Prefs.getBoolean(this, PKeys.A2_CHECK_VERSION, true))
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
            }
        } else if (shareData != null) {
            String scheme = shareData.getScheme();
            if (scheme != null) {
                if (scheme.equals("magnet") || scheme.equals("http") || scheme.equals("https") || scheme.equals("ftp") || scheme.equals("sftp")) {
                    processUrl(shareData);
                } else {
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        _sharedUri = shareData;
                        Utils.requestReadPermission(this, R.string.readExternalStorageRequest_base64Message, REQUEST_READ_CODE);
                    } else {
                        processFileUri(shareData);
                    }
                }
            }
        }

        DownloaderUtils.startService(this, Prefs.getBoolean(this, PKeys.DD_RESUME, true));
    }

    private void doVersionCheck() {
        GitHubApi.getLatestVersion(new GitHubApi.IRelease() {
            @Override
            public void onRelease(final String latestVersion) {
                JTA2 jta2;
                try {
                    jta2 = JTA2.instantiate(MainActivity.this);
                } catch (JTA2.InitializingException ex) {
                    Logging.log(ex);
                    return;
                }

                jta2.getVersion(new JTA2.IVersion() {
                    @Override
                    public void onVersion(List<String> rawFeatures, String version) {
                        String skipVersion = Prefs.getString(MainActivity.this, PKeys.A2_CHECK_VERSION_SKIP, null);
                        if (!Objects.equals(skipVersion, latestVersion) && !Objects.equals(version, latestVersion))
                            showOutdatedDialog(latestVersion, version);
                    }

                    @Override
                    public void onException(Exception ex) {
                        Logging.log(ex);
                    }
                });
            }

            @Override
            public void onException(Exception ex) {
                Logging.log(ex);
            }
        });
    }

    @Override
    protected void onDestroy() {
        AbstractClient.removeConnectivityListener(this);

        if (adapter != null) adapter.activityDestroying(this);

        super.onDestroy();
    }

    private void processUrl(Uri shareData) {
        URI uri;
        try {
            uri = new URI(shareData.toString());
        } catch (URISyntaxException ex) {
            Toaster.show(this, Utils.Messages.INVALID_FILE, new Exception("Cannot identify shared file/url: " + shareData, ex));
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

    private void showOutdatedDialog(final String latest, String current) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.outdated_aria2)
                .setMessage(getString(R.string.outdated_aria2_message, current, latest))
                .setNeutralButton(R.string.skipThisVersion, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Prefs.putString(MainActivity.this, PKeys.A2_CHECK_VERSION_SKIP, latest);
                    }
                })
                .setPositiveButton(android.R.string.ok, null);

        CommonUtils.showDialog(this, builder);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
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
        if (drawerManager != null) drawerManager.syncTogglerState();
        if (fabMenu != null) fabMenu.collapseImmediately();

        try {
            ProfilesManager.get(this).reloadCurrentProfile(this);
        } catch (IOException | JSONException | ProfilesManager.NoCurrentProfileException ex) {
            Logging.log(ex);
            WebSocketing.clear();
            HTTPing.clear();
            ProfilesManager.get(this).unsetLastProfile(this);
            LoadingActivity.startActivity(this, ex);
            return;
        }

        if (downloaderMessenger != null) DownloaderUtils.refreshCount(downloaderMessenger);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        getMenuInflater().inflate(R.menu.main_sorting, menu.findItem(R.id.main_sort).getSubMenu());

        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        MenuItem searchItem = menu.findItem(R.id.main_search);
        searchItem.setOnActionExpandListener(this);
        searchView = (SearchView) searchItem.getActionView();

        if (searchManager != null) {
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

            DownloadCardsAdapter.SortBy sorting = DownloadCardsAdapter.SortBy.valueOf(Prefs.getString(this, PKeys.A2_MAIN_SORTING, DownloadCardsAdapter.SortBy.STATUS.name()));
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
        if (fabMenu != null && fabMenu.isExpanded()) fabMenu.collapse();
        else super.onBackPressed();
    }

    private void showFilteringDialog() {
        final Download.Status[] filters = new Download.Status[]{Download.Status.ACTIVE, Download.Status.PAUSED, Download.Status.WAITING, Download.Status.ERROR, Download.Status.REMOVED, Download.Status.COMPLETE};
        CharSequence[] stringFilters = new CharSequence[filters.length];

        for (int i = 0; i < filters.length; i++)
            stringFilters[i] = filters[i].getFormal(this, true);

        final boolean[] checkedFilters = new boolean[filters.length];
        Set<String> checkedFiltersSet = Prefs.getSet(this, PKeys.A2_MAIN_FILTERS, null);

        if (checkedFiltersSet == null) {
            for (int i = 0; i < checkedFilters.length; i++) checkedFilters[i] = true;
        } else {
            for (String checkedFilter : checkedFiltersSet) {
                Download.Status filter = Download.Status.valueOf(checkedFilter);
                int pos = CommonUtils.indexOf(filters, filter);
                if (pos != -1) checkedFilters[pos] = true;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.filters)
                .setMultiChoiceItems(stringFilters, checkedFilters, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        checkedFilters[which] = isChecked;
                    }
                })
                .setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        List<Download.Status> toApplyFilters = new ArrayList<>();
                        for (int i = 0; i < checkedFilters.length; i++)
                            if (!checkedFilters[i]) toApplyFilters.add(filters[i]);

                        if (adapter != null) adapter.setFilters(toApplyFilters);
                        Set<String> set = new HashSet<>();
                        for (int i = 0; i < checkedFilters.length; i++)
                            if (checkedFilters[i]) set.add(filters[i].name());

                        Prefs.putSet(MainActivity.this, PKeys.A2_MAIN_FILTERS, set);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);

        CommonUtils.showDialog(this, builder);
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
        final JTA2 jta2;
        try {
            jta2 = JTA2.instantiate(this);
        } catch (JTA2.InitializingException ex) {
            onException(ex);
            return;
        }

        jta2.pauseAll(new JTA2.ISuccess() {
            boolean retried = false;

            @Override
            public void onSuccess() {
                Toaster.show(MainActivity.this, Utils.Messages.PAUSED_ALL);
            }

            @Override
            public void onException(Exception ex) {
                if (!retried) jta2.forcePauseAll(this);
                else MainActivity.this.onException(ex);
                retried = true;
            }
        });
    }

    private void unpauseAll() {
        final JTA2 jta2;
        try {
            jta2 = JTA2.instantiate(this);
        } catch (JTA2.InitializingException ex) {
            onException(ex);
            return;
        }

        jta2.unpauseAll(new JTA2.ISuccess() {
            @Override
            public void onSuccess() {
                Toaster.show(MainActivity.this, Utils.Messages.RESUMED_ALL);
            }

            @Override
            public void onException(Exception ex) {
                MainActivity.this.onException(ex);
            }
        });
    }

    private void purgeDownloadResult() {
        final JTA2 jta2;
        try {
            jta2 = JTA2.instantiate(this);
        } catch (JTA2.InitializingException ex) {
            onException(ex);
            return;
        }

        jta2.purgeDownloadResult(new JTA2.ISuccess() {
            @Override
            public void onSuccess() {
                Toaster.show(MainActivity.this, Utils.Messages.PURGED_DOWNLOAD_RESULT);
            }

            @Override
            public void onException(Exception ex) {
                MainActivity.this.onException(ex);
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
        }

        return false;
    }

    private void handleSortingReal(DownloadCardsAdapter.SortBy sorting) {
        if (adapter != null) adapter.sort(sorting);
        Prefs.putString(this, PKeys.A2_MAIN_SORTING, sorting.name());
    }

    @Override
    public void onMenuExpanded() {
        final View mask = findViewById(R.id.main_mask);
        mask.setVisibility(View.VISIBLE);
        mask.setClickable(true);
        mask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fabMenu.collapse();
            }
        });
    }

    @Override
    public void onMenuCollapsed() {
        final View mask = findViewById(R.id.main_mask);
        mask.setVisibility(View.GONE);
        mask.setClickable(false);
    }

    @Override
    public int getColorAccent() {
        return R.color.colorAccent;
    }

    @Override
    public int getHeaderBackground() {
        return R.drawable.drawer_background;
    }

    @Override
    public int getDrawerBadge() {
        return R.drawable.drawer_badge;
    }

    @Override
    public int getColorPrimary() {
        return R.color.colorPrimary;
    }

    @Nullable
    @Override
    public ProfilesAdapter<MultiProfile> getProfilesAdapter(Context context, List<MultiProfile> profiles, final DrawerManager.IDrawerListener<MultiProfile> listener) {
        return new CustomProfilesAdapter(context, profiles, new ProfilesAdapter.IAdapter<MultiProfile>() {
            @Override
            public void onProfileSelected(MultiProfile profile) {
                if (listener != null) listener.onProfileSelected(profile);
                if (drawerManager != null) drawerManager.performUnlock();
            }
        }, true, null);
    }

    @Override
    public int getColorPrimaryShadow() {
        return R.color.colorPrimary_shadow;
    }

    @Override
    public void onUpdateAdapter(List<Download> downloads) {
        if (adapter != null) {
            adapter.notifyItemsChanged(downloads);
            recyclerViewLayout.stopLoading();
        }

        String gid = getIntent().getStringExtra("gid");
        if (gid != null && !downloads.isEmpty()) {
            for (Download download : downloads) {
                if (Objects.equals(download.gid, gid)) {
                    onMoreClick(download);
                    getIntent().removeExtra("gid");
                }
            }
        }
    }

    @Override
    @SuppressLint("SetTextI18n")
    public void onUpdateGlobalStats(GlobalStats stats) {
        active.setText(String.valueOf(stats.numActive));
        paused.setText(String.valueOf(stats.numWaiting));
        stopped.setText(stats.numStopped + " (" + stats.numStoppedTotal + ")");

        LineData data = overallChart.getData();
        if (data == null) {
            Utils.setupChart(overallChart, true, R.color.white);
            data = overallChart.getData();
        }

        if (data != null) {
            int pos = data.getEntryCount() / 2 + 1;
            data.addEntry(new Entry(pos, stats.downloadSpeed), Utils.CHART_DOWNLOAD_SET);
            data.addEntry(new Entry(pos, stats.uploadSpeed), Utils.CHART_UPLOAD_SET);
            data.notifyDataChanged();
            overallChart.notifyDataSetChanged();

            overallChart.setVisibleXRangeMaximum(90);
            overallChart.moveViewToX(pos - 91);
        }
    }

    @Override
    public void onMoreClick(Download item) {
        try {
            MoreAboutDownloadActivity.start(this, item);
        } catch (RuntimeException ex) {
            if (ex.getCause() instanceof TransactionTooLargeException) {
                Crashlytics.log(item.toString() + " size: " + Utils.sizeOf(item));
                Crashlytics.logException(ex.getCause());
            } else {
                throw ex;
            }
        }
    }

    @Override
    public void onItemCountUpdated(int count) {
        if (drawerManager != null) drawerManager.updateBadge(DrawerConst.HOME, count);

        if (count == 0) {
            if (!recyclerViewLayout.isLoading())
                recyclerViewLayout.showMessage(R.string.noDownloads, false);
        } else {
            recyclerViewLayout.showList();
        }

        if (!isShowingHint && toolbar != null && count >= 5 && TutorialManager.shouldShowHintFor(this, TutorialManager.Discovery.TOOLBAR)) {
            isShowingHint = true;

            TapTargetSequence sequence = new TapTargetSequence(this)
                    .continueOnCancel(true);

            if (toolbar.findViewById(R.id.main_search) != null)
                sequence.target(TapTarget.forToolbarMenuItem(toolbar, R.id.main_search, getString(R.string.search), getString(R.string.search_desc)));

            if (toolbar.findViewById(R.id.main_filter) != null)
                sequence.target(TapTarget.forToolbarMenuItem(toolbar, R.id.main_filter, getString(R.string.filters), getString(R.string.filters_desc)));

            sequence.listener(new TapTargetSequence.Listener() {
                @Override
                public void onSequenceFinish() {
                    TutorialManager.setHintShown(MainActivity.this, TutorialManager.Discovery.TOOLBAR);
                    isShowingHint = false;
                }

                @Override
                public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {

                }

                @Override
                public void onSequenceCanceled(TapTarget lastTarget) {
                    onSequenceFinish();
                }
            }).start();
        }

        if (!isShowingHint && count >= 1 && TutorialManager.shouldShowHintFor(this, TutorialManager.Discovery.CARD)) {
            DownloadCardsAdapter.ViewHolder holder = (DownloadCardsAdapter.ViewHolder) recyclerViewLayout.getList().findViewHolderForLayoutPosition(0);
            if (holder != null && !CommonUtils.isExpanded(holder.details)) {
                isShowingHint = true;

                recyclerViewLayout.getList().scrollToPosition(0);

                Rect rect = new Rect();
                holder.itemView.getGlobalVisibleRect(rect);
                rect.offset((int) (holder.itemView.getWidth() * 0.3), (int) (-holder.itemView.getHeight() * 0.2));

                new TapTargetSequence(this)
                        .continueOnCancel(true)
                        .targets(TapTarget.forBounds(rect, getString(R.string.moreDetails), getString(R.string.moreDetails_desc)).tintTarget(false),
                                TapTarget.forView(holder.more, getString(R.string.evenMoreDetails), getString(R.string.evenMoreDetails_desc)))
                        .listener(new TapTargetSequence.Listener() {
                            @Override
                            public void onSequenceFinish() {
                                TutorialManager.setHintShown(MainActivity.this, TutorialManager.Discovery.CARD);
                                isShowingHint = false;
                            }

                            @Override
                            public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {

                            }

                            @Override
                            public void onSequenceCanceled(TapTarget lastTarget) {
                                onSequenceFinish();
                            }
                        }).start();
            }
        }
    }

    @Override
    public void onMenuItemSelected(final Download download, JTA2.DownloadActions action) {
        final JTA2 jta2;
        try {
            jta2 = JTA2.instantiate(this);
        } catch (JTA2.InitializingException ex) {
            onException(ex);
            return;
        }

        switch (action) {
            case MOVE_UP:
                jta2.moveUp(download.gid, this);
                break;
            case MOVE_DOWN:
                jta2.moveDown(download.gid, this);
                break;
            case PAUSE:
                jta2.pause(download.gid, this);
                break;
            case REMOVE:
                if (download.status == Download.Status.ACTIVE || download.status == Download.Status.PAUSED) {
                    CommonUtils.showDialog(this, new AlertDialog.Builder(this)
                            .setTitle(getString(R.string.removeName, download.getName()))
                            .setMessage(R.string.removeDownloadAlert)
                            .setNegativeButton(android.R.string.no, null)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    removeDownload(jta2, download);
                                }
                            }));
                } else {
                    removeDownload(jta2, download);
                }
                break;
            case RESTART:
                jta2.restart(download.gid, this);
                break;
            case RESUME:
                jta2.unpause(download.gid, this);
                break;
        }
    }

    private void removeDownload(final JTA2 jta2, final Download download) {
        if (download.following != null) {
            CommonUtils.showDialog(this, new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.removeMetadataName, download.getName()))
                    .setMessage(R.string.removeDownload_removeMetadata)
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            jta2.remove(download.gid, false, MainActivity.this);
                        }
                    })
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            jta2.remove(download.gid, true, MainActivity.this);
                        }
                    }));
        } else {
            jta2.remove(download.gid, false, this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (broadcastReceiver == null) {
            broadcastReceiver = new InternalBroadcastReceiver();
            DownloaderUtils.registerReceiver(this, broadcastReceiver, false);
        }

        DownloaderUtils.bindService(this, this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        DownloaderUtils.unbindService(this, this);
    }

    @Nullable
    @Override
    public RecyclerView getRecyclerView() {
        return recyclerViewLayout.getList();
    }

    @Override
    public void onPaused(String gid) {
        Toaster.show(this, Utils.Messages.PAUSED, gid);
    }

    @Override
    public void onRestarted(String gid) {
        Toaster.show(this, Utils.Messages.RESTARTED, gid);
    }

    @Override
    public void onUnpaused(String gid) {
        Toaster.show(this, Utils.Messages.RESUMED, gid);
    }

    @Override
    public void onMoved(String gid) {
        Toaster.show(this, Utils.Messages.MOVED, gid);
    }

    @Override
    public void onException(Exception ex) {
        Toaster.show(this, Utils.Messages.FAILED_PERFORMING_ACTION, ex);
    }

    @Override
    public void onRemoved(String gid) {
        Toaster.show(this, Utils.Messages.REMOVED, gid);
    }

    @Override
    public void onRemovedResult(String gid) {
        Toaster.show(this, Utils.Messages.RESULT_REMOVED, gid);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        adapter.filterWithQuery(query);
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
    @SuppressWarnings("ConstantConditions")
    public void connectivityChanged(@NonNull final MultiProfile.UserProfile profile) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (drawerManager != null)
                    drawerManager.setCurrentProfile(profile.getParent());
            }
        });

        List<MultiProfile.UserProfile> profiles = profile.getParent().profiles;
        if (!(profiles.size() == 1 && profiles.get(0).connectivityCondition.type == MultiProfile.ConnectivityCondition.Type.ALWAYS)) {
            Toaster.show(this, Utils.Messages.CONNECTIVITY_CHANGED, profile.connectivityCondition.toString());
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        downloaderMessenger = new Messenger(service);
        DownloaderUtils.refreshCount(downloaderMessenger);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        DownloaderUtils.unregisterReceiver(this, broadcastReceiver);
        broadcastReceiver = null;

        downloaderMessenger = null;
    }

    @Override
    @Nullable
    protected BaseUpdater createUpdater() {
        try {
            return new UpdateUI(this, this);
        } catch (JTA2.InitializingException ex) {
            ErrorHandler.get().notifyException(ex, true);
            if (recyclerViewLayout != null)
                recyclerViewLayout.showMessage(R.string.failedLoadingDownloads, true);
            return null;
        }
    }

    @Override
    public void refreshed() {
        adapter = new DownloadCardsAdapter(MainActivity.this, new ArrayList<Download>(), MainActivity.this);
        recyclerViewLayout.loadListData(adapter);
        recyclerViewLayout.startLoading();
        setupAdapterFiltersAndSorting();
    }

    private class InternalBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, final Intent intent) {
            if (intent.getAction() == null) return;

            switch (intent.getAction()) {
                case DownloaderUtils.ACTION_COUNT_CHANGED:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (drawerManager != null)
                                drawerManager.updateBadge(DrawerConst.DIRECT_DOWNLOAD, intent.getIntExtra("count", 0));
                        }
                    });
                    break;
            }
        }
    }
}