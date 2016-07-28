package com.gianlu.aria2app;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.ArrayMap;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.gianlu.aria2app.DownloadsListing.Charting;
import com.gianlu.aria2app.DownloadsListing.DownloadItem;
import com.gianlu.aria2app.DownloadsListing.DownloadItemAdapter;
import com.gianlu.aria2app.DownloadsListing.ILoadDownloads;
import com.gianlu.aria2app.DownloadsListing.LoadDownloads;
import com.gianlu.aria2app.Google.Analytics;
import com.gianlu.aria2app.Google.UncaughtExceptionHandler;
import com.gianlu.aria2app.Main.AddTorrentActivity;
import com.gianlu.aria2app.Main.AddURIActivity;
import com.gianlu.aria2app.Main.IThread;
import com.gianlu.aria2app.Main.UpdateUI;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.IOption;
import com.gianlu.aria2app.NetIO.JTA2.ISuccess;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.WebSocketing;
import com.gianlu.aria2app.Options.LocalParser;
import com.gianlu.aria2app.Options.OptionAdapter;
import com.gianlu.aria2app.Options.OptionChild;
import com.gianlu.aria2app.Options.OptionHeader;
import com.gianlu.aria2app.SelectProfile.SingleModeProfileItem;
import com.gianlu.aria2app.Services.NotificationWebSocketService;
import com.github.mikephil.charting.charts.LineChart;
import com.google.android.gms.analytics.HitBuilders;

import org.json.JSONException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private LineChart mainChart;
    private ListView downloadsListView;
    private UpdateUI updater;
    private LoadDownloads loadDownloads;
    private ILoadDownloads IloadDownloads;
    private Timer reloadDownloadsListTimer;
    private List<AlertDialog> dialogs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        UncaughtExceptionHandler.application = getApplication();
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(this));

        mainChart = (LineChart) findViewById(R.id.mainChart);
        Charting.newChart(this, mainChart);
        downloadsListView = (ListView) findViewById(R.id.mainDownloadsListView);

        if (updater != null) updater.stop();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (getIntent().getStringExtra("profileName") != null) {
            setTitle(getResources().getString(R.string.app_name) + " - " + getIntent().getStringExtra("profileName"));

            SingleModeProfileItem profile = getIntent().getParcelableExtra("profile");
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("a2_profileName", profile.getProfileName())
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
        } else {
            setTitle(getResources().getString(R.string.app_name) + " - " + sharedPreferences.getString("a2_profileName", getString(R.string.unknown_profile)));
        }
        Integer autoReloadDownloadsListRate = Integer.parseInt(sharedPreferences.getString("a2_downloadListRate", "0")) * 1000;
        boolean enableNotifications = sharedPreferences.getBoolean("a2_enableNotifications", true);

        // Start WebSocketing and enabling event manager
        try {
            WebSocketing.enableEventManager(this);
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        FloatingActionButton fabAddURI = (FloatingActionButton) findViewById(R.id.mainFAB_addURI);
        assert fabAddURI != null;
        fabAddURI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AddURIActivity.class));
            }
        });
        FloatingActionButton fabAddTorrent = (FloatingActionButton) findViewById(R.id.mainFAB_addTorrent);
        assert fabAddTorrent != null;
        fabAddTorrent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AddTorrentActivity.class).putExtra("torrentMode", true));
            }
        });
        FloatingActionButton fabAddMetalink = (FloatingActionButton) findViewById(R.id.mainFAB_addMetalink);
        assert fabAddMetalink != null;
        fabAddMetalink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AddTorrentActivity.class).putExtra("torrentMode", false));
            }
        });

        downloadsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                DownloadItem item = (DownloadItem) adapterView.getItemAtPosition(i);
                Intent launchActivity = new Intent(MainActivity.this, MoreAboutDownloadActivity.class)
                        .putExtra("gid", item.download.GID)
                        .putExtra("name", item.download.getName())
                        .putExtra("status", item.download.status.name());
                if (!(item.getDownloadStatus().equals(Download.STATUS.UNKNOWN) || item.getDownloadStatus().equals(Download.STATUS.ERROR)))
                    startActivity(launchActivity);
            }
        });
        downloadsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                final DownloadItem item = (DownloadItem) adapterView.getItemAtPosition(i);
                if (!item.getDownloadStatus().equals(Download.STATUS.UNKNOWN)) {
                    if (updater != null) updater.stop();

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle(item.download.getName());
                    final Map<DownloadAction.ACTION, String> list = new ArrayMap<>();
                    list.put(DownloadAction.ACTION.PAUSE, getString(R.string.pause));
                    list.put(DownloadAction.ACTION.RESUME, getString(R.string.resume));
                    list.put(DownloadAction.ACTION.REMOVE, getString(R.string.remove));
                    list.put(DownloadAction.ACTION.MOVE_DOWN, getString(R.string.move_down));
                    list.put(DownloadAction.ACTION.MOVE_UP, getString(R.string.move_up));
                    list.put(DownloadAction.ACTION.RESTART, getString(R.string.restart));
                    list.put(DownloadAction.ACTION.SHOW_MORE, getString(R.string.show_more));

                    switch (item.download.status) {
                        case ACTIVE:
                            list.remove(DownloadAction.ACTION.RESUME);
                            list.remove(DownloadAction.ACTION.RESTART);
                            list.remove(DownloadAction.ACTION.MOVE_UP);
                            list.remove(DownloadAction.ACTION.MOVE_DOWN);
                            break;
                        case WAITING:
                            list.remove(DownloadAction.ACTION.PAUSE);
                            list.remove(DownloadAction.ACTION.RESUME);
                            list.remove(DownloadAction.ACTION.RESTART);
                            break;
                        case PAUSED:
                            list.remove(DownloadAction.ACTION.PAUSE);
                            list.remove(DownloadAction.ACTION.RESTART);
                            list.remove(DownloadAction.ACTION.MOVE_UP);
                            list.remove(DownloadAction.ACTION.MOVE_DOWN);
                            break;
                        case COMPLETE:
                            list.remove(DownloadAction.ACTION.PAUSE);
                            list.remove(DownloadAction.ACTION.RESUME);
                            list.remove(DownloadAction.ACTION.RESTART);
                            list.remove(DownloadAction.ACTION.MOVE_UP);
                            list.remove(DownloadAction.ACTION.MOVE_DOWN);
                            break;
                        case ERROR:
                            list.remove(DownloadAction.ACTION.PAUSE);
                            list.remove(DownloadAction.ACTION.RESUME);
                            list.remove(DownloadAction.ACTION.RESTART);
                            list.remove(DownloadAction.ACTION.SHOW_MORE);
                            list.remove(DownloadAction.ACTION.MOVE_UP);
                            list.remove(DownloadAction.ACTION.MOVE_DOWN);
                            break;
                        case REMOVED:
                            if (item.download.isBitTorrent)
                                list.remove(DownloadAction.ACTION.RESTART);
                            list.remove(DownloadAction.ACTION.PAUSE);
                            list.remove(DownloadAction.ACTION.RESUME);
                            list.remove(DownloadAction.ACTION.SHOW_MORE);
                            list.remove(DownloadAction.ACTION.MOVE_UP);
                            list.remove(DownloadAction.ACTION.MOVE_DOWN);
                            break;
                    }

                    builder.setAdapter(new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, new ArrayList<>(list.values())), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            DownloadAction downloadAction;
                            try {
                                downloadAction = new DownloadAction(MainActivity.this);
                            } catch (IOException | NoSuchAlgorithmException ex) {
                                Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.WS_EXCEPTION, ex);
                                return;
                            }

                            DownloadAction.ACTION action = new ArrayList<>(list.keySet()).get(i);
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
                                    downloadAction.pause(MainActivity.this, item.getDownloadGID(), new DownloadAction.IPause() {
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
                                    downloadAction.remove(MainActivity.this, item.getDownloadGID(), item.download.status, new DownloadAction.IRemove() {
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
                                    downloadAction.restart(item.getDownloadGID(), new DownloadAction.IRestart() {
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
                                    downloadAction.unpause(item.getDownloadGID(), new DownloadAction.IUnpause() {
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
                                    downloadAction.moveDown(item.getDownloadGID(), iMove);
                                    break;
                                case MOVE_UP:
                                    downloadAction.moveUp(item.getDownloadGID(), iMove);
                                    break;
                                case SHOW_MORE:
                                    Intent launchActivity = new Intent(MainActivity.this, MoreAboutDownloadActivity.class)
                                            .putExtra("gid", item.download.GID)
                                            .putExtra("status", item.download.status.name())
                                            .putExtra("name", item.download.getName());
                                    startActivity(launchActivity);
                                    break;
                            }
                        }
                    });

                    AlertDialog dialog = builder.create();
                    dialogs.add(dialog);
                    dialog.show();
                }
                return true;
            }
        });

        final ProgressDialog pd = Utils.fastProgressDialog(this, R.string.loading_downloads, true, false);
        IloadDownloads = new ILoadDownloads() {
            @Override
            public void onStart() {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!MainActivity.this.isFinishing()) pd.show();
                    }
                });
            }

            @Override
            public void onException(Exception ex) {
                try {
                    if (pd.isShowing()) pd.dismiss();
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
                        });

                Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex, new Runnable() {
                    @Override
                    public void run() {
                        builder.create().show();
                    }
                });

            }

            @Override
            public void onEnd() {
                if (updater != null) {
                    updater.stop(new IThread() {
                        @Override
                        public void stopped() {
                            Charting.newChart(MainActivity.this, mainChart);
                            updater = new UpdateUI(MainActivity.this, mainChart, downloadsListView);
                            new Thread(updater).start();
                            try {
                                if (pd.isShowing()) pd.dismiss();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }

                            if (getIntent().getStringExtra("gid") == null) return;

                            Download item = ((DownloadItemAdapter) downloadsListView.getAdapter()).getItem(getIntent().getStringExtra("gid")).download;
                            Intent launchActivity = new Intent(MainActivity.this, MoreAboutDownloadActivity.class)
                                    .putExtra("gid", item.GID)
                                    .putExtra("status", item.status.name())
                                    .putExtra("name", item.getName());

                            if (item.status == Download.STATUS.UNKNOWN) return;
                            startActivity(launchActivity);
                        }
                    });
                } else {
                    Charting.newChart(MainActivity.this, mainChart);
                    updater = new UpdateUI(MainActivity.this, mainChart, downloadsListView);
                    new Thread(updater).start();
                    try {
                        if (pd.isShowing()) pd.dismiss();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    if (getIntent().getStringExtra("gid") == null) return;

                    Download item = ((DownloadItemAdapter) downloadsListView.getAdapter()).getItem(getIntent().getStringExtra("gid")).download;
                    Intent launchActivity = new Intent(MainActivity.this, MoreAboutDownloadActivity.class)
                            .putExtra("gid", item.GID)
                            .putExtra("status", item.status.name())
                            .putExtra("name", item.getName());

                    if (item.status == Download.STATUS.UNKNOWN) return;
                    startActivity(launchActivity);
                }
            }
        };
        loadDownloads = new LoadDownloads(this, downloadsListView, IloadDownloads);
        new Thread(loadDownloads).start();

        if (autoReloadDownloadsListRate != 0) {
            reloadDownloadsListTimer = new Timer(false);
            reloadDownloadsListTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (updater != null) updater.stop(new IThread() {
                        @Override
                        public void stopped() {
                            loadDownloads = new LoadDownloads(MainActivity.this, downloadsListView, IloadDownloads);
                            new Thread(loadDownloads).start();
                        }
                    });
                }
            }, 1000, autoReloadDownloadsListRate);
        }

        if (enableNotifications) {
            Intent startNotification = NotificationWebSocketService.createStartIntent(this, sharedPreferences.getString("a2_profileName", ""));
            if (startNotification != null) {
                startService(startNotification);
            } else {
                Utils.UIToast(this, Utils.TOAST_MESSAGES.FATAL_EXCEPTION, "NULL notification intent");
            }
        } else {
            stopService(new Intent(this, NotificationWebSocketService.class));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        getMenuInflater().inflate(R.menu.main_filters, menu.findItem(R.id.a2menu_filtering).getSubMenu());
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reloadDownloadsListTimer != null) reloadDownloadsListTimer.cancel();
        if (updater != null) updater.stop();
        for (AlertDialog dialog : dialogs) dialog.dismiss();
        finishActivity(0);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (reloadDownloadsListTimer != null) reloadDownloadsListTimer.cancel();
        if (updater != null) updater.stop();
        for (AlertDialog dialog : dialogs) dialog.dismiss();
        finishActivity(0);
    }

    public void reloadPage() {
        if (updater != null) {
            updater.stop(new IThread() {
                @Override
                public void stopped() {
                    Charting.newChart(MainActivity.this, mainChart);
                    loadDownloads = new LoadDownloads(MainActivity.this, downloadsListView, IloadDownloads);
                    new Thread(loadDownloads).start();
                }
            });
        } else {
            Charting.newChart(MainActivity.this, mainChart);
            loadDownloads = new LoadDownloads(this, downloadsListView, IloadDownloads);
            new Thread(loadDownloads).start();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.a2menu_refreshPage:
                reloadPage();
                break;
            case R.id.a2menu_globalOptions:
                showOptionsDialog();
                break;
            case R.id.a2menu_preferences:
                startActivity(new Intent(this, MainSettingsActivity.class));
                break;
            case R.id.a2menu_terminal:
                startActivity(new Intent(this, TerminalActivity.class));
                break;
            // Filters
            case R.id.a2menu_active:
                item.setChecked(!item.isChecked());
                if (downloadsListView.getAdapter() == null) break;
                if (item.isChecked())
                    ((DownloadItemAdapter) downloadsListView.getAdapter()).removeFilter(Download.STATUS.ACTIVE);
                else
                    ((DownloadItemAdapter) downloadsListView.getAdapter()).addFilter(Download.STATUS.ACTIVE);
                break;
            case R.id.a2menu_paused:
                item.setChecked(!item.isChecked());
                if (downloadsListView.getAdapter() == null) break;
                if (item.isChecked())
                    ((DownloadItemAdapter) downloadsListView.getAdapter()).removeFilter(Download.STATUS.PAUSED);
                else
                    ((DownloadItemAdapter) downloadsListView.getAdapter()).addFilter(Download.STATUS.PAUSED);
                break;
            case R.id.a2menu_error:
                item.setChecked(!item.isChecked());
                if (downloadsListView.getAdapter() == null) break;
                if (item.isChecked())
                    ((DownloadItemAdapter) downloadsListView.getAdapter()).removeFilter(Download.STATUS.ERROR);
                else
                    ((DownloadItemAdapter) downloadsListView.getAdapter()).addFilter(Download.STATUS.ERROR);
                break;
            case R.id.a2menu_waiting:
                item.setChecked(!item.isChecked());
                if (downloadsListView.getAdapter() == null) break;
                if (item.isChecked())
                    ((DownloadItemAdapter) downloadsListView.getAdapter()).removeFilter(Download.STATUS.WAITING);
                else
                    ((DownloadItemAdapter) downloadsListView.getAdapter()).addFilter(Download.STATUS.WAITING);
                break;
            case R.id.a2menu_complete:
                item.setChecked(!item.isChecked());
                if (downloadsListView.getAdapter() == null) break;
                if (item.isChecked())
                    ((DownloadItemAdapter) downloadsListView.getAdapter()).removeFilter(Download.STATUS.COMPLETE);
                else
                    ((DownloadItemAdapter) downloadsListView.getAdapter()).addFilter(Download.STATUS.COMPLETE);
                break;
            case R.id.a2menu_removed:
                item.setChecked(!item.isChecked());
                if (downloadsListView.getAdapter() == null) break;
                if (item.isChecked())
                    ((DownloadItemAdapter) downloadsListView.getAdapter()).removeFilter(Download.STATUS.REMOVED);
                else
                    ((DownloadItemAdapter) downloadsListView.getAdapter()).addFilter(Download.STATUS.REMOVED);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showOptionsDialog() {
        final List<OptionHeader> headers = new ArrayList<>();
        final Map<OptionHeader, OptionChild> children = new HashMap<>();

        final JTA2 jta2;
        try {
            jta2 = Utils.readyJTA2(this);
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
                                reloadPage();
                            }
                        });

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final AlertDialog dialog = builder.create();
                        dialogs.add(dialog);
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