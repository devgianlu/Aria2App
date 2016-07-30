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
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ExpandableListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gianlu.aria2app.Google.Analytics;
import com.gianlu.aria2app.Main.IThread;
import com.gianlu.aria2app.MoreAboutDownload.UpdateUI;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.IOption;
import com.gianlu.aria2app.NetIO.JTA2.ISuccess;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.Options.LocalParser;
import com.gianlu.aria2app.Options.OptionAdapter;
import com.gianlu.aria2app.Options.OptionChild;
import com.gianlu.aria2app.Options.OptionHeader;
import com.github.mikephil.charting.charts.LineChart;
import com.google.android.gms.analytics.HitBuilders;

import org.json.JSONException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoreAboutDownloadActivity extends AppCompatActivity {
    private UpdateUI updateUI;
    private String gid;
    private Download.STATUS status;
    private ViewHolder holder;
    private boolean canWrite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more_about_download);

        View root = findViewById(android.R.id.content);
        assert root != null;
        holder = new ViewHolder(root.getRootView());

        gid = getIntent().getStringExtra("gid");
        status = Download.STATUS.valueOf(getIntent().getStringExtra("status"));
        setTitle(getIntent().getStringExtra("name"));

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

        // Charting.newChart(chart);
        updateUI = new UpdateUI(this, new UpdateUI.IFirstUpdate() {
            @Override
            public void onFirstUpdate(Download item) { /* NOT USED */}
        }, canWrite, gid, holder);
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
                UpdateUI.stop(updateUI, new IThread() {
                    @Override
                    public void stopped() {
                        updateUI = new UpdateUI(MoreAboutDownloadActivity.this, null, canWrite, gid, holder);
                        new Thread(updateUI).start();
                    }
                });
                break;
            case R.id.moreAboutDownloadMenu_options:
                showOptionsDialog();
                break;
            case android.R.id.home:
                onBackPressed();
                break;
            // TODO: Show peers
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        UpdateUI.stop(updateUI);
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
        UpdateUI.stop(updateUI);
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

        final JTA2 jta2;
        try {
            jta2 = Utils.readyJTA2(this);
        } catch (IOException | NoSuchAlgorithmException ex) {
            Utils.UIToast(this, Utils.TOAST_MESSAGES.WS_EXCEPTION, ex);
            return;
        }
        final ProgressDialog pd = Utils.fastProgressDialog(this, R.string.gathering_information, true, false);
        pd.show();

        jta2.getOption(gid, new IOption() {
            @Override
            public void onOptions(Map<String, String> options) {
                LocalParser localOptions;
                try {
                    localOptions = new LocalParser(MoreAboutDownloadActivity.this, false);
                } catch (IOException | JSONException ex) {
                    pd.dismiss();
                    Utils.UIToast(MoreAboutDownloadActivity.this, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex);
                    return;
                }

                for (String resOption : getResources().getStringArray(R.array.downloadOptions)) {
                    try {
                        OptionHeader header = new OptionHeader(resOption, localOptions.getCommandLine(resOption), options.get(resOption), false);
                        headers.add(header);

                        children.put(header, new OptionChild(
                                localOptions.getDefinition(resOption),
                                String.valueOf(localOptions.getDefaultValue(resOption)),
                                String.valueOf(options.get(resOption))));
                    } catch (JSONException ex) {
                        pd.dismiss();
                        Utils.UIToast(MoreAboutDownloadActivity.this, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex);
                    }
                }

                pd.dismiss();

                final AlertDialog.Builder builder = new AlertDialog.Builder(MoreAboutDownloadActivity.this);

                @SuppressLint("InflateParams") final View view = getLayoutInflater().inflate(R.layout.options_dialog, null);
                ExpandableListView listView = (ExpandableListView) view.findViewById(R.id.moreAboutDownload_dialog_expandableListView);
                listView.setAdapter(new OptionAdapter(MoreAboutDownloadActivity.this, headers, children));

                builder.setView(view)
                        .setTitle(R.string.options)
                        .setPositiveButton(R.string.change, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Map<String, String> map = new HashMap<>();

                                for (Map.Entry<OptionHeader, OptionChild> item : children.entrySet()) {
                                    if (!item.getValue().isChanged()) continue;
                                    map.put(item.getKey().getOptionName(), item.getValue().getValue());
                                }

                                if (map.entrySet().size() == 0) return;

                                pd.show();

                                if (Analytics.isTrackingAllowed(MoreAboutDownloadActivity.this))
                                    Analytics.getDefaultTracker(MoreAboutDownloadActivity.this.getApplication()).send(new HitBuilders.EventBuilder()
                                            .setCategory(Analytics.CATEGORY_USER_INPUT)
                                            .setAction(Analytics.ACTION_CHANGED_DOWNLOAD_OPTIONS)
                                            .build());

                                jta2.changeOption(gid, map, new ISuccess() {
                                    @Override
                                    public void onSuccess() {
                                        pd.dismiss();
                                        Utils.UIToast(MoreAboutDownloadActivity.this, Utils.TOAST_MESSAGES.DOWNLOAD_OPTIONS_CHANGED);

                                        MoreAboutDownloadActivity.this.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                UpdateUI.stop(updateUI, new IThread() {
                                                    @Override
                                                    public void stopped() {
                                                        updateUI = new UpdateUI(MoreAboutDownloadActivity.this, null, canWrite, gid, holder);
                                                        new Thread(updateUI).start();
                                                    }
                                                });
                                            }
                                        });
                                    }

                                    @Override
                                    public void onException(Exception exception) {
                                        pd.dismiss();
                                        Utils.UIToast(MoreAboutDownloadActivity.this, Utils.TOAST_MESSAGES.FAILED_CHANGE_OPTIONS, exception);
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
                                UpdateUI.stop(updateUI, new IThread() {
                                    @Override
                                    public void stopped() {
                                        updateUI = new UpdateUI(MoreAboutDownloadActivity.this, null, canWrite, gid, holder);
                                        new Thread(updateUI).start();
                                    }
                                });
                            }
                        });
                    }
                });

                MoreAboutDownloadActivity.this.runOnUiThread(new Runnable() {
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
                Utils.UIToast(MoreAboutDownloadActivity.this, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception);
            }
        });
    }

    public class ViewHolder {
        public LineChart chart;
        public TextView downloadSpeed;
        public TextView uploadSpeed;
        public TextView time;
        public TextView percentage;
        public TextView completedLength;
        public TextView totalLength;
        public TextView uploadedLength;
        public TextView piecesNumber;
        public TextView piecesLength;
        public TextView connections;
        public TextView gid;
        public TextView seedersNumber;
        public RelativeLayout bitTorrentContainer;
        public TextView bitTorrentMode;
        public TextView bitTorrentAnnounceList;
        public TextView infoHash;
        public TextView seeder;
        public TextView bitTorrentComment;
        public TextView bitTorrentCreationDate;
        public RelativeLayout treeNodeContainer;

        public ViewHolder(View rootView) {
            chart = (LineChart) rootView.findViewById(R.id.moreAboutDownload_chart);
            treeNodeContainer = (RelativeLayout) rootView.findViewById(R.id.moreAboutDownload_treeViewContainer);
            downloadSpeed = (TextView) rootView.findViewById(R.id.moreAboutDownload_downloadSpeed);
            uploadSpeed = (TextView) rootView.findViewById(R.id.moreAboutDownload_uploadSpeed);
            time = (TextView) rootView.findViewById(R.id.moreAboutDownload_time);
            percentage = (TextView) rootView.findViewById(R.id.moreAboutDownload_percentage);
            completedLength = (TextView) rootView.findViewById(R.id.moreAboutDownload_completedLength);
            totalLength = (TextView) rootView.findViewById(R.id.moreAboutDownload_totalLength);
            uploadedLength = (TextView) rootView.findViewById(R.id.moreAboutDownload_uploadedLength);
            piecesNumber = (TextView) rootView.findViewById(R.id.moreAboutDownload_piecesNumber);
            piecesLength = (TextView) rootView.findViewById(R.id.moreAboutDownload_piecesLength);
            connections = (TextView) rootView.findViewById(R.id.moreAboutDownload_connections);
            gid = (TextView) rootView.findViewById(R.id.moreAboutDownload_gid);
            seedersNumber = (TextView) rootView.findViewById(R.id.moreAboutDownload_bitTorrentSeedersNumber);
            bitTorrentContainer = (RelativeLayout) rootView.findViewById(R.id.moreAboutDownload_bitTorrentContainer);
            bitTorrentMode = (TextView) rootView.findViewById(R.id.moreAboutDownload_bitTorrentMode);
            bitTorrentAnnounceList = (TextView) rootView.findViewById(R.id.moreAboutDownload_bitTorrentAnnounceList);
            infoHash = (TextView) rootView.findViewById(R.id.moreAboutDownload_bitTorrentInfoHash);
            seeder = (TextView) rootView.findViewById(R.id.moreAboutDownload_bitTorrentSeeder);
            bitTorrentComment = (TextView) rootView.findViewById(R.id.moreAboutDownload_bitTorrentComment);
            bitTorrentCreationDate = (TextView) rootView.findViewById(R.id.moreAboutDownload_bitTorrentCreationDate);
        }
    }
}