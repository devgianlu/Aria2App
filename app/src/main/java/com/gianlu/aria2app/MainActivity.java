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
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;

import com.gianlu.aria2app.DownloadsListing.Charting;
import com.gianlu.aria2app.DownloadsListing.DownloadItem;
import com.gianlu.aria2app.DownloadsListing.DownloadItemAdapter;
import com.gianlu.aria2app.DownloadsListing.ILoadDownloads;
import com.gianlu.aria2app.DownloadsListing.LoadDownloads;
import com.gianlu.aria2app.Google.Analytics;
import com.gianlu.aria2app.Google.UncaughtExceptionHandler;
import com.gianlu.aria2app.Main.AddDownloadActivity;
import com.gianlu.aria2app.Main.IThread;
import com.gianlu.aria2app.Main.UpdateUI;
import com.gianlu.aria2app.Options.OptionAdapter;
import com.gianlu.aria2app.Options.OptionChild;
import com.gianlu.aria2app.Options.OptionHeader;
import com.gianlu.aria2app.Services.InAppAdapter;
import com.gianlu.aria2app.Services.InAppWebSocket;
import com.gianlu.aria2app.Services.NotificationWebSocketService;
import com.gianlu.jtitan.Aria2Helper.Download;
import com.gianlu.jtitan.Aria2Helper.IOption;
import com.gianlu.jtitan.Aria2Helper.ISuccess;
import com.gianlu.jtitan.Aria2Helper.JTA2;
import com.github.mikephil.charting.charts.LineChart;
import com.google.android.gms.analytics.HitBuilders;

import java.util.ArrayList;
import java.util.Arrays;
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
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("a2_profileName", getIntent().getStringExtra("profileName"))
                    .putString("a2_serverIP", getIntent().getStringExtra("serverIP"))
                    .putBoolean("a2_serverAuth", getIntent().getBooleanExtra("serverAuth", false))
                    .putString("a2_serverToken", getIntent().getStringExtra("serverToken"))
                    .putString("a2_directDownload", getIntent().getStringExtra("directDownload"))
                    .putBoolean("a2_serverSSL", getIntent().getBooleanExtra("serverSSL", false))
                    .putBoolean("a2_directDownload", getIntent().getBooleanExtra("serverDirectDownload", false));
            if (getIntent().getBooleanExtra("serverDirectDownload", false)) {
                editor.putString("dd_addr", getIntent().getStringExtra("ddAddr"))
                        .putBoolean("dd_auth", getIntent().getBooleanExtra("ddAuth", false))
                        .putString("dd_user", getIntent().getStringExtra("ddUser"))
                        .putString("dd_passwd", getIntent().getStringExtra("ddPasswd"));
            }
            editor.apply();
        } else {
            setTitle(getResources().getString(R.string.app_name) + " - " + sharedPreferences.getString("a2_profileName", getString(R.string.unknown_profile)));
        }
        Integer autoReloadDownloadsListRate = Integer.parseInt(sharedPreferences.getString("a2_downloadListRate", "0")) * 1000;
        boolean enableNotifications = sharedPreferences.getBoolean("a2_enableNotifications", true);

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
                            DownloadAction.ACTION action = new ArrayList<>(list.keySet()).get(i);
                            DownloadAction.IMove iMove = new DownloadAction.IMove() {
                                @Override
                                public void onMoved() {

                                }

                                @Override
                                public void onException(Exception ex) {
                                    Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_CHANGE_POSITION, ex.getMessage());
                                }
                            };

                            switch (action) {
                                case PAUSE:
                                    DownloadAction.pause(MainActivity.this, item.getDownloadGID(), new DownloadAction.IPause() {
                                        @Override
                                        public void onPaused() {

                                        }

                                        @Override
                                        public void onException(Exception ex) {
                                            Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_PAUSE, ex.getMessage());
                                        }
                                    });
                                    break;
                                case REMOVE:
                                    DownloadAction.remove(MainActivity.this, item.getDownloadGID(), item.download.status, new DownloadAction.IRemove() {
                                        @Override
                                        public void onRemoved() {

                                        }

                                        @Override
                                        public void onRemovedResult() {

                                        }

                                        @Override
                                        public void onException(boolean b, Exception ex) {
                                            if (b)
                                                Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_REMOVE, ex.getMessage());
                                            else
                                                Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_REMOVE_RESULT, ex.getMessage());
                                        }
                                    });
                                    break;
                                case RESTART:
                                    DownloadAction.restart(MainActivity.this, item.getDownloadGID(), new DownloadAction.IRestart() {
                                        @Override
                                        public void onRestarted(String gid) {

                                        }

                                        @Override
                                        public void onException(Exception ex) {
                                            Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_ADD_DOWNLOAD, ex.getMessage());
                                        }

                                        @Override
                                        public void onRemoveResultException(Exception ex) {
                                            Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_REMOVE_RESULT, ex.getMessage());
                                        }

                                        @Override
                                        public void onGatheringInformationException(Exception ex) {
                                            Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex.getMessage());
                                        }
                                    });
                                    break;
                                case RESUME:
                                    DownloadAction.unpause(MainActivity.this, item.getDownloadGID(), new DownloadAction.IUnpause() {
                                        @Override
                                        public void onUnpaused() {

                                        }

                                        @Override
                                        public void onException(Exception ex) {
                                            Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_UNPAUSE, ex.getMessage());
                                        }
                                    });
                                    break;
                                case MOVE_DOWN:
                                    DownloadAction.moveDown(MainActivity.this, item.getDownloadGID(), iMove);
                                    break;
                                case MOVE_UP:
                                    DownloadAction.moveUp(MainActivity.this, item.getDownloadGID(), iMove);
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
        final ProgressDialog progressDialog = Utils.fastProgressDialog(this, R.string.loading_downloads, true, false);

        IloadDownloads = new ILoadDownloads() {
            @Override
            public void onStart() {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!MainActivity.this.isFinishing()) progressDialog.show();
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
                                if (progressDialog.isShowing()) progressDialog.dismiss();
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
                    progressDialog.dismiss();

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

        new InAppWebSocket(this).connect(new InAppAdapter(this, updater, loadDownloads, downloadsListView, IloadDownloads));

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.a2menu_refreshPage:
                if (updater != null) {
                    updater.stop(new IThread() {
                        @Override
                        public void stopped() {
                            loadDownloads = new LoadDownloads(MainActivity.this, downloadsListView, IloadDownloads);
                            new Thread(loadDownloads).start();
                        }
                    });
                } else {
                    loadDownloads = new LoadDownloads(this, downloadsListView, IloadDownloads);
                    new Thread(loadDownloads).start();
                }
                break;
            case R.id.a2menu_addDownload:
                startActivity(new Intent(this, AddDownloadActivity.class));
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

        final JTA2 jta2 = Utils.readyJTA2(this);
        final ProgressDialog progressDialog = Utils.fastProgressDialog(this, R.string.gathering_information, true, false);
        progressDialog.show();

        jta2.getGlobalOption(new IOption() {
            @Override
            public void onOptions(Map<String, String> options) {
                String[] availableOptions;
                availableOptions = getResources().getStringArray(R.array.globalOptions);

                for (String option : availableOptions) {
                    String optionn = option;
                    if (option.equals("continue")) optionn += "e";

                    String[] optionIdentifier = getResources().getStringArray(getResources().getIdentifier(optionn.replace("-", "_"), "array", "com.gianlu.aria2app"));
                    String optionName = optionIdentifier[0];
                    String optionDesc = optionIdentifier[1];
                    OptionChild.OPTION_TYPE optionType;
                    Object optionDefaultVal;

                    String responseOption = options.get(option);
                    if (responseOption == null) responseOption = "";
                    OptionHeader thisHeader = new OptionHeader(optionName, "--" + option, responseOption);
                    headers.add(thisHeader);
                    OptionChild thisChild;

                    switch (optionIdentifier[2]) {
                        case "string":
                            optionType = OptionChild.OPTION_TYPE.STRING;
                            optionDefaultVal = String.valueOf(optionIdentifier[3]);

                            thisChild = new OptionChild(option, responseOption, optionDefaultVal, optionType, optionDesc);
                            break;
                        case "integer":
                            optionType = OptionChild.OPTION_TYPE.INTEGER;
                            optionDefaultVal = Integer.parseInt(optionIdentifier[3]);

                            thisChild = new OptionChild(option, responseOption, optionDefaultVal, optionType, optionDesc);
                            break;
                        case "boolean":
                            optionType = OptionChild.OPTION_TYPE.BOOLEAN;
                            optionDefaultVal = Boolean.parseBoolean(optionIdentifier[3]);

                            thisChild = new OptionChild(option, responseOption, optionDefaultVal, optionType, optionDesc);
                            break;
                        default:
                            // optionType = OptionChild.OPTION_TYPE.MULTIPLE;
                            String[] possibleValues = optionIdentifier[2].split(",");
                            optionDefaultVal = optionIdentifier[3];
                            thisChild = new OptionChild(option, responseOption, optionDefaultVal, optionDesc, Arrays.asList(possibleValues));
                            break;
                    }

                    children.put(thisHeader, thisChild);
                }

                progressDialog.dismiss();

                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                @SuppressLint("InflateParams") final View view = getLayoutInflater().inflate(R.layout.options_dialog, null);
                ExpandableListView listView = (ExpandableListView) view.findViewById(R.id.moreAboutDownload_dialog_expandableListView);
                listView.setAdapter(new OptionAdapter(MainActivity.this, headers, children));

                builder.setView(view)
                        .setTitle(R.string.menu_globalOptions)
                        .setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Map<String, String> map = new HashMap<>();

                                for (Map.Entry<OptionHeader, OptionChild> item : children.entrySet()) {
                                    if (item.getValue().getCurrentValue() == null) continue;
                                    if (item.getValue().getCurrentValue().equals("")) {
                                        map.put(item.getValue().getOption(), String.valueOf(item.getValue().getDefaultVal()));
                                    } else {
                                        map.put(item.getValue().getOption(), String.valueOf(item.getValue().getCurrentValue()));
                                    }
                                }

                                if (map.entrySet().size() == 0) return;

                                progressDialog.show();

                                if (Analytics.isTrackingAllowed(MainActivity.this))
                                    Analytics.getDefaultTracker(MainActivity.this.getApplication()).send(new HitBuilders.EventBuilder()
                                            .setCategory(Analytics.CATEGORY_USER_INPUT)
                                            .setAction(Analytics.ACTION_CHANGED_GLOBAL_OPTIONS)
                                            .build());

                                jta2.changeGlobalOption(map, new ISuccess() {
                                    @Override
                                    public void onSuccess() {
                                        progressDialog.dismiss();
                                        Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.DOWNLOAD_OPTIONS_CHANGED);

                                        if (updater != null) {
                                            updater.stop(new IThread() {
                                                @Override
                                                public void stopped() {
                                                    Charting.newChart(MainActivity.this, mainChart);
                                                    updater = new UpdateUI(MainActivity.this, mainChart, downloadsListView);
                                                    new Thread(updater).start();
                                                }
                                            });
                                        } else {
                                            Charting.newChart(MainActivity.this, mainChart);
                                            updater = new UpdateUI(MainActivity.this, mainChart, downloadsListView);
                                            new Thread(updater).start();
                                        }
                                    }

                                    @Override
                                    public void onException(Exception exception) {
                                        progressDialog.dismiss();
                                        Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_CHANGE_OPTIONS, exception.getMessage());
                                    }
                                });
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (updater != null) {
                                    updater.stop(new IThread() {
                                        @Override
                                        public void stopped() {
                                            Charting.newChart(MainActivity.this, mainChart);
                                            updater = new UpdateUI(MainActivity.this, mainChart, downloadsListView);
                                            new Thread(updater).start();
                                        }
                                    });
                                } else {
                                    Charting.newChart(MainActivity.this, mainChart);
                                    updater = new UpdateUI(MainActivity.this, mainChart, downloadsListView);
                                    new Thread(updater).start();
                                }
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
                progressDialog.dismiss();
                Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception.getMessage());
            }
        });
    }
}