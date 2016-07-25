package com.gianlu.aria2app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ExpandableListView;

import com.gianlu.aria2app.DownloadsListing.Charting;
import com.gianlu.aria2app.Google.Analytics;
import com.gianlu.aria2app.Main.IThread;
import com.gianlu.aria2app.MoreAboutDownload.UpdateUI;
import com.gianlu.aria2app.Options.OptionAdapter;
import com.gianlu.aria2app.Options.OptionChild;
import com.gianlu.aria2app.Options.OptionHeader;
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

public class MoreAboutDownloadActivity extends AppCompatActivity {
    private UpdateUI updateUI;
    private String gid;
    private Download.STATUS status;
    private LineChart chart;
    private ViewGroup rootView;
    private boolean canWrite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more_about_download);

        View root = findViewById(android.R.id.content);
        assert root != null;
        rootView = (ViewGroup) root.getRootView();

        gid = getIntent().getStringExtra("gid");
        status = Download.STATUS.valueOf(getIntent().getStringExtra("status"));
        setTitle(getIntent().getStringExtra("name"));

        chart = (LineChart) findViewById(R.id.moreAboutDownload_chart);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            canWrite = true;
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.externalStorage_box)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(MoreAboutDownloadActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 45);
                            }
                        });
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 45);
            }
        }

        Charting.newChart(chart);
        updateUI = new UpdateUI(this, new UpdateUI.IFirstUpdate() {
            @Override
            public void onFirstUpdate(Download item) { /* NOT USED */}
        }, canWrite, gid, chart, rootView);
        new Thread(updateUI).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.more_about_download, menu);
        MenuItem item = menu.findItem(R.id.moreAboutDownloadMenu_options);
        if (status.equals(Download.STATUS.COMPLETE) || status.equals(Download.STATUS.ERROR) || status.equals(Download.STATUS.REMOVED))
            item.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.moreAboutDownloadMenu_refresh:
                if (updateUI != null) {
                    updateUI.stop(new IThread() {
                        @Override
                        public void stopped() {
                            updateUI = new UpdateUI(MoreAboutDownloadActivity.this, null, canWrite, gid, chart, rootView);
                            new Thread(updateUI).start();
                        }
                    });
                } else {
                    updateUI = new UpdateUI(this, null, canWrite, gid, chart, rootView);
                    new Thread(updateUI).start();
                }
                break;
            case R.id.moreAboutDownloadMenu_options:
                showOptionsDialog();
                break;
            case android.R.id.home:
                onBackPressed();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if (updateUI != null) updateUI.stop();
        finishActivity(0);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, MainActivity.class));
        finishActivity(0);
        super.onBackPressed();
    }

    @Override
    protected void onStop() {
        if (updateUI != null) updateUI.stop();
        finishActivity(0);
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 45:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    canWrite = true;
                } else {
                    canWrite = false;
                    Utils.UIToast(this, Utils.TOAST_MESSAGES.NO_WRITE_PERMISSION);
                }
                break;
        }
    }

    private void showOptionsDialog() {
        final List<OptionHeader> headers = new ArrayList<>();
        final Map<OptionHeader, OptionChild> children = new HashMap<>();

        final JTA2 jta2 = Utils.readyJTA2(this);
        final ProgressDialog progressDialog = Utils.fastProgressDialog(this, R.string.gathering_information, true, false);
        progressDialog.show();

        jta2.getOption(gid, new IOption() {
            @Override
            public void onOptions(Map<String, String> options) {
                String[] availableOptions;
                if (status.equals(Download.STATUS.ACTIVE)) {
                    availableOptions = getResources().getStringArray(R.array.activeDownloadOptions);
                } else {
                    availableOptions = getResources().getStringArray(R.array.pausedDownloadOptions);
                }

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

                final AlertDialog.Builder builder = new AlertDialog.Builder(MoreAboutDownloadActivity.this);

                @SuppressLint("InflateParams") View view = getLayoutInflater().inflate(R.layout.options_dialog, null);
                ExpandableListView listView = (ExpandableListView) view.findViewById(R.id.moreAboutDownload_dialog_expandableListView);
                listView.setAdapter(new OptionAdapter(MoreAboutDownloadActivity.this, headers, children));

                builder.setView(view)
                        .setTitle(R.string.options)
                        .setPositiveButton(R.string.change, new DialogInterface.OnClickListener() {
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

                                if (Analytics.isTrackingAllowed(MoreAboutDownloadActivity.this))
                                    Analytics.getDefaultTracker(MoreAboutDownloadActivity.this.getApplication()).send(new HitBuilders.EventBuilder()
                                            .setCategory(Analytics.CATEGORY_USER_INPUT)
                                            .setAction(Analytics.ACTION_CHANGED_DOWNLOAD_OPTIONS)
                                            .build());

                                jta2.changeOption(gid, map, new ISuccess() {
                                    @Override
                                    public void onSuccess() {
                                        progressDialog.dismiss();
                                        Utils.UIToast(MoreAboutDownloadActivity.this, Utils.TOAST_MESSAGES.DOWNLOAD_OPTIONS_CHANGED);

                                        MoreAboutDownloadActivity.this.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (updateUI != null) {
                                                    updateUI.stop(new IThread() {
                                                        @Override
                                                        public void stopped() {
                                                            Charting.newChart(chart);
                                                            updateUI = new UpdateUI(MoreAboutDownloadActivity.this, null, canWrite, gid, chart, rootView);
                                                            new Thread(updateUI).start();
                                                        }
                                                    });
                                                } else {
                                                    Charting.newChart(chart);
                                                    updateUI = new UpdateUI(MoreAboutDownloadActivity.this, null, canWrite, gid, chart, rootView);
                                                    new Thread(updateUI).start();
                                                }
                                            }
                                        });
                                    }

                                    @Override
                                    public void onException(Exception exception) {
                                        progressDialog.dismiss();
                                        Utils.UIToast(MoreAboutDownloadActivity.this, Utils.TOAST_MESSAGES.FAILED_CHANGE_OPTIONS, exception.getMessage());
                                    }
                                });
                            }
                        });

                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        MoreAboutDownloadActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (updateUI != null) {
                                    updateUI.stop(new IThread() {
                                        @Override
                                        public void stopped() {
                                            Charting.newChart(chart);
                                            updateUI = new UpdateUI(MoreAboutDownloadActivity.this, null, canWrite, gid, chart, rootView);
                                            new Thread(updateUI).start();
                                        }
                                    });
                                } else {
                                    Charting.newChart(chart);
                                    updateUI = new UpdateUI(MoreAboutDownloadActivity.this, null, canWrite, gid, chart, rootView);
                                    new Thread(updateUI).start();
                                }
                            }
                        });
                    }
                });

                MoreAboutDownloadActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog dialog = builder.create();
                        dialog.show();
                        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
                    }
                });
            }

            @Override
            public void onException(Exception exception) {
                progressDialog.dismiss();
                Utils.UIToast(MoreAboutDownloadActivity.this, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception.getMessage());
            }
        });
    }
}
