package com.gianlu.aria2app;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.gianlu.aria2app.Activities.DirectDownloadActivity;
import com.gianlu.aria2app.Activities.EditProfileActivity;
import com.gianlu.aria2app.Activities.MoreAboutDownloadActivity;
import com.gianlu.aria2app.Activities.SearchActivity;
import com.gianlu.aria2app.Adapters.DownloadCardsAdapter;
import com.gianlu.aria2app.Main.DrawerConst;
import com.gianlu.aria2app.Main.UpdateUI;
import com.gianlu.aria2app.NetIO.BaseUpdater;
import com.gianlu.aria2app.NetIO.ErrorHandler;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.JTA2.JTA2InitializingException;
import com.gianlu.aria2app.Options.OptionsUtils;
import com.gianlu.aria2app.ProfilesManager.CustomProfilesAdapter;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.ProfilesManager.UserProfile;
import com.gianlu.aria2app.Services.NotificationService;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Drawer.BaseDrawerItem;
import com.gianlu.commonutils.Drawer.DrawerManager;
import com.gianlu.commonutils.Drawer.Initializer;
import com.gianlu.commonutils.Drawer.ProfilesAdapter;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.MessageLayout;
import com.gianlu.commonutils.SuperTextView;
import com.liulishuo.filedownloader.FileDownloader;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements FloatingActionsMenu.OnFloatingActionsMenuUpdateListener, JTA2.IUnpause, JTA2.IRemove, JTA2.IPause, DrawerManager.IDrawerListener<UserProfile>, DrawerManager.ISetup<UserProfile>, UpdateUI.IUI, DownloadCardsAdapter.IAdapter, JTA2.IRestart {
    private DrawerManager<UserProfile> drawerManager;
    private FloatingActionsMenu fabMenu;
    private SwipeRefreshLayout swipeRefresh;
    private Menu sortingSubMenu;
    private RecyclerView list;
    private DownloadCardsAdapter adapter;
    private UpdateUI updater;

    private void refresh() {
        updater.stopThread(new BaseUpdater.IThread() {
            @Override
            public void onStopped() {
                adapter = new DownloadCardsAdapter(MainActivity.this, new ArrayList<Download>(), MainActivity.this);
                list.setAdapter(adapter);

                try {
                    updater = new UpdateUI(MainActivity.this, MainActivity.this);
                    updater.start();
                } catch (JTA2InitializingException ex) {
                    ErrorHandler.get().notifyException(ex, true);
                }

                swipeRefresh.setRefreshing(false);
            }
        });
    }

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
                refresh();
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
                CommonUtils.sendEmail(MainActivity.this, getString(R.string.app_name));
                return true;
            case DrawerConst.ABOUT_ARIA2:
                showAboutDialog();
                return true;
            default:
                return true;
        }
    }

    @Override
    public void onProfileSelected(final UserProfile profile) {
        ProfilesManager.get(this).setLastProfile(this, profile);
        startActivity(new Intent(this, LoadingActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    @Override
    public void addProfile() {
        EditProfileActivity.start(this, false);
    }

    @Override
    public void editProfile(final List<UserProfile> items) {
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
        } catch (JTA2InitializingException ex) {
            CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_GATHERING_INFORMATION, ex);
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
                layout.addView(new SuperTextView(MainActivity.this, R.string.features, CommonUtils.join(rawFeatures, ",")));

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
                                                CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.SESSION_SAVED);
                                            }

                                            @Override
                                            public void onException(Exception exception) {
                                                CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_SAVE_SESSION, exception);
                                            }
                                        });
                                    }
                                })
                                .setPositiveButton(android.R.string.ok, null));
                    }

                    @Override
                    public void onException(Exception ex) {
                        CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_GATHERING_INFORMATION, ex);
                        pd.dismiss();
                    }
                });
            }

            @Override
            public void onException(Exception ex) {
                CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_GATHERING_INFORMATION, ex);
                pd.dismiss();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name);

        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(this));

        if (Prefs.getString(this, Prefs.Keys.DD_DOWNLOAD_PATH, null) == null)
            Prefs.editString(this, Prefs.Keys.DD_DOWNLOAD_PATH, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());

        Logging.clearLogs(this);
        Utils.renameOldProfiles(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        drawerManager = new DrawerManager<>(new Initializer<>(this, (DrawerLayout) findViewById(R.id.main_drawer), toolbar, this).addMenuItem(new BaseDrawerItem(DrawerConst.HOME, R.drawable.ic_home_black_48dp, getString(R.string.home)))
                .addMenuItem(new BaseDrawerItem(DrawerConst.DIRECT_DOWNLOAD, R.drawable.ic_cloud_download_black_48dp, getString(R.string.directDownload)))
                .addMenuItem(new BaseDrawerItem(DrawerConst.QUICK_OPTIONS, R.drawable.ic_favorite_black_48dp, getString(R.string.quickGlobalOptions)))
                .addMenuItem(new BaseDrawerItem(DrawerConst.GLOBAL_OPTIONS, R.drawable.ic_list_black_48dp, getString(R.string.globalOptions)))
                .addMenuItem(new BaseDrawerItem(DrawerConst.ABOUT_ARIA2, R.drawable.ic_cloud_black_48dp, getString(R.string.about_aria2)))
                .addMenuItemSeparator()
                .addMenuItem(new BaseDrawerItem(DrawerConst.PREFERENCES, R.drawable.ic_settings_black_48dp, getString(R.string.menu_preferences)))
                .addMenuItem(new BaseDrawerItem(DrawerConst.SUPPORT, R.drawable.ic_report_problem_black_48dp, getString(R.string.support)))
                .addProfiles(ProfilesManager.get(this).getProfiles()));

        ProfilesManager manager = ProfilesManager.get(this);
        UserProfile currentProfile = manager.getCurrentAssert();

        drawerManager.setCurrentProfile(currentProfile).setDrawerListener(this);

        setTitle(currentProfile.getProfileName() + " - " + getString(R.string.app_name));

        list = (RecyclerView) findViewById(R.id.main_list);
        list.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.main_swipeLayout);
        swipeRefresh.setColorSchemeResources(R.color.colorAccent, R.color.colorMetalink, R.color.colorTorrent);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

        fabMenu = (FloatingActionsMenu) findViewById(R.id.main_fab);
        fabMenu.setOnFloatingActionsMenuUpdateListener(this);

        FloatingActionButton fabSearch = (FloatingActionButton) findViewById(R.id.mainFab_search);
        fabSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SearchActivity.class));
            }
        });
        FloatingActionButton fabAddURI = (FloatingActionButton) findViewById(R.id.mainFab_addURI);
        fabAddURI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: startActivity(new Intent(MainActivity.this, AddURIActivity.class));
            }
        });
        final FloatingActionButton fabAddTorrent = (FloatingActionButton) findViewById(R.id.mainFab_addTorrent);
        fabAddTorrent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: startActivity(new Intent(MainActivity.this, AddTorrentActivity.class).putExtra("torrentMode", true));
            }
        });
        final FloatingActionButton fabAddMetalink = (FloatingActionButton) findViewById(R.id.mainFab_addMetalink);
        fabAddMetalink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: startActivity(new Intent(MainActivity.this, AddTorrentActivity.class).putExtra("torrentMode", false));
            }
        });

        if (Prefs.getBoolean(this, Prefs.Keys.A2_ENABLE_NOTIFS, true))
            NotificationService.start(this);
        else NotificationService.stop(this);

        FileDownloader.getImpl().bindService(new Runnable() {
            @Override
            public void run() {
                FileDownloader.getImpl().setMaxNetworkThreadCount(3);
            }
        });

        adapter = new DownloadCardsAdapter(this, new ArrayList<Download>(), this);
        list.setAdapter(adapter);

        try {
            updater = new UpdateUI(this, this);
            updater.start();
        } catch (JTA2InitializingException ex) {
            ErrorHandler.get().notifyException(ex, true);
        }
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        getMenuInflater().inflate(R.menu.main_filters, menu.findItem(R.id.a2menu_filtering).getSubMenu());
        getMenuInflater().inflate(R.menu.main_sorting, menu.findItem(R.id.a2menu_sorting).getSubMenu());
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        sortingSubMenu = menu.findItem(R.id.a2menu_sorting).getSubMenu();
        sortingSubMenu.setGroupCheckable(0, true, true);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        if (fabMenu != null && fabMenu.isExpanded()) fabMenu.collapse();
        else super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.a2menu_refreshPage:
                refresh();
                break;
            // Filters
            case R.id.a2menu_active:
                if (adapter == null) break;
                item.setChecked(!item.isChecked());
                if (item.isChecked()) adapter.removeFilter(Download.Status.ACTIVE);
                else adapter.addFilter(Download.Status.ACTIVE);
                break;
            case R.id.a2menu_paused:
                if (adapter == null) break;
                item.setChecked(!item.isChecked());
                if (item.isChecked()) adapter.removeFilter(Download.Status.PAUSED);
                else adapter.addFilter(Download.Status.PAUSED);
                break;
            case R.id.a2menu_error:
                if (adapter == null) break;
                item.setChecked(!item.isChecked());
                if (item.isChecked()) adapter.removeFilter(Download.Status.ERROR);
                else adapter.addFilter(Download.Status.ERROR);
                break;
            case R.id.a2menu_waiting:
                if (adapter == null) break;
                item.setChecked(!item.isChecked());
                if (item.isChecked()) adapter.removeFilter(Download.Status.WAITING);
                else adapter.addFilter(Download.Status.WAITING);
                break;
            case R.id.a2menu_complete:
                if (adapter == null) break;
                item.setChecked(!item.isChecked());
                if (item.isChecked()) adapter.removeFilter(Download.Status.COMPLETE);
                else adapter.addFilter(Download.Status.COMPLETE);
                break;
            case R.id.a2menu_removed:
                if (adapter == null) break;
                item.setChecked(!item.isChecked());
                if (item.isChecked()) adapter.removeFilter(Download.Status.REMOVED);
                else adapter.addFilter(Download.Status.REMOVED);
                break;
            // Sorting
            default:
                handleSorting(item);
        }

        return super.onOptionsItemSelected(item);
    }

    private void handleSorting(MenuItem clicked) {
        if (sortingSubMenu == null || adapter == null)
            return;

        clicked.setChecked(true);
        switch (clicked.getItemId()) {
            case R.id.a2menu_sortStatus:
                adapter.sortBy(DownloadCardsAdapter.SortBy.STATUS);
                break;
            case R.id.a2menu_sortProgress:
                adapter.sortBy(DownloadCardsAdapter.SortBy.PROGRESS);
                break;
            case R.id.a2menu_sortDownloadSpeed:
                adapter.sortBy(DownloadCardsAdapter.SortBy.DOWNLOAD_SPEED);
                break;
            case R.id.a2menu_sortUploadSpeed:
                adapter.sortBy(DownloadCardsAdapter.SortBy.UPLOAD_SPEED);
                break;
            case R.id.a2menu_sortLength:
                adapter.sortBy(DownloadCardsAdapter.SortBy.LENGTH);
                break;
            case R.id.a2menu_sortCompletedLength:
                adapter.sortBy(DownloadCardsAdapter.SortBy.COMPLETED_LENGTH);
                break;
        }
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
    public int getOpenDrawerDesc() {
        return R.string.openDrawer;
    }

    @Override
    public int getCloseDrawerDesc() {
        return R.string.closeDrawer;
    }

    @Override
    public int getRippleDark() {
        return R.drawable.ripple_effect_dark;
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
    public ProfilesAdapter<UserProfile> getProfilesAdapter(Context context, List<UserProfile> profiles, final DrawerManager.IDrawerListener<UserProfile> listener) {
        return new CustomProfilesAdapter(context, profiles, new ProfilesAdapter.IAdapter<UserProfile>() {
            @Override
            public void onProfileSelected(UserProfile profile) {
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
    public void onUpdateAdapter(Download download) {
        if (adapter != null) adapter.notifyItemChanged(download);
    }

    @Override
    public void onMoreClick(Download item) {
        MoreAboutDownloadActivity.start(this, item);
    }

    @Override
    public void onItemCountUpdated(int count) {
        if (drawerManager != null) drawerManager.updateBadge(DrawerConst.HOME, count);

        if (count == 0) {
            MessageLayout.show((ViewGroup) findViewById(R.id.main_drawer), R.string.noDownloads, R.drawable.ic_info_outline_black_48dp);
            list.setVisibility(View.GONE);
        } else {
            MessageLayout.hide((ViewGroup) findViewById(R.id.main_drawer));
            list.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onMenuItemSelected(final Download download, JTA2.DownloadActions action) {
        final JTA2 jta2;
        try {
            jta2 = JTA2.instantiate(this);
        } catch (JTA2InitializingException ex) {
            onException(ex);
            return;
        }

        switch (action) {
            case MOVE_UP:
                break;
            case MOVE_DOWN:
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
                                    jta2.remove(download.gid, download.status, MainActivity.this);
                                }
                            }));
                } else {
                    jta2.remove(download.gid, download.status, MainActivity.this);
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

    @Override
    public void onPaused(String gid) {
        CommonUtils.UIToast(this, Utils.ToastMessages.PAUSED, gid);
    }

    @Override
    public void onRestarted(String gid) {
        CommonUtils.UIToast(this, Utils.ToastMessages.RESTARTED, gid);
    }

    @Override
    public void onUnpaused(String gid) {
        CommonUtils.UIToast(this, Utils.ToastMessages.RESUMED, gid);
    }

    @Override
    public void onException(Exception ex) {
        CommonUtils.UIToast(this, Utils.ToastMessages.FAILED_PERFORMING_ACTION, ex);
    }

    @Override
    public void onRemoved(String gid) {
        CommonUtils.UIToast(this, Utils.ToastMessages.REMOVED, gid);
    }

    @Override
    public void onRemovedResult(String gid) {
        CommonUtils.UIToast(this, Utils.ToastMessages.RESULT_REMOVED, gid);
    }
}