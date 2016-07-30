package com.gianlu.aria2app.MoreAboutDownload;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.gianlu.aria2app.DownloadsListing.Charting;
import com.gianlu.aria2app.FileListing.CustomItemHolder;
import com.gianlu.aria2app.FileListing.CustomTreeItem;
import com.gianlu.aria2app.FileListing.Directory;
import com.gianlu.aria2app.FileListing.FileNode;
import com.gianlu.aria2app.FileListing.FilesTree;
import com.gianlu.aria2app.Google.Analytics;
import com.gianlu.aria2app.Google.UncaughtExceptionHandler;
import com.gianlu.aria2app.Main.IThread;
import com.gianlu.aria2app.MoreAboutDownloadActivity;
import com.gianlu.aria2app.NetIO.JTA2.BitTorrent;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.File;
import com.gianlu.aria2app.NetIO.JTA2.IDownload;
import com.gianlu.aria2app.NetIO.JTA2.IOption;
import com.gianlu.aria2app.NetIO.JTA2.ISuccess;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.gms.analytics.HitBuilders;
import com.unnamed.b.atv.model.TreeNode;
import com.unnamed.b.atv.view.AndroidTreeView;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UpdateUI implements Runnable {
    private String directDownloadAddr;
    private Activity context;
    private TreeNode root;
    private boolean enableAnimations;
    private JTA2 jta2;
    private AndroidTreeView treeView;
    private String gid;


    private Resources res;
    private boolean directDownloadEnabled;
    private int updateTreeViewCount = 1;
    private boolean _shouldStop;
    private boolean _stopped;
    private int updateRate;
    private IFirstUpdate handler;
    private boolean canWrite;
    private MoreAboutDownloadActivity.ViewHolder viewHolder;
    private boolean firstRun = true;


    public UpdateUI(Activity context, IFirstUpdate handler, boolean canWrite, String gid, MoreAboutDownloadActivity.ViewHolder viewHolder) {
        this.handler = handler;
        this.canWrite = canWrite;
        this.viewHolder = viewHolder;
        _shouldStop = false;
        _stopped = false;
        this.context = context;
        this.gid = gid;

        viewHolder.chart = Charting.setupChart(viewHolder.chart);

        //sharedPreferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        updateRate = Integer.parseInt(sharedPreferences.getString("a2_updateRate", "2")) * 1000;
        enableAnimations = sharedPreferences.getBoolean("a2_enableAnimations", false);
        directDownloadEnabled = sharedPreferences.getBoolean("a2_directDownload", false);
        directDownloadAddr = sharedPreferences.getString("dd_addr", "http://127.0.0.1/");

        //jta2
        try {
            jta2 = Utils.readyJTA2(context);
        } catch (IOException | NoSuchAlgorithmException ex) {
            Utils.UIToast(context, Utils.TOAST_MESSAGES.WS_EXCEPTION, ex);
            stop();
        }
        res = context.getResources();
    }

    public static void stop(UpdateUI updateUI) {
        if (updateUI != null) updateUI.stop();
    }

    public static void stop(UpdateUI updateUI, IThread handler) {
        if (updateUI == null)
            handler.stopped();
        else
            updateUI.stop(handler);
    }

    public void stop() {
        _shouldStop = true;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    public void stop(IThread handler) {
        _shouldStop = true;
        while (!_stopped) ;
        handler.stopped();
    }

    @Override
    public void run() {
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(context));

        while ((!_shouldStop) && jta2 != null) {
            jta2.tellStatus(gid, new IDownload() {
                @Override
                public void onDownload(final Download download) {
                    final String timeRemaining;
                    final String noDataTextChart;

                    switch (download.status) {
                        case ACTIVE:
                            if (download.downloadSpeed == 0) {
                                timeRemaining = Utils.TimeFormatter(0L);
                            } else {
                                timeRemaining = Utils.TimeFormatter((download.length - download.completedLength) / download.downloadSpeed);
                            }
                            noDataTextChart = context.getString(R.string.downloadStatus_waiting);
                            break;
                        case WAITING:
                            timeRemaining = context.getString(R.string.downloadStatus_waiting);
                            noDataTextChart = timeRemaining;
                            break;
                        case ERROR:
                            timeRemaining = context.getString(R.string.downloadStatus_error);
                            noDataTextChart = timeRemaining;
                            break;
                        case PAUSED:
                            timeRemaining = context.getString(R.string.downloadStatus_paused);
                            noDataTextChart = timeRemaining;
                            break;
                        case COMPLETE:
                            timeRemaining = context.getString(R.string.downloadStatus_complete);
                            noDataTextChart = timeRemaining;
                            break;
                        case REMOVED:
                            timeRemaining = context.getString(R.string.downloadStatus_removed);
                            noDataTextChart = timeRemaining;
                            break;
                        default:
                            timeRemaining = context.getString(R.string.downloadStatus_unknown);
                            noDataTextChart = timeRemaining;
                            break;
                    }

                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            viewHolder.downloadSpeed.setText(Utils.SpeedFormatter(download.downloadSpeed));
                            viewHolder.uploadSpeed.setText(Utils.SpeedFormatter(download.uploadSpeed));
                            viewHolder.time.setText(timeRemaining);
                            viewHolder.percentage.setText(download.getPercentage());
                            viewHolder.completedLength.setText(String.format(Locale.getDefault(), res.getString(R.string.completed_length), Utils.DimensionFormatter(download.completedLength)));
                            viewHolder.totalLength.setText(String.format(Locale.getDefault(), res.getString(R.string.total_length), Utils.DimensionFormatter(download.length)));
                            viewHolder.uploadedLength.setText(String.format(Locale.getDefault(), res.getString(R.string.uploaded_length), Utils.DimensionFormatter(download.uploadedLength)));
                            viewHolder.piecesNumber.setText(String.format(Locale.getDefault(), res.getString(R.string.pieces), download.numPieces));
                            viewHolder.piecesLength.setText(String.format(Locale.getDefault(), res.getString(R.string.pieces_length), Utils.DimensionFormatter(download.pieceLength)));
                            viewHolder.connections.setText(String.format(Locale.getDefault(), res.getString(R.string.connections), download.connections));
                            viewHolder.gid.setText(String.format(Locale.getDefault(), res.getString(R.string.gid), download.GID));


                            if (download.isBitTorrent) {
                                viewHolder.seedersNumber.setText(String.format(Locale.getDefault(), res.getString(R.string.numSeeder), download.numSeeders));
                                viewHolder.infoHash.setText(String.format(Locale.getDefault(), res.getString(R.string.info_hash), download.infoHash));
                                viewHolder.seeder.setText(String.format(Locale.getDefault(), res.getString(R.string.seeder), String.valueOf(download.seeder)));
                                viewHolder.bitTorrentCreationDate.setText(String.format(Locale.getDefault(), res.getString(R.string.creation_date), download.bitTorrent.creationDate));
                                viewHolder.bitTorrentComment.setText(String.format(Locale.getDefault(), res.getString(R.string.comment), download.bitTorrent.creationDate));
                                viewHolder.bitTorrentMode.setText(String.format(Locale.getDefault(), res.getString(R.string.mode), download.bitTorrent.mode.equals(BitTorrent.MODE.SINGLE) ? "single" : "multi"));
                                viewHolder.bitTorrentAnnounceList.setText(loadBTAnnounceList(download.bitTorrent));
                            } else {
                                viewHolder.bitTorrentContainer.removeAllViews();
                            }
                        }
                    });


                    // Chart
                    viewHolder.chart.setNoDataText(noDataTextChart);
                    if (download.status.equals(Download.STATUS.ACTIVE)) {
                        final LineData data = viewHolder.chart.getData();
                        ILineDataSet downloadSet = data.getDataSetByIndex(0);
                        ILineDataSet uploadSet = data.getDataSetByIndex(1);

                        data.addXValue(new SimpleDateFormat("hh:mm:ss", Locale.getDefault()).format(new java.util.Date()));
                        data.addEntry(new Entry(download.downloadSpeed, downloadSet.getEntryCount()), 0);
                        data.addEntry(new Entry(download.uploadSpeed, uploadSet.getEntryCount()), 1);

                        viewHolder.chart.notifyDataSetChanged();
                        viewHolder.chart.setVisibleXRangeMaximum(90);
                        viewHolder.chart.moveViewToX(data.getXValCount() - 91);
                    } else {
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                viewHolder.chart.clear();
                            }
                        });
                    }

                    if (updateTreeViewCount == 1) {
                        FilesTree tree = new FilesTree(new FileNode("", "", null));

                        for (File file : download.files) {
                            tree.addElement(file);
                        }

                        root = tree.toTreeNode();

                        String state = null;
                        if (treeView != null) state = treeView.getSaveState();
                        treeView = loadTreeNode(download);
                        if (state != null) treeView.restoreState(state);

                        final View treeRealView = treeView.getView();
                        if (treeRealView != null) {
                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    viewHolder.treeNodeContainer.removeAllViews();
                                    viewHolder.treeNodeContainer.addView(treeRealView);
                                }
                            });
                        }

                        updateTreeViewCount = 6;
                    } else {
                        updateTreeViewCount -= 1;
                    }

                    if (firstRun && handler != null) {
                        handler.onFirstUpdate(download);
                        firstRun = false;
                    }
                }

                @Override
                public void onException(Exception exception) {
                    Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception);
                    _shouldStop = true;
                }
            });

            try {
                Thread.sleep(updateRate);
            } catch (InterruptedException ex) {
                _shouldStop = true;
                break;
            }
        }

        _stopped = true;
    }

    private AndroidTreeView loadTreeNode(final Download download) {
        AndroidTreeView tView = new AndroidTreeView(context, root);
        tView.setDefaultViewHolder(CustomItemHolder.class);
        tView.setDefaultContainerStyle(R.style.TreeNodeStyleCustom);
        tView.setDefaultAnimation(enableAnimations);
        tView.setDefaultNodeClickListener(new TreeNode.TreeNodeClickListener() {
            @Override
            public void onClick(TreeNode node, Object value) {
                CustomTreeItem item = (CustomTreeItem) value;
                if (item.type.equals(CustomTreeItem.TYPE.FILE))
                    createFileDialog((File) item.file, download);
            }
        });
        tView.setDefaultNodeLongClickListener(new TreeNode.TreeNodeLongClickListener() {
            @Override
            public boolean onLongClick(TreeNode node, Object value) {
                CustomTreeItem item = (CustomTreeItem) value;
                if (item.type.equals(CustomTreeItem.TYPE.FOLDER))
                    createDirectoryDialog((Directory) item.file, download);
                return true;
            }
        });
        return tView;
    }

    private void createFileDialog(final File file, final Download download) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);

        @SuppressLint("InflateParams") View main = context.getLayoutInflater().inflate(R.layout.filelisting_dialog, null);
        TextView totalLength = (TextView) main.findViewById(R.id.fileListing_customDialog_fileLength);
        final TextView completedLength = (TextView) main.findViewById(R.id.fileListing_customDialog_completedLength);
        TextView fileProgress = (TextView) main.findViewById(R.id.fileListing_customDialog_filePercentage);
        final TextView fileIndex = (TextView) main.findViewById(R.id.fileListing_customDialog_fileIndex);
        TextView fileUris = (TextView) main.findViewById(R.id.fileListing_customDialog_fileUris);
        CheckBox fileSelected = (CheckBox) main.findViewById(R.id.fileListing_customDialog_selected);

        totalLength.setText(String.format(Locale.getDefault(), res.getString(R.string.total_length), Utils.DimensionFormatter(file.length)));
        completedLength.setText(String.format(Locale.getDefault(), res.getString(R.string.completed_length), Utils.DimensionFormatter(file.completedLength)));
        fileProgress.setText(file.getPercentage());
        fileIndex.setText(String.format(Locale.getDefault(), res.getString(R.string.index), file.index));

        if (download.status.equals(Download.STATUS.ACTIVE)) {
            fileSelected.setText(R.string.selectFileNotPaused);
            fileSelected.setEnabled(false);
        } else if (!download.isBitTorrent) {
            fileSelected.setText(R.string.selectFileNotTorrent);
            fileSelected.setEnabled(false);
        } else if (download.files.size() == 1) {
            fileSelected.setText(R.string.selectFileSingleFile);
            fileSelected.setEnabled(false);
        } else {
            fileSelected.setText(R.string.selectFile);
            fileSelected.setChecked(file.selected);
        }

        fileSelected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, final boolean b) {
                final List<Integer> indexes = new ArrayList<>();
                jta2.getOption(gid, new IOption() {
                    @Override
                    public void onOptions(Map<String, String> options) {
                        if (!(options.get("select-file") == null || options.get("select-file").isEmpty())) {
                            String[] indexesString = options.get("select-file").split(",");
                            for (String indexString : indexesString) {
                                indexes.add(Integer.parseInt(indexString));
                            }
                        } else {
                            for (File filee : download.files) {
                                indexes.add(filee.index);
                            }
                        }

                        if (b) {
                            indexes.add(file.index);
                        } else {
                            indexes.remove(file.index);
                        }

                        String newerValue = "";
                        for (Integer index : indexes) {
                            if (newerValue.isEmpty()) {
                                newerValue += index;
                            } else {
                                newerValue += "," + index;
                            }
                        }
                        Map<String, String> newerOptions = new HashMap<>();
                        newerOptions.put("select-file", newerValue);

                        jta2.changeOption(gid, newerOptions, new ISuccess() {
                            @Override
                            public void onSuccess() {
                                Utils.UIToast(context, (b ? Utils.TOAST_MESSAGES.FILE_INCLUDED : Utils.TOAST_MESSAGES.FILE_EXCLUDED), "Index: " + file.index);
                            }

                            @Override
                            public void onException(Exception exception) {
                                Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_INCEXCFILE, exception);
                            }
                        });
                    }

                    @Override
                    public void onException(Exception exception) {
                        Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception);
                    }
                });
            }
        });

        String uris = "Uri(s): ";
        for (Map.Entry<File.URI_STATUS, String> uri : file.uris.entrySet()) {
            uris += "\n    " + uri.getValue() + " (" + (uri.getKey().equals(File.URI_STATUS.USED) ? "used" : "waiting") + ")";
        }
        fileUris.setText(uris);
        if (uris.equals("Uri(s): ")) fileUris.setVisibility(View.INVISIBLE);

        builder.setView(main)
                .setTitle(file.path);

        if (directDownloadEnabled && (!download.getName().startsWith("[METADATA]"))) {
            builder.setPositiveButton(R.string.downloadFile, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (!canWrite) {
                        Utils.UIToast(context, Utils.TOAST_MESSAGES.NO_WRITE_PERMISSION);
                        return;
                    }

                    if (DownloadFile.isRunning) {
                        Utils.UIToast(context, Utils.TOAST_MESSAGES.ANOTHER_DOWNLOAD_RUNNING);
                        return;
                    }

                    URI uri;
                    try {
                        URI addr = new URI(directDownloadAddr);
                        uri = new URI(addr.getScheme(), null, addr.getHost(), addr.getPort(), file.getRelativePath(download.dir), null, null);
                    } catch (URISyntaxException ex) {
                        Utils.UIToast(context, Utils.TOAST_MESSAGES.CANNOT_START_DOWNLOAD, ex);
                        return;
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(file.getName())
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    DownloadFile._shouldStop = true;
                                }
                            })
                            .setPositiveButton(R.string.background, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            });
                            /* TODO: Pause/Resume download
                            .setNeutralButton(R.string.pause, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    DownloadFile._shouldPause = true;
                                }
                            })
                            */
                    DownloadProgressDialogHelper.setContext(context);
                    final DownloadProgressDialogHelper pdh = new DownloadProgressDialogHelper(builder.create());
                    pdh.setFileURL(uri.toASCIIString());
                    pdh.show();

                    DownloadFile.setContext(context);
                    DownloadFile.setHandler(new DownloadFile.IDownloading() {
                        @Override
                        public void onStart(Long fileLength) {
                            pdh.setFileLength(fileLength);

                            if (Analytics.isTrackingAllowed(context))
                                Analytics.getDefaultTracker(context.getApplication()).send(new HitBuilders.EventBuilder()
                                        .setCategory(Analytics.CATEGORY_USER_INPUT)
                                        .setAction(Analytics.ACTION_DOWNLOAD_FILE)
                                        .build());
                        }

                        @Override
                        public void onComplete() {
                            pdh.dismiss();
                            Utils.UIToast(context, Utils.TOAST_MESSAGES.FILE_DOWNLOAD_COMPLETED);
                        }

                        @Override
                        public void onUserStopped(boolean fileDeleted) {
                            pdh.dismiss();
                            Utils.UIToast(context, Utils.TOAST_MESSAGES.FILE_DOWNLOAD_USER_STOPPED, "File deleted: " + fileDeleted);
                        }

                        @Override
                        public void publishProgress(long downloaded) {
                            pdh.setProgress(downloaded);
                        }

                        @Override
                        public void onException(Exception ex) {
                            pdh.dismiss();
                            Utils.UIToast(context, Utils.TOAST_MESSAGES.FILE_DOWNLOAD_FAILED, ex);
                        }

                        @Override
                        public void onConnectionError(int respCode, String respMessage) {
                            pdh.dismiss();
                            Utils.UIToast(context, Utils.TOAST_MESSAGES.FILE_DOWNLOAD_FAILED, respCode + ": " + respMessage);
                        }
                    });

                    // TODO: Working really bad :((
                    final Intent downloadIntent = new Intent(context, DownloadFile.class)
                            .putExtra("gid", download.GID)
                            .putExtra("url", uri.toASCIIString())
                            .putExtra("fileName", file.getName());
                    if (download.completedLength.equals(download.length)) {
                        context.startService(downloadIntent);
                    } else {
                        AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
                        builder1.setMessage(R.string.downloadNotComplete)
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        context.startService(downloadIntent);
                                    }
                                })
                                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                    }
                                })
                                .create().show();
                    }
                }
            });
        }

        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                builder.create().show();
            }
        });
    }

    private void createDirectoryDialog(final Directory directory, final Download download) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = context.getLayoutInflater();
        @SuppressLint("InflateParams") View main = inflater.inflate(R.layout.filelisting_dialog, null);

        TextView totalLength = (TextView) main.findViewById(R.id.fileListing_customDialog_fileLength);
        TextView completedLength = (TextView) main.findViewById(R.id.fileListing_customDialog_completedLength);
        TextView directoryProgress = (TextView) main.findViewById(R.id.fileListing_customDialog_filePercentage);
        main.findViewById(R.id.fileListing_customDialog_fileIndex).setVisibility(View.INVISIBLE);
        main.findViewById(R.id.fileListing_customDialog_fileUris).setVisibility(View.INVISIBLE);
        CheckBox filesSelected = (CheckBox) main.findViewById(R.id.fileListing_customDialog_selected);

        totalLength.setText(String.format(Locale.getDefault(), res.getString(R.string.total_length), Utils.DimensionFormatter(directory.getTotalLength())));
        completedLength.setText(String.format(Locale.getDefault(), res.getString(R.string.completed_length), Utils.DimensionFormatter(directory.getCompletedLength())));
        directoryProgress.setText(directory.getPercentage());

        if (download.status.equals(Download.STATUS.ACTIVE)) {
            filesSelected.setText(R.string.selectDirectoryNotPaused);
            filesSelected.setEnabled(false);
        } else if (!download.isBitTorrent) {
            filesSelected.setText(R.string.selectDirectoryNotTorrent);
            filesSelected.setEnabled(false);
        } else if (download.files.size() == 1) {
            filesSelected.setText(R.string.selectDirectorySingleFile);
            filesSelected.setEnabled(false);
        } else {
            filesSelected.setText(R.string.selectDirectory);
            filesSelected.setChecked(directory.areAllSelected());
        }


        filesSelected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, final boolean b) {
                final List<Integer> indexes = new ArrayList<>();

                jta2.getOption(gid, new IOption() {
                    @Override
                    public void onOptions(Map<String, String> options) {
                        if (!(options.get("select-file") == null || options.get("select-file").isEmpty())) {
                            String[] indexesString = options.get("select-file").split(",");
                            for (String indexString : indexesString) {
                                indexes.add(Integer.parseInt(indexString));
                            }
                        } else {
                            for (File filee : download.files) {
                                indexes.add(filee.index);
                            }
                        }

                        if (b) {
                            indexes.addAll(directory.getSubIndexes());
                        } else {
                            indexes.removeAll(directory.getSubIndexes());
                        }

                        String newerValue = "";
                        for (Integer index : indexes) {
                            if (newerValue.isEmpty()) {
                                newerValue += index;
                            } else {
                                newerValue += "," + index;
                            }
                        }
                        Map<String, String> newerOptions = new HashMap<>();
                        newerOptions.put("select-file", newerValue);

                        jta2.changeOption(gid, newerOptions, new ISuccess() {
                            @Override
                            public void onSuccess() {
                                Utils.UIToast(context, (b ? Utils.TOAST_MESSAGES.FILES_INCLUDED : Utils.TOAST_MESSAGES.FILES_EXCLUDED), "Folder: " + directory.name);
                            }

                            @Override
                            public void onException(Exception exception) {
                                Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_INCEXCFILES, exception);
                            }
                        });
                    }

                    @Override
                    public void onException(Exception exception) {
                        Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception);
                    }
                });
            }
        });

        builder.setView(main);
        builder.setTitle(directory.name);

        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                builder.create().show();
            }
        });
    }

    private String loadBTAnnounceList(BitTorrent bitTorrent) {
        String announces = "";
        for (String announce : bitTorrent.announceList) {
            announces += "\n    " + announce;
        }

        return announces;
    }

    public interface IFirstUpdate {
        void onFirstUpdate(Download download);
    }
}
