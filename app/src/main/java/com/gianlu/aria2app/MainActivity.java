package com.gianlu.aria2app;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
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
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.gianlu.aria2app.Google.Analytics;
import com.gianlu.aria2app.Google.UncaughtExceptionHandler;
import com.gianlu.aria2app.Main.AddTorrentActivity;
import com.gianlu.aria2app.Main.AddURIActivity;
import com.gianlu.aria2app.Main.DrawerManager;
import com.gianlu.aria2app.Main.IThread;
import com.gianlu.aria2app.Main.LoadDownloads;
import com.gianlu.aria2app.Main.MainCardAdapter;
import com.gianlu.aria2app.Main.Profile.AddProfileActivity;
import com.gianlu.aria2app.Main.Profile.MultiModeProfileItem;
import com.gianlu.aria2app.Main.Profile.ProfileItem;
import com.gianlu.aria2app.Main.Profile.SingleModeProfileItem;
import com.gianlu.aria2app.Main.UpdateUI;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.IOption;
import com.gianlu.aria2app.NetIO.JTA2.ISession;
import com.gianlu.aria2app.NetIO.JTA2.ISuccess;
import com.gianlu.aria2app.NetIO.JTA2.IVersion;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.WebSocketing;
import com.gianlu.aria2app.Options.LocalParser;
import com.gianlu.aria2app.Options.OptionAdapter;
import com.gianlu.aria2app.Options.OptionChild;
import com.gianlu.aria2app.Options.OptionHeader;
import com.gianlu.aria2app.Options.Parser;
import com.gianlu.aria2app.Services.NotificationWebSocketService;
import com.google.android.gms.analytics.HitBuilders;

import org.json.JSONException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private RecyclerView mainRecyclerView;
    private DrawerManager drawerManager;
    private FloatingActionsMenu fabMenu;
    private LoadDownloads.ILoading loadingHandler;
    private UpdateUI updateUI;
    private LoadDownloads loadDownloads;
    private MainCardAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name);

        UncaughtExceptionHandler.application = getApplication();
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(this));

        Utils.logCleaner(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        assert toolbar != null;
        setSupportActionBar(toolbar);

        drawerManager = new DrawerManager(this, (DrawerLayout) findViewById(R.id.main_drawer));
        drawerManager.setToolbar(toolbar)
                .buildProfiles()
                .buildMenu()
                .setDrawerListener(new DrawerManager.IDrawerListener() {
                    @Override
                    public boolean onListItemSelected(DrawerManager.DrawerListItems which) {
                        switch (which) {
                            case HOME:
                                reloadPage();
                                return true;
                            case TERMINAL:
                                startActivity(new Intent(MainActivity.this, TerminalActivity.class));
                                return false;
                            case GLOBAL_OPTIONS:
                                showOptionsDialog();
                                return true;
                            case PREFERENCES:
                                startActivity(new Intent(MainActivity.this, MainSettingsActivity.class));
                                return false;
                            case SUPPORT:
                                return true;
                            case ABOUT_ARIA2:
                                final JTA2 jta2;
                                try {
                                    jta2 = JTA2.newInstance(MainActivity.this);
                                } catch (IOException | NoSuchAlgorithmException ex) {
                                    Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex);
                                    return true;
                                }

                                final ProgressDialog pd = Utils.fastProgressDialog(MainActivity.this, R.string.gathering_information, true, false);
                                pd.show();
                                jta2.getVersion(new IVersion() {
                                    @Override
                                    public void onVersion(List<String> rawFeatures, List<JTA2.FEATURES> enabledFeatures, String version) {
                                        final LinearLayout box = new LinearLayout(MainActivity.this);
                                        box.setOrientation(LinearLayout.VERTICAL);
                                        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
                                        box.setPadding(padding, padding, padding, padding);


                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                            box.addView(Utils.fastTextView(MainActivity.this, Html.fromHtml(getString(R.string.version, version), Html.FROM_HTML_MODE_COMPACT)));
                                        } else {
                                            //noinspection deprecation
                                            box.addView(Utils.fastTextView(MainActivity.this, Html.fromHtml(getString(R.string.version, version))));
                                        }

                                        String extendedList = "";
                                        boolean first = true;
                                        for (String _feature : rawFeatures) {
                                            if (!first)
                                                extendedList += ", ";
                                            extendedList += _feature;

                                            first = false;
                                        }

                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                            box.addView(Utils.fastTextView(MainActivity.this, Html.fromHtml(getString(R.string.features, extendedList), Html.FROM_HTML_MODE_COMPACT)));
                                        } else {
                                            //noinspection deprecation
                                            box.addView(Utils.fastTextView(MainActivity.this, Html.fromHtml(getString(R.string.features, extendedList))));

                                        }

                                        jta2.getSessionInfo(new ISession() {
                                            @Override
                                            public void onSessionInfo(String sessionID) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                    box.addView(Utils.fastTextView(MainActivity.this, Html.fromHtml(getString(R.string.sessionId, sessionID), Html.FROM_HTML_MODE_COMPACT)));
                                                } else {
                                                    //noinspection deprecation
                                                    box.addView(Utils.fastTextView(MainActivity.this, Html.fromHtml(getString(R.string.sessionId, sessionID))));
                                                }

                                                pd.dismiss();
                                                MainActivity.this.runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        new AlertDialog.Builder(MainActivity.this).setTitle(R.string.about_aria2)
                                                                .setView(box)
                                                                .setNeutralButton(R.string.saveSession, new DialogInterface.OnClickListener() {
                                                                    @Override
                                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                                        jta2.saveSession(new ISuccess() {
                                                                            @Override
                                                                            public void onSuccess() {
                                                                                Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.SESSION_SAVED);
                                                                            }

                                                                            @Override
                                                                            public void onException(Exception exception) {
                                                                                Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_SAVE_SESSION, exception);
                                                                            }
                                                                        });
                                                                    }
                                                                })
                                                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                                    @Override
                                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                                    }
                                                                })
                                                                .create().show();
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onException(Exception exception) {
                                                Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception);
                                                pd.dismiss();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onException(Exception exception) {
                                        Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception);
                                        pd.dismiss();
                                    }
                                });
                                return true;
                            default:
                                return true;
                        }
                    }

                    @Override
                    public void onProfileItemSelected(final SingleModeProfileItem profile) {
                        if (profile.getStatus() != ProfileItem.STATUS.ONLINE) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setMessage(R.string.serverOffline)
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
                                    }).create().show();
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
        assert mainRecyclerView != null;

        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mainRecyclerView.setLayoutManager(llm);

        final SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) findViewById(R.id.main_swipeLayout);
        assert swipeLayout != null;

        swipeLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorMetalink, R.color.colorTorrent);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                reloadPage();
            }
        });

        final ProgressDialog pd = Utils.fastProgressDialog(this, R.string.loading_downloads, true, false);
        loadingHandler = new LoadDownloads.ILoading() {
            @Override
            public void onStarted() {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing()) pd.show();
                    }
                });
            }

            @Override
            public void onLoaded(final List<Download> downloads) {
                adapter = new MainCardAdapter(MainActivity.this, downloads, new MainCardAdapter.IActionMore() {
                    @Override
                    public void onClick(View view, int position, Download item) {
                        Intent launchActivity = new Intent(MainActivity.this, MoreAboutDownloadActivity.class)
                                .putExtra("gid", item.gid)
                                .putExtra("name", item.getName())
                                .putExtra("isTorrent", item.isBitTorrent)
                                .putExtra("status", item.status.name());
                        if (!(item.status.equals(Download.STATUS.UNKNOWN) || item.status.equals(Download.STATUS.ERROR)))
                            MainActivity.this.startActivity(launchActivity);
                    }
                }, new MainCardAdapter.IMenuSelected() {
                    @Override
                    public void onItemSelected(Download item, DownloadAction.ACTION action) {
                        DownloadAction downloadAction;
                        try {
                            downloadAction = new DownloadAction(MainActivity.this);
                        } catch (IOException | NoSuchAlgorithmException ex) {
                            Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.WS_EXCEPTION, ex);
                            return;
                        }

                        DownloadAction.IMove iMove = new DownloadAction.IMove() {
                            @Override
                            public void onMoved(String gid) {
                                Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.MOVED, gid);
                            }

                            @Override
                            public void onException(Exception ex) {
                                Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_CHANGE_POSITION, ex);
                            }
                        };

                        switch (action) {
                            case PAUSE:
                                downloadAction.pause(MainActivity.this, item.gid, new DownloadAction.IPause() {
                                    @Override
                                    public void onPaused(String gid) {
                                        Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.PAUSED, gid);
                                    }

                                    @Override
                                    public void onException(Exception ex) {
                                        Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_PAUSE, ex);
                                    }
                                });
                                break;
                            case REMOVE:
                                downloadAction.remove(MainActivity.this, item.gid, item.status, new DownloadAction.IRemove() {
                                    @Override
                                    public void onRemoved(String gid) {
                                        Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.REMOVED, gid);
                                    }

                                    @Override
                                    public void onRemovedResult(String gid) {
                                        Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.REMOVED_RESULT, gid);
                                    }

                                    @Override
                                    public void onException(boolean b, Exception ex) {
                                        if (b)
                                            Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_REMOVE, ex);
                                        else
                                            Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_REMOVE_RESULT, ex);
                                    }
                                });
                                break;
                            case RESTART:
                                downloadAction.restart(item.gid, new DownloadAction.IRestart() {
                                    @Override
                                    public void onRestarted(String gid) {
                                        Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.RESTARTED);
                                    }

                                    @Override
                                    public void onException(Exception ex) {
                                        Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_ADD_DOWNLOAD, ex);
                                    }

                                    @Override
                                    public void onRemoveResultException(Exception ex) {
                                        Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_REMOVE_RESULT, ex);
                                    }

                                    @Override
                                    public void onGatheringInformationException(Exception ex) {
                                        Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex);
                                    }
                                });
                                break;
                            case RESUME:
                                downloadAction.unpause(item.gid, new DownloadAction.IUnpause() {
                                    @Override
                                    public void onUnpaused(String gid) {
                                        Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.RESUMED, gid);
                                    }

                                    @Override
                                    public void onException(Exception ex) {
                                        Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_UNPAUSE, ex);
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
                        drawerManager.updateBadge(downloads.size());

                        updateUI = new UpdateUI(MainActivity.this, (MainCardAdapter) mainRecyclerView.getAdapter());
                        new Thread(updateUI).start();

                        try {
                            pd.dismiss();
                            swipeLayout.setRefreshing(false);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                        if (getIntent().getStringExtra("gid") != null) {
                            Download item = ((MainCardAdapter) mainRecyclerView.getAdapter()).getItem(getIntent().getStringExtra("gid"));
                            Intent launchActivity = new Intent(MainActivity.this, MoreAboutDownloadActivity.class)
                                    .putExtra("gid", item.gid)
                                    .putExtra("isTorrent", item.isBitTorrent)
                                    .putExtra("status", item.status.name())
                                    .putExtra("name", item.getName());

                            if (item.status == Download.STATUS.UNKNOWN) return;
                            startActivity(launchActivity);
                        }
                    }
                });
            }

            @Override
            public void onException(boolean queuing, final Exception ex) {
                if (queuing) {
                    WebSocketing.notifyConnection(new WebSocketing.IConnecting() {
                        @Override
                        public void onDone(boolean connected) {
                            loadDownloads = new LoadDownloads(MainActivity.this, loadingHandler);
                            new Thread(loadDownloads).start();
                        }
                    });
                    return;
                }

                try {
                    pd.dismiss();
                    swipeLayout.setRefreshing(false);
                } catch (Exception exx) {
                    exx.printStackTrace();
                }

                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
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
                                System.exit(0);
                            }
                        })
                        .setNeutralButton(R.string.changeProfile, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                drawerManager.openProfiles(true);
                            }
                        });

                Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex, new Runnable() {
                    @Override
                    public void run() {
                        builder.create().show();
                        drawerManager.updateBadge(-1);
                    }
                });
            }
        };

        UpdateUI.stop(updateUI);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        long intervalLastSourceRefresh = System.currentTimeMillis() - sharedPreferences.getLong("lastSourceRefresh", System.currentTimeMillis());
        if ((intervalLastSourceRefresh > 604800000) || (intervalLastSourceRefresh < 100)) {
            new Parser().refreshSource(this, new Parser.ISourceProcessor() {
                @Override
                public void onStarted() {
                }

                @Override
                public void onDownloadEnded(String source) {
                }

                @Override
                public void onConnectionError(int code, String message) {
                    Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.CANT_REFRESH_SOURCE, code + ": " + message);
                }

                @Override
                public void onError(Exception ex) {
                    Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.CANT_REFRESH_SOURCE, ex);
                }

                @Override
                public void onFailed() {
                    Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.CANT_REFRESH_SOURCE);
                }

                @Override
                public void onEnd() {
                    Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.SOURCE_REFRESHED);
                }
            });
            sharedPreferences.edit().putLong("lastSourceRefresh", System.currentTimeMillis()).apply();
        }

        if (getIntent().getBooleanExtra("external", false)) {
            setTitle(getString(R.string.app_name) + " - Local device");

            saveExternalProfile(new SingleModeProfileItem("Local device",
                    "localhost",
                    getIntent().getIntExtra("port", 6800),
                    "/jsonrpc",
                    JTA2.AUTH_METHOD.TOKEN,
                    false,
                    getIntent().getStringExtra("token"),
                    false,
                    null)
                    .setGlobalProfileName("Local device"));
        }

        try {
            SingleModeProfileItem profile;
            if (getIntent().getBooleanExtra("external", false)) {
                profile = SingleModeProfileItem.fromString(this, "Local device");
            } else {
                profile = defaultProfile();
            }

            if (profile == null) {
                drawerManager.openProfiles(true);
                return;
            }

            setTitle(getString(R.string.app_name) + " - " + profile.getGlobalProfileName());

            startWithProfile(profile, false);
        } catch (IOException | JSONException ex) {
            Utils.UIToast(this, Utils.TOAST_MESSAGES.FATAL_EXCEPTION, ex);
        }


        Integer autoReloadDownloadsListRate = Integer.parseInt(sharedPreferences.getString("a2_downloadListRate", "0")) * 1000;
        boolean enableNotifications = sharedPreferences.getBoolean("a2_enableNotifications", true);

        fabMenu = (FloatingActionsMenu) findViewById(R.id.main_fab);
        assert fabMenu != null;
        fabMenu.setOnFloatingActionsMenuUpdateListener(new FloatingActionsMenu.OnFloatingActionsMenuUpdateListener() {
            @Override
            public void onMenuExpanded() {
                final View mask = findViewById(R.id.main_mask);
                assert mask != null;
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
                assert mask != null;
                mask.setVisibility(View.GONE);
                mask.setClickable(false);
            }
        });

        FloatingActionButton fabAddURI = (FloatingActionButton) findViewById(R.id.mainFab_addURI);
        assert fabAddURI != null;
        fabAddURI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AddURIActivity.class));
            }
        });
        final FloatingActionButton fabAddTorrent = (FloatingActionButton) findViewById(R.id.mainFab_addTorrent);
        assert fabAddTorrent != null;
        fabAddTorrent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AddTorrentActivity.class).putExtra("torrentMode", true));
            }
        });
        final FloatingActionButton fabAddMetalink = (FloatingActionButton) findViewById(R.id.mainFab_addMetalink);
        assert fabAddMetalink != null;
        fabAddMetalink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AddTorrentActivity.class).putExtra("torrentMode", false));
            }
        });

        if (getIntent().getBooleanExtra("backFromAddProfile", false)) {
            drawerManager.buildProfiles().openProfiles(false);
        }

        if (autoReloadDownloadsListRate != 0) {
            Timer reloadDownloadsListTimer = new Timer(false);
            reloadDownloadsListTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    UpdateUI.stop(updateUI, new IThread() {
                        @Override
                        public void stopped() {
                            loadDownloads = new LoadDownloads(MainActivity.this, loadingHandler);
                            new Thread(loadDownloads).start();
                        }
                    });
                }
            }, 1000, autoReloadDownloadsListRate);
        } else {
            loadDownloads = new LoadDownloads(this, loadingHandler);
            new Thread(loadDownloads).start();
        }

        try {
            WebSocketing.enableEventManager(this);
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }


        if (enableNotifications) {
            Intent startNotification = NotificationWebSocketService.createStartIntent(this, sharedPreferences.getString("a2_profileName", ""));
            if (startNotification != null)
                startService(startNotification);
            else
                Utils.UIToast(this, Utils.TOAST_MESSAGES.FATAL_EXCEPTION, "NULL notification intent");
        } else {
            stopService(new Intent(this, NotificationWebSocketService.class));
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerManager.onTogglerConfigurationChanged(newConfig);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerManager.syncTogglerState();
    }

    @Nullable
    public SingleModeProfileItem defaultProfile() throws IOException, JSONException {
        String lastProfile = PreferenceManager.getDefaultSharedPreferences(this).getString("lastUsedProfile", null);

        if (ProfileItem.exists(this, lastProfile)) {
            if (ProfileItem.isSingleMode(this, lastProfile))
                return SingleModeProfileItem.fromString(this, lastProfile);
            else
                return MultiModeProfileItem.fromString(this, lastProfile).getCurrentProfile(this);
        } else {
            return null;
        }
    }

    public void saveExternalProfile(@NonNull SingleModeProfileItem profile) {
        try {
            deleteFile("Local device.profile");

            FileOutputStream fOut = openFileOutput("Local device.profile", Context.MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fOut);

            osw.write(profile.toJSON().toString());
            osw.flush();
            osw.close();
        } catch (IOException | JSONException ex) {
            Utils.UIToast(this, Utils.TOAST_MESSAGES.FATAL_EXCEPTION, ex);
            ex.printStackTrace();
        }
    }

    public void startWithProfile(@NonNull SingleModeProfileItem profile, boolean recreate) {
        drawerManager.setCurrentProfile(profile, false);

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString("lastUsedProfile", profile.getGlobalProfileName())
                .putString("a2_profileName", profile.getProfileName())
                .putString("a2_serverIP", profile.getFullServerAddr())
                .putString("a2_authMethod", profile.getAuthMethod().name())
                .putString("a2_serverToken", profile.getServerToken())
                .putString("a2_serverUsername", profile.getServerUsername())
                .putString("a2_serverPassword", profile.getServerPassword())
                .putBoolean("a2_serverSSL", profile.isServerSSL())
                .putBoolean("a2_directDownload", profile.isDirectDownloadEnabled());

        if (profile.isDirectDownloadEnabled()) {
            editor.putString("dd_addr", profile.getDirectDownload().getAddress())
                    .putBoolean("dd_auth", profile.getDirectDownload().isAuth())
                    .putString("dd_user", profile.getDirectDownload().getUsername())
                    .putString("dd_passwd", profile.getDirectDownload().getPassword());
        }
        editor.apply();

        WebSocketing.destroyInstance();
        if (recreate)
            recreate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        getMenuInflater().inflate(R.menu.main_filters, menu.findItem(R.id.a2menu_filtering).getSubMenu());
        return true;
    }

    public void reloadPage() {
        reloadPage(null);
    }

    public void reloadPage(final IThread handler) {
        UpdateUI.stop(updateUI, new IThread() {
            @Override
            public void stopped() {
                if (handler != null) handler.stopped();
                loadDownloads = new LoadDownloads(MainActivity.this, loadingHandler);
                new Thread(loadDownloads).start();
            }
        });
    }

    @Override
    public void onBackPressed() {
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
                item.setChecked(!item.isChecked());

                if (item.isChecked())
                    adapter.removeFilter(Download.STATUS.ACTIVE);
                else
                    adapter.addFilter(Download.STATUS.ACTIVE);
            case R.id.a2menu_paused:
                item.setChecked(!item.isChecked());

                if (item.isChecked())
                    adapter.removeFilter(Download.STATUS.PAUSED);
                else
                    adapter.addFilter(Download.STATUS.PAUSED);
                break;
            case R.id.a2menu_error:
                item.setChecked(!item.isChecked());

                if (item.isChecked())
                    adapter.removeFilter(Download.STATUS.ERROR);
                else
                    adapter.addFilter(Download.STATUS.ERROR);
                break;
            case R.id.a2menu_waiting:
                item.setChecked(!item.isChecked());

                if (item.isChecked())
                    adapter.removeFilter(Download.STATUS.WAITING);
                else
                    adapter.addFilter(Download.STATUS.WAITING);
                break;
            case R.id.a2menu_complete:
                item.setChecked(!item.isChecked());

                if (item.isChecked())
                    adapter.removeFilter(Download.STATUS.COMPLETE);
                else
                    adapter.addFilter(Download.STATUS.COMPLETE);
                break;
            case R.id.a2menu_removed:
                item.setChecked(!item.isChecked());

                if (item.isChecked())
                    adapter.removeFilter(Download.STATUS.REMOVED);
                else
                    adapter.addFilter(Download.STATUS.REMOVED);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showOptionsDialog() {
        final List<OptionHeader> headers = new ArrayList<>();
        final Map<OptionHeader, OptionChild> children = new HashMap<>();

        final JTA2 jta2;
        try {
            jta2 = JTA2.newInstance(this);
        } catch (IOException | NoSuchAlgorithmException ex) {
            Utils.UIToast(this, Utils.TOAST_MESSAGES.WS_EXCEPTION, ex);
            return;
        }
        final ProgressDialog pd = Utils.fastProgressDialog(this, R.string.gathering_information, true, false);
        pd.show();

        jta2.getGlobalOption(new IOption() {
            @Override
            public void onOptions(Map<String, String> options) {
                LocalParser localOptions;
                try {
                    localOptions = new LocalParser(MainActivity.this, false);
                } catch (IOException | JSONException ex) {
                    pd.dismiss();
                    Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex);
                    return;
                }

                for (String resOption : getResources().getStringArray(R.array.globalOptions)) {
                    try {
                        OptionHeader header = new OptionHeader(resOption, localOptions.getCommandLine(resOption), options.get(resOption), false);
                        headers.add(header);

                        children.put(header, new OptionChild(
                                localOptions.getDefinition(resOption),
                                String.valueOf(localOptions.getDefaultValue(resOption)),
                                String.valueOf(options.get(resOption))));
                    } catch (JSONException ex) {
                        pd.dismiss();
                        Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex);
                    }
                }

                pd.dismiss();

                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                @SuppressLint("InflateParams") final View view = getLayoutInflater().inflate(R.layout.options_dialog, null);
                ((ViewGroup) view).removeView(view.findViewById(R.id.optionsDialog_info));
                ExpandableListView listView = (ExpandableListView) view.findViewById(R.id.moreAboutDownload_dialog_expandableListView);
                listView.setAdapter(new OptionAdapter(MainActivity.this, headers, children));

                builder.setView(view)
                        .setTitle(R.string.menu_globalOptions)
                        .setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Map<String, String> map = new HashMap<>();

                                for (Map.Entry<OptionHeader, OptionChild> item : children.entrySet()) {
                                    if (!item.getValue().isChanged()) continue;
                                    map.put(item.getKey().getOptionName(), item.getValue().getValue());
                                }

                                if (map.entrySet().size() == 0) return;

                                pd.show();

                                if (Analytics.isTrackingAllowed(MainActivity.this))
                                    Analytics.getDefaultTracker(MainActivity.this.getApplication()).send(new HitBuilders.EventBuilder()
                                            .setCategory(Analytics.CATEGORY_USER_INPUT)
                                            .setAction(Analytics.ACTION_CHANGED_GLOBAL_OPTIONS)
                                            .build());

                                jta2.changeGlobalOption(map, new ISuccess() {
                                    @Override
                                    public void onSuccess() {
                                        pd.dismiss();
                                        Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.DOWNLOAD_OPTIONS_CHANGED);

                                        reloadPage();
                                    }

                                    @Override
                                    public void onException(Exception exception) {
                                        pd.dismiss();
                                        Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_CHANGE_OPTIONS, exception);
                                    }
                                });
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                            }
                        });

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final AlertDialog dialog = builder.create();
                        dialog.show();
                        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

                        ViewTreeObserver vto = view.getViewTreeObserver();
                        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                dialog.getWindow().setLayout(dialog.getWindow().getDecorView().getWidth(), dialog.getWindow().getDecorView().getHeight());
                                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            }
                        });
                    }
                });
            }

            @Override
            public void onException(Exception exception) {
                pd.dismiss();
                Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception);
            }
        });
    }
}