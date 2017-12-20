package com.gianlu.aria2app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Toaster;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class Utils {
    public static final int CHART_DOWNLOAD_SET = 1;
    public static final int CHART_UPLOAD_SET = 0;
    public static final String ACTION_DOWNLOAD_FILE = "dd_download_file";
    public static final String ACTION_NEW_PROFILE = "new_profile";
    public static final String ACTION_DELETE_PROFILE = "deleted_profile";
    public static final String ACTION_CHANGED_GLOBAL_OPTIONS = "changed_global_options";
    public static final String ACTION_CHANGED_DOWNLOAD_OPTIONS = "changed_download_options";
    public static final String ACTION_NEW_TORRENT = "new_torrent_download";
    public static final String ACTION_NEW_METALINK = "new_metalink_download";
    public static final String ACTION_NEW_URI = "new_uri_download";
    public static final String ACTION_SEARCH_DOWNLOAD = "new_uri_from_search";
    public static final String ACTION_SHARE = "received_share";
    public static final String ACTION_DOWNLOAD_DIRECTORY = "dd_download_directory";
    public static final String ACTION_SEARCH = "search_torrent";
    public static final String ACTION_STARTED_TEST = "run_profile_test";
    public static final String ACTION_SEARCH_GET_TORRENT = "new_torrent_from_search";
    public static final String ACTION_SEARCH_GET_MAGNET = "new_magnet_from_search";
    public static final String ACTION_SHORTCUT = "used_shortcut";

    public static int indexOf(String[] items, String item) {
        for (int i = 0; i < items.length; i++)
            if (Objects.equals(items[i], item))
                return i;

        return -1;
    }

    public static void setupChart(LineChart chart, boolean small, @ColorRes int textColor) {
        chart.clear();

        chart.setDescription(null);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.alpha(0));
        chart.setTouchEnabled(false);
        Legend legend = chart.getLegend();
        legend.setTextColor(ContextCompat.getColor(chart.getContext(), textColor));
        legend.setEnabled(true);

        LineData data = new LineData();
        data.setValueTextColor(ContextCompat.getColor(chart.getContext(), textColor));
        chart.setData(data);

        YAxis ya = chart.getAxisLeft();
        ya.setAxisLineColor(ContextCompat.getColor(chart.getContext(), textColor));
        ya.setTextColor(ContextCompat.getColor(chart.getContext(), textColor));
        ya.setTextSize(small ? 8 : 9);
        ya.setAxisMinimum(0);
        ya.setDrawAxisLine(false);
        ya.setLabelCount(small ? 4 : 8, true);
        ya.setEnabled(true);
        ya.setDrawGridLines(true);
        ya.setValueFormatter(new CustomYAxisValueFormatter());

        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setEnabled(false);

        data.addDataSet(initUploadSet(chart.getContext()));
        data.addDataSet(initDownloadSet(chart.getContext()));

        chart.invalidate();
    }

    private static LineDataSet initDownloadSet(Context context) {
        LineDataSet set = new LineDataSet(null, context.getString(R.string.downloadSpeed));
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(2f);
        set.setColor(ContextCompat.getColor(context, R.color.downloadColor));
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawFilled(false);
        return set;
    }

    private static LineDataSet initUploadSet(Context context) {
        LineDataSet set = new LineDataSet(null, context.getString(R.string.uploadSpeed));
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(2f);
        set.setColor(ContextCompat.getColor(context, R.color.uploadColor));
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawFilled(false);
        return set;
    }

    public static JSONArray readyParams(@NonNull Context context) throws ProfilesManager.NoCurrentProfileException {
        return readyParams(ProfilesManager.get(context).getCurrent(context).getProfile(context));
    }

    public static JSONArray readyParams(@NonNull MultiProfile.UserProfile profile) {
        JSONArray array = new JSONArray();
        if (profile.authMethod == JTA2.AuthMethod.TOKEN) array.put("token:" + profile.serverToken);
        return array;
    }

    public static boolean requestWritePermission(final Activity activity, final int code) {
        if (activity == null) return false;
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                CommonUtils.showDialog(activity, new AlertDialog.Builder(activity)
                        .setTitle(R.string.writeExternalStorageRequest_title)
                        .setMessage(R.string.writeExternalStorageRequest_message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, code);
                            }
                        }));

                return false;
            } else {
                ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, code);
                return false;
            }
        }

        return true;
    }

    public static void requestReadPermission(final Activity activity, @StringRes int message, final int code) {
        if (activity == null) return;
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                CommonUtils.showDialog(activity, new AlertDialog.Builder(activity)
                        .setTitle(R.string.readExternalStorageRequest_title)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, code);
                            }
                        }));
            } else {
                ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, code);
            }
        }
    }

    public static JSONObject readyRequest() throws JSONException {
        return new JSONObject().put("jsonrpc", "2.0").put("id", ThreadLocalRandom.current().nextInt());
    }

    public static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (byte b : bytes) {
            if (first && b == 0) continue;
            if (!first) sb.append(":");
            sb.append(String.format("%02X", b));
            first = false;
        }
        return sb.toString();
    }

    @SuppressWarnings("WeakerAccess")
    public static class Messages {
        public static final Toaster.Message FAILED_GATHERING_INFORMATION = new Toaster.Message(R.string.failedGatheringInfo, true);
        public static final Toaster.Message FAILED_DOWNLOAD_FILE = new Toaster.Message(R.string.failedDownloadingFile, true);
        public static final Toaster.Message DOWNLOAD_ADDED = new Toaster.Message(R.string.downloadAdded, false);
        public static final Toaster.Message SESSION_SAVED = new Toaster.Message(R.string.sessionSaved, false);
        public static final Toaster.Message FAILED_SAVE_SESSION = new Toaster.Message(R.string.failedSavingSession, true);
        public static final Toaster.Message NO_URIS = new Toaster.Message(R.string.atLeastOneUri, false);
        public static final Toaster.Message FAILED_ADD_DOWNLOAD = new Toaster.Message(R.string.failedAddingDownload, true);
        public static final Toaster.Message DOWNLOAD_OPTIONS_CHANGED = new Toaster.Message(R.string.downloadOptionsChanged, false);
        public static final Toaster.Message FAILED_CHANGE_FILE_SELECTION = new Toaster.Message(R.string.failedFileChangeSelection, true);
        public static final Toaster.Message NO_QUICK_OPTIONS = new Toaster.Message(R.string.noQuickOptions, false);
        public static final Toaster.Message INVALID_DOWNLOAD_PATH = new Toaster.Message(R.string.invalidDownloadPath, false);
        public static final Toaster.Message INVALID_FILE = new Toaster.Message(R.string.invalidFile, false);
        public static final Toaster.Message FAILED_CONNECTING = new Toaster.Message(R.string.failedConnecting, true);
        public static final Toaster.Message FAILED_LOADING = new Toaster.Message(R.string.failedLoading, true);
        public static final Toaster.Message CANNOT_SAVE_PROFILE = new Toaster.Message(R.string.cannotSaveProfile, true);
        public static final Toaster.Message FAILED_PERFORMING_ACTION = new Toaster.Message(R.string.failedAction, true);
        public static final Toaster.Message PAUSED = new Toaster.Message(R.string.downloadPaused, false);
        public static final Toaster.Message RESTARTED = new Toaster.Message(R.string.downloadRestarted, false);
        public static final Toaster.Message RESUMED = new Toaster.Message(R.string.downloadResumed, false);
        public static final Toaster.Message MOVED = new Toaster.Message(R.string.downloadMoved, false);
        public static final Toaster.Message REMOVED = new Toaster.Message(R.string.downloadRemoved, false);
        public static final Toaster.Message RESULT_REMOVED = new Toaster.Message(R.string.downloadResultRemoved, false);
        public static final Toaster.Message GLOBAL_OPTIONS_CHANGED = new Toaster.Message(R.string.globalOptionsChanged, false);
        public static final Toaster.Message ONLY_ONE_TORRENT = new Toaster.Message(R.string.onlyOneTorrentUri, false);
        public static final Toaster.Message NO_FILE_MANAGER = new Toaster.Message(R.string.noFilemanager, true);
        public static final Toaster.Message FILE_DESELECTED = new Toaster.Message(R.string.fileDeselected, false);
        public static final Toaster.Message FILE_SELECTED = new Toaster.Message(R.string.fileSelected, false);
        public static final Toaster.Message DIR_DESELECTED = new Toaster.Message(R.string.dirFilesDeselected, false);
        public static final Toaster.Message DIR_SELECTED = new Toaster.Message(R.string.dirFilesSelected, false);
        public static final Toaster.Message CANT_DESELECT_ALL_FILES = new Toaster.Message(R.string.cannotDeselectAllFiles, false);
        public static final Toaster.Message FAILED_DOWNLOAD_DIR = new Toaster.Message(R.string.failedDownloadingDir, true);
        public static final Toaster.Message DUPLICATED_CONDITION = new Toaster.Message(R.string.duplicatedCondition, false);
        public static final Toaster.Message HAS_ALWAYS_CONDITION = new Toaster.Message(R.string.hasAlwaysCondition, false);
        public static final Toaster.Message CANNOT_ADD_ALWAYS = new Toaster.Message(R.string.cannotAddAlwaysCondition, false);
        public static final Toaster.Message FAILED_CHANGE_OPTIONS = new Toaster.Message(R.string.failedChangingOptions, true);
        public static final Toaster.Message CONNECTIVITY_CHANGED = new Toaster.Message(R.string.connectivityChanged, false);
        public static final Toaster.Message NO_ENGINES_SELECTED = new Toaster.Message(R.string.noEnginesSelected, false);
        public static final Toaster.Message INVALID_MAX_SIMULTANEOUS_DOWNLOADS = new Toaster.Message(R.string.invalidMaxSimultaneousDownloads, false);
        public static final Toaster.Message FAILED_OPENING_DOWNLOAD = new Toaster.Message(R.string.failedOpeningDownload, true);
        public static final Toaster.Message DD_NOT_ENABLED = new Toaster.Message(R.string.ddNotEnabled, false);
        public static final Toaster.Message PROFILE_DOES_NOT_EXIST = new Toaster.Message(R.string.profileDoesntExist, false);
        public static final Toaster.Message FAILED_LOADING_CERTIFICATE = new Toaster.Message(R.string.invalidCertificate, true);
        public static final Toaster.Message PAUSED_ALL = new Toaster.Message(R.string.pausedAll, false);
        public static final Toaster.Message RESUMED_ALL = new Toaster.Message(R.string.resumedAll, false);
        public static final Toaster.Message PURGED_DOWNLOAD_RESULT = new Toaster.Message(R.string.purgedDownloadResult, false);
        public static final Toaster.Message EXPORT_OPTIONS_GRANT_WRITE = new Toaster.Message(R.string.exportOptionsGrantWrite, false);
        public static final Toaster.Message FAILED_EXPORTING_OPTIONS = new Toaster.Message(R.string.failedExportingOptions, true);
    }

    private static class CustomYAxisValueFormatter implements IAxisValueFormatter {
        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            return CommonUtils.speedFormatter(value, false);
        }
    }
}