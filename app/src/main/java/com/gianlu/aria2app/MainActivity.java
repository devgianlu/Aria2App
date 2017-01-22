package com.gianlu.aria2app;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.gianlu.aria2app.Google.UncaughtExceptionHandler;
import com.gianlu.aria2app.Main.AddTorrentActivity;
import com.gianlu.aria2app.Main.AddURIActivity;
import com.gianlu.aria2app.Main.DrawerManager;
import com.gianlu.aria2app.Main.IThread;
import com.gianlu.aria2app.Main.LoadDownloads;
import com.gianlu.aria2app.Main.MainCardAdapter;
import com.gianlu.aria2app.Main.SearchActivity;
import com.gianlu.aria2app.Main.UpdateUI;
import com.gianlu.aria2app.NetIO.AsyncRequest;
import com.gianlu.aria2app.NetIO.IResponse;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.WebSocketing;
import com.gianlu.aria2app.Profile.AddProfileActivity;
import com.gianlu.aria2app.Profile.MultiModeProfileItem;
import com.gianlu.aria2app.Profile.ProfileItem;
import com.gianlu.aria2app.Profile.SingleModeProfileItem;
import com.gianlu.aria2app.Services.NotificationService;
import com.gianlu.commonutils.CommonUtils;
import com.google.android.gms.analytics.HitBuilders;
import com.liulishuo.filedownloader.FileDownloader;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements FloatingActionsMenu.OnFloatingActionsMenuUpdateListener, LoadDownloads.ILoading {
    private static boolean versionChecked = false;
    private RecyclerView mainRecyclerView;
    private DrawerManager drawerManager;
    private FloatingActionsMenu fabMenu;
    private UpdateUI updateUI;
    private SwipeRefreshLayout swipeLayout;
    private LoadDownloads loadDownloads;
    private MainCardAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name);

        CommonUtils.DEBUG = BuildConfig.DEBUG;
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (sharedPreferences.getString("dd_downloadPath", null) == null) {
            sharedPreferences.edit()
                    .putString("dd_downloadPath", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath())
                    .apply();
        }

        CommonUtils.logCleaner(this);
        Utils.renameOldProfiles(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        drawerManager = new DrawerManager(this, (DrawerLayout) findViewById(R.id.main_drawer));
        drawerManager.setToolbar(toolbar)
                .buildProfiles()
                .buildMenu()
                .setDrawerListener(new DrawerManager.IDrawerListener() {
                    @Override
                    public boolean onListItemSelected(DrawerManager.DrawerListItems which) {
                        Utils.IOptionsDialog handler = new Utils.IOptionsDialog() {
                            @Override
                            public void onApply(JTA2 jta2, Map<String, String> options) {
                                if (options.entrySet().size() == 0) return;

                                final ProgressDialog pd = CommonUtils.fastIndeterminateProgressDialog(MainActivity.this, R.string.gathering_information);
                                CommonUtils.showDialog(MainActivity.this, pd);

                                ThisApplication.sendAnalytics(MainActivity.this, new HitBuilders.EventBuilder()
                                        .setCategory(ThisApplication.CATEGORY_USER_INPUT)
                                        .setAction(ThisApplication.ACTION_CHANGED_GLOBAL_OPTIONS)
                                        .build());

                                jta2.changeGlobalOption(options, new JTA2.ISuccess() {
                                    @Override
                                    public void onSuccess() {
                                        pd.dismiss();
                                        CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.DOWNLOAD_OPTIONS_CHANGED);
                                    }

                                    @Override
                                    public void onException(Exception exception) {
                                        pd.dismiss();
                                        CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_CHANGE_OPTIONS, exception);
                                    }
                                });
                            }
                        };

                        switch (which) {
                            case HOME:
                                reloadPage();
                                return true;
                            case DIRECT_DOWNLOAD:
                                startActivity(new Intent(MainActivity.this, DirectDownloadActivity.class));
                                return false;
                            case TERMINAL:
                                startActivity(new Intent(MainActivity.this, TerminalActivity.class));
                                return false;
                            case QUICK_OPTIONS:
                                Utils.showOptionsDialog(MainActivity.this, null, true, true, handler);
                                return true;
                            case GLOBAL_OPTIONS:
                                Utils.showOptionsDialog(MainActivity.this, null, true, false, handler);
                                return true;
                            case PREFERENCES:
                                startActivity(new Intent(MainActivity.this, PreferencesActivity.class));
                                return false;
                            case SUPPORT:
                                CommonUtils.sendEmail(MainActivity.this, getString(R.string.app_name));
                                return true;
                            case ABOUT_ARIA2:
                                final JTA2 jta2;
                                try {
                                    jta2 = JTA2.newInstance(MainActivity.this);
                                } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyManagementException | KeyStoreException ex) {
                                    CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_GATHERING_INFORMATION, ex);
                                    return true;
                                }

                                final ProgressDialog pd = CommonUtils.fastIndeterminateProgressDialog(MainActivity.this, R.string.gathering_information);
                                CommonUtils.showDialog(MainActivity.this, pd);
                                jta2.getVersion(new JTA2.IVersion() {
                                    @Override
                                    public void onVersion(List<String> rawFeatures, String version) {
                                        final LinearLayout box = new LinearLayout(MainActivity.this);
                                        box.setOrientation(LinearLayout.VERTICAL);
                                        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
                                        box.setPadding(padding, padding, padding, padding);

                                        //noinspection deprecation
                                        box.addView(CommonUtils.fastTextView(MainActivity.this, Html.fromHtml(getString(R.string.version, version))));

                                        String extendedList = "";
                                        boolean first = true;
                                        for (String _feature : rawFeatures) {
                                            if (!first)
                                                extendedList += ", ";
                                            else
                                                first = false;

                                            extendedList += _feature;
                                        }

                                        //noinspection deprecation
                                        box.addView(CommonUtils.fastTextView(MainActivity.this, Html.fromHtml(getString(R.string.features, extendedList))));

                                        jta2.getSessionInfo(new JTA2.ISession() {
                                            @Override
                                            public void onSessionInfo(String sessionID) {
                                                //noinspection deprecation
                                                box.addView(CommonUtils.fastTextView(MainActivity.this, Html.fromHtml(getString(R.string.sessionId, sessionID))));

                                                pd.dismiss();
                                                CommonUtils.showDialog(MainActivity.this, new AlertDialog.Builder(MainActivity.this).setTitle(R.string.about_aria2)
                                                        .setView(box)
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
                                                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                            }
                                                        }));
                                            }

                                            @Override
                                            public void onException(Exception exception) {
                                                CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_GATHERING_INFORMATION, exception);
                                                pd.dismiss();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onException(Exception exception) {
                                        CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_GATHERING_INFORMATION, exception);
                                        pd.dismiss();
                                    }
                                });
                                return true;
                            default:
                                return true;
                        }
                    }

                    @Override
                    public void onProfileItemSelected(final SingleModeProfileItem profile, boolean fromRecent) {
                        if (!fromRecent && profile.status != ProfileItem.STATUS.ONLINE) {
                            CommonUtils.showDialog(MainActivity.this, new AlertDialog.Builder(MainActivity.this).setMessage(R.string.serverOffline)
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            startWithProfile(profile, true);
                                            drawerManager.setDrawerState(false, true);
                                        }
                                    })
                                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            drawerManager.setDrawerState(true, true);
                                        }
                                    }));
                        } else {
                            drawerManager.setDrawerState(false, true);
                            startWithProfile(profile, true);
                        }
                    }

                    @Override
                    public void onAddProfile() {
                        startActivity(new Intent(MainActivity.this, AddProfileActivity.class)
                                .putExtra("edit", false));
                    }
                });

        mainRecyclerView = (RecyclerView) findViewById(R.id.main_recyclerView);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mainRecyclerView.setLayoutManager(llm);

        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.main_swipeLayout);
        swipeLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorMetalink, R.color.colorTorrent);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                reloadPage();
            }
        });

        UpdateUI.stop(updateUI);

        if (getIntent().getBooleanExtra("external", false)) {
            setTitle(getString(R.string.app_name) + " - Local device");

            saveExternalProfile(SingleModeProfileItem.externalDefault(getIntent().getIntExtra("port", 6800), getIntent().getStringExtra("token")));
        }

        try {
            SingleModeProfileItem profile;
            if (getIntent().getBooleanExtra("external", false)) {
                getIntent().removeExtra("external");

                profile = SingleModeProfileItem.fromName(this, SingleModeProfileItem.EXTERNAL_DEFAULT_FILE_NAME);
            } else if (getIntent().getBooleanExtra("fromNotification", false)) {
                getIntent().removeExtra("fromNotification");

                if (ProfileItem.isSingleMode(this, getIntent().getStringExtra("fileName")))
                    profile = SingleModeProfileItem.fromName(this, getIntent().getStringExtra("fileName"));
                else
                    profile = MultiModeProfileItem.fromName(this, getIntent().getStringExtra("fileName")).getCurrentProfile(this);
            } else if (!drawerManager.hasProfiles()) {
                profile = null;
                startActivity(new Intent(MainActivity.this, AddProfileActivity.class)
                        .putExtra("canGoBack", false)
                        .putExtra("edit", false));
            } else {
                profile = CurrentProfile.getCurrentProfile(this);
            }

            if (profile == null) {
                drawerManager.openProfiles(true);
                return;
            }

            setTitle(getString(R.string.app_name) + " - " + profile.globalProfileName);

            startWithProfile(profile, false);
        } catch (IOException | JSONException ex) {
            CommonUtils.UIToast(this, Utils.ToastMessages.FATAL_EXCEPTION, ex);
            return;
        }

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
                startActivity(new Intent(MainActivity.this, AddURIActivity.class));
            }
        });
        final FloatingActionButton fabAddTorrent = (FloatingActionButton) findViewById(R.id.mainFab_addTorrent);
        fabAddTorrent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AddTorrentActivity.class).putExtra("torrentMode", true));
            }
        });
        final FloatingActionButton fabAddMetalink = (FloatingActionButton) findViewById(R.id.mainFab_addMetalink);
        fabAddMetalink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AddTorrentActivity.class).putExtra("torrentMode", false));
            }
        });

        if (getIntent().getBooleanExtra("backFromAddProfile", false)) {
            drawerManager.buildProfiles().openProfiles(false);
        }

        WebSocketing.setGlobalHandler(new WebSocketing.IListener() {
            @Override
            public void onException(Throwable ex) {
                CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.WS_DISCONNECTED, ex, new Runnable() {
                    @Override
                    public void run() {
                        swipeLayout.setRefreshing(false);
                        drawerManager.updateBadge(DrawerManager.DrawerListItems.HOME, -1);
                    }
                });

                CommonUtils.showDialog(MainActivity.this, new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.noCommunication)
                        .setCancelable(false)
                        .setMessage(getString(R.string.noCommunication_message, ex.getMessage()))
                        .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                recreate();
                            }
                        })
                        .setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        })
                        .setNeutralButton(R.string.changeProfile, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                drawerManager.openProfiles(true);
                            }
                        }));
            }

            @Override
            public void onDisconnected() {
                UpdateUI.stop(updateUI);

                CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.WS_DISCONNECTED, new Runnable() {
                    @Override
                    public void run() {
                        swipeLayout.setRefreshing(false);
                        drawerManager.openProfiles(true);
                        drawerManager.updateBadge(DrawerManager.DrawerListItems.HOME, -1);
                    }
                });
            }
        });

        int autoReloadDownloadsListRate = Integer.parseInt(sharedPreferences.getString("a2_downloadListRate", "0")) * 1000;
        if (autoReloadDownloadsListRate != 0) {
            Timer reloadDownloadsListTimer = new Timer(false);
            reloadDownloadsListTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    UpdateUI.stop(updateUI, new IThread() {
                        @Override
                        public void stopped() {
                            loadDownloads = new LoadDownloads(MainActivity.this, MainActivity.this);
                            try {
                                new Thread(loadDownloads).start();
                            } catch (InternalError ignored) {
                            }
                        }
                    });
                }
            }, 0, autoReloadDownloadsListRate);
        } else {
            loadDownloads = new LoadDownloads(this, this);
            new Thread(loadDownloads).start();
        }

        try {
            WebSocketing.enableEventManager(this);
        } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyManagementException | KeyStoreException ex) {
            CommonUtils.logMe(this, ex);
        }

        if (sharedPreferences.getBoolean("a2_enableNotifications", true)) {
            startService(NotificationService.createStartIntent(this));
        } else {
            stopService(new Intent(this, NotificationService.class).setAction("STOP"));
        }

        long firstStart = sharedPreferences.getLong("firstStart", -1);
        if (firstStart == -1) {
            sharedPreferences.edit()
                    .putLong("firstStart", System.currentTimeMillis())
                    .apply();
        } else if (firstStart > 0) {
            if (System.currentTimeMillis() - sharedPreferences.getLong("firstStart", -1) >= 259200000) {
                CommonUtils.showDialog(this, new AlertDialog.Builder(this)
                        .setTitle(R.string.voteApp)
                        .setMessage(R.string.voteApp_message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                try {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getApplicationContext().getPackageName())));
                                } catch (android.content.ActivityNotFoundException ex) {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getApplicationContext().getPackageName())));
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.no, null));

                sharedPreferences.edit()
                        .putLong("firstStart", -2)
                        .apply();
            }
        }

        FileDownloader.getImpl().bindService(new Runnable() {
            @Override
            public void run() {
                FileDownloader.getImpl().setMaxNetworkThreadCount(3);
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (drawerManager != null)
            drawerManager.onTogglerConfigurationChanged(newConfig);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (drawerManager != null)
            drawerManager.syncTogglerState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (drawerManager != null)
            drawerManager.syncTogglerState();
    }

    private void saveExternalProfile(@NonNull SingleModeProfileItem profile) {
        try {
            deleteFile(SingleModeProfileItem.EXTERNAL_DEFAULT_FILE_NAME);

            FileOutputStream fOut = openFileOutput(SingleModeProfileItem.EXTERNAL_DEFAULT_FILE_NAME, Context.MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fOut);

            osw.write(profile.toJSON().toString());
            osw.flush();
            osw.close();
        } catch (IOException | JSONException ex) {
            CommonUtils.UIToast(this, Utils.ToastMessages.FATAL_EXCEPTION, ex);
        }
    }

    private void startWithProfile(@NonNull SingleModeProfileItem profile, boolean recreate) {
        drawerManager.setCurrentProfile(profile);

        CurrentProfile.setCurrentProfile(this, profile);

        if (recreate) {
            WebSocketing.destroyInstance();
            recreate();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        getMenuInflater().inflate(R.menu.main_filters, menu.findItem(R.id.a2menu_filtering).getSubMenu());
        return true;
    }

    public void reloadPage() {
        UpdateUI.stop(updateUI, new IThread() {
            @Override
            public void stopped() {
                loadDownloads = new LoadDownloads(MainActivity.this, MainActivity.this);
                new Thread(loadDownloads).start();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (fabMenu == null) {
            super.onBackPressed();
            return;
        }

        if (fabMenu.isExpanded()) {
            fabMenu.collapse();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.a2menu_refreshPage:
                reloadPage();
                break;
            // Filters
            case R.id.a2menu_active:
                if (adapter == null)
                    break;

                item.setChecked(!item.isChecked());

                if (item.isChecked())
                    adapter.removeFilter(Download.STATUS.ACTIVE);
                else
                    adapter.addFilter(Download.STATUS.ACTIVE);
                break;
            case R.id.a2menu_paused:
                if (adapter == null)
                    break;

                item.setChecked(!item.isChecked());

                if (item.isChecked())
                    adapter.removeFilter(Download.STATUS.PAUSED);
                else
                    adapter.addFilter(Download.STATUS.PAUSED);
                break;
            case R.id.a2menu_error:
                if (adapter == null)
                    break;

                item.setChecked(!item.isChecked());

                if (item.isChecked())
                    adapter.removeFilter(Download.STATUS.ERROR);
                else
                    adapter.addFilter(Download.STATUS.ERROR);
                break;
            case R.id.a2menu_waiting:
                if (adapter == null)
                    break;

                item.setChecked(!item.isChecked());

                if (item.isChecked())
                    adapter.removeFilter(Download.STATUS.WAITING);
                else
                    adapter.addFilter(Download.STATUS.WAITING);
                break;
            case R.id.a2menu_complete:
                if (adapter == null)
                    break;

                item.setChecked(!item.isChecked());

                if (item.isChecked())
                    adapter.removeFilter(Download.STATUS.COMPLETE);
                else
                    adapter.addFilter(Download.STATUS.COMPLETE);
                break;
            case R.id.a2menu_removed:
                if (adapter == null)
                    break;

                item.setChecked(!item.isChecked());

                if (item.isChecked())
                    adapter.removeFilter(Download.STATUS.REMOVED);
                else
                    adapter.addFilter(Download.STATUS.REMOVED);
                break;
        }

        return super.onOptionsItemSelected(item);
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
    public void onStarted() {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                swipeLayout.setRefreshing(true);
            }
        });
    }

    @Override
    public void onLoaded(JTA2 jta2, final List<Download> downloads) {
        adapter = new MainCardAdapter(MainActivity.this, downloads, new MainCardAdapter.IActions() {
            @Override
            public void onMoreClick(Download item) {
                Intent launchActivity = new Intent(MainActivity.this, MoreAboutDownloadActivity.class)
                        .putExtra("gid", item.gid)
                        .putExtra("name", item.getName())
                        .putExtra("isTorrent", item.isBitTorrent)
                        .putExtra("status", item.status.name());
                if (!item.status.equals(Download.STATUS.UNKNOWN) && !item.status.equals(Download.STATUS.ERROR))
                    MainActivity.this.startActivity(launchActivity);
            }

            @Override
            public void onItemCountUpdated(final int count) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (count > 0) {
                            findViewById(R.id.main_noItems).setVisibility(View.GONE);
                            mainRecyclerView.setVisibility(View.VISIBLE);
                        } else {
                            findViewById(R.id.main_noItems).setVisibility(View.VISIBLE);
                            mainRecyclerView.setVisibility(View.GONE);
                        }

                        if (drawerManager != null)
                            drawerManager.updateBadge(DrawerManager.DrawerListItems.HOME, count);
                    }
                });
            }

            @Override
            public void onMenuItemSelected(Download item, DownloadAction.ACTION action) {
                DownloadAction downloadAction;
                try {
                    downloadAction = new DownloadAction(MainActivity.this);
                } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyManagementException | KeyStoreException ex) {
                    CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.WS_EXCEPTION, ex);
                    return;
                }

                DownloadAction.IMove iMove = new DownloadAction.IMove() {
                    @Override
                    public void onMoved(String gid) {
                        CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.MOVED, gid);
                    }

                    @Override
                    public void onException(Exception ex) {
                        CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_CHANGE_POSITION, ex);
                    }
                };

                switch (action) {
                    case PAUSE:
                        downloadAction.pause(MainActivity.this, item.gid, new DownloadAction.IPause() {
                            @Override
                            public void onPaused(String gid) {
                                CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.PAUSED, gid);
                            }

                            @Override
                            public void onException(Exception ex) {
                                CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_PAUSE, ex);
                            }
                        });
                        break;
                    case REMOVE:
                        downloadAction.remove(MainActivity.this, item.gid, item.status, new DownloadAction.IRemove() {
                            @Override
                            public void onRemoved(String gid) {
                                CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.REMOVED, gid);
                            }

                            @Override
                            public void onRemovedResult(final String gid) {
                                CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.REMOVED_RESULT, gid, new Runnable() {
                                    @Override
                                    public void run() {
                                        adapter.removeItem(gid);
                                    }
                                });
                            }

                            @Override
                            public void onException(boolean b, Exception ex) {
                                if (b)
                                    CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_REMOVE, ex);
                                else
                                    CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_REMOVE_RESULT, ex);
                            }
                        });
                        break;
                    case RESTART:
                        downloadAction.restart(item.gid, new DownloadAction.IRestart() {
                            @Override
                            public void onRestarted() {
                                CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.RESTARTED);
                            }

                            @Override
                            public void onException(Exception ex) {
                                CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_ADD_DOWNLOAD, ex);
                            }

                            @Override
                            public void onRemoveResultException(Exception ex) {
                                CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_REMOVE_RESULT, ex);
                            }

                            @Override
                            public void onGatheringInformationException(Exception ex) {
                                CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_GATHERING_INFORMATION, ex);
                            }
                        });
                        break;
                    case RESUME:
                        downloadAction.unpause(item.gid, new DownloadAction.IUnpause() {
                            @Override
                            public void onUnpaused(String gid) {
                                CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.RESUMED, gid);
                            }

                            @Override
                            public void onException(Exception ex) {
                                CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_UNPAUSE, ex);
                            }
                        });
                        break;
                    case MOVE_DOWN:
                        downloadAction.moveDown(item.gid, iMove);
                        break;
                    case MOVE_UP:
                        downloadAction.moveUp(item.gid, iMove);
                        break;
                }
            }
        });

        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainRecyclerView.setAdapter(adapter);
                drawerManager.updateBadge(DrawerManager.DrawerListItems.HOME, downloads.size());

                updateUI = new UpdateUI(MainActivity.this, (MainCardAdapter) mainRecyclerView.getAdapter());
                new Thread(updateUI).start();

                swipeLayout.setRefreshing(false);

                if (getIntent().getBooleanExtra("fromNotification", false)) {
                    Download item = ((MainCardAdapter) mainRecyclerView.getAdapter()).getItem(getIntent().getStringExtra("gid"));
                    if (item == null || item.status == Download.STATUS.UNKNOWN) return;

                    startActivity(new Intent(MainActivity.this, MoreAboutDownloadActivity.class)
                            .putExtra("gid", item.gid)
                            .putExtra("isTorrent", item.isBitTorrent)
                            .putExtra("status", item.status.name())
                            .putExtra("name", item.getName()));
                } else if (getIntent().getBooleanExtra("fromDirectDownload", false)) {
                    Download item = ((MainCardAdapter) mainRecyclerView.getAdapter()).getItem(getIntent().getStringExtra("gid"));
                    if (item == null || item.status == Download.STATUS.UNKNOWN) return;

                    startActivity(new Intent(MainActivity.this, MoreAboutDownloadActivity.class)
                            .putExtra("fileIndex", getIntent().getIntExtra("index", -1))
                            .putExtra("gid", item.gid)
                            .putExtra("isTorrent", item.isBitTorrent)
                            .putExtra("status", item.status.name())
                            .putExtra("name", item.getName()));
                }
            }
        });

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("a2_runVersionCheckAtStartup", true) && !versionChecked) {
            jta2.getVersion(new JTA2.IVersion() {
                @Override
                public void onVersion(List<String> rawFeatures, final String version) {
                    new Thread(new AsyncRequest(getString(R.string.versionCheckURL), new IResponse() {
                        @Override
                        public void onResponse(String response) {
                            String latest;
                            try {
                                latest = new JSONArray(response).getJSONObject(0).getString("name");
                                latest = latest.replace("aria2 ", "");
                            } catch (JSONException ex) {
                                CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_CHECKING_VERSION, ex);
                                return;
                            }

                            if (!Objects.equals(latest, version)) {
                                CommonUtils.showDialog(MainActivity.this, new AlertDialog.Builder(MainActivity.this)
                                        .setTitle(R.string.dialogVersionCheck)
                                        .setMessage(getString(R.string.dialogVersionCheckMessage, latest, version))
                                        .setPositiveButton(android.R.string.ok, null));
                            }

                            versionChecked = true;
                        }

                        @Override
                        public void onException(Exception exception) {
                            versionChecked = false;
                            CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_CHECKING_VERSION, exception);
                        }
                    })).start();
                }

                @Override
                public void onException(Exception exception) {
                    CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_CHECKING_VERSION, exception);
                }
            });
        }
    }

    @Override
    public void onException(boolean queuing, final Exception ex) {
        if (queuing) {
            WebSocketing.notifyConnection(new WebSocketing.IConnecting() {
                @Override
                public void onDone() {
                    loadDownloads = new LoadDownloads(MainActivity.this, MainActivity.this);
                    new Thread(loadDownloads).start();
                }
            });
            return;
        }

        CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_GATHERING_INFORMATION, ex, new Runnable() {
            @Override
            public void run() {
                swipeLayout.setRefreshing(false);
                drawerManager.updateBadge(DrawerManager.DrawerListItems.HOME, -1);
            }
        });
    }
}