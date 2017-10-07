package com.gianlu.aria2app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.webkit.MimeTypeMap;

import com.gianlu.aria2app.Main.SharedFile;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Random;

public class Utils {
    public static final int CHART_DOWNLOAD_SET = 1;
    public static final int CHART_UPLOAD_SET = 0;
    private static final Random random = new Random();

    public static int indexOf(String[] items, String item) {
        for (int i = 0; i < items.length; i++)
            if (Objects.equals(items[i], item))
                return i;

        return -1;
    }

    public static void setupChart(LineChart chart, boolean isCardView, @ColorRes int textColor) {
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
        ya.setTextSize(isCardView ? 8 : 9);
        ya.setAxisMinimum(0);
        ya.setDrawAxisLine(false);
        ya.setLabelCount(isCardView ? 4 : 8, true);
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

    public static JSONArray readyParams(@NonNull Context context) {
        return readyParams(ProfilesManager.get(context).getCurrent(context).getProfile(context));
    }

    public static JSONArray readyParams(@NonNull MultiProfile.UserProfile profile) {
        JSONArray array = new JSONArray();
        if (profile.authMethod == JTA2.AuthMethod.TOKEN) array.put("token:" + profile.serverToken);
        return array;
    }

    public static void requestWritePermission(final Activity activity, final int code) {
        if (activity == null) return;
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
            } else {
                ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, code);
            }
        }
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
        return new JSONObject().put("jsonrpc", "2.0").put("id", random.nextInt());
    }

    @Nullable
    public static String resolveUri(Context context, Uri uri) {
        if (uri == null) return null;

        if (uri.getScheme().equalsIgnoreCase("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, new String[]{"_data"}, null, null, null)) {
                if (cursor != null) {
                    int column_index = cursor.getColumnIndexOrThrow("_data");
                    if (cursor.moveToFirst()) return cursor.getString(column_index);
                }
            } catch (Exception ex) {
                Logging.logMe(context, ex);
            }
        } else if (uri.getScheme().equalsIgnoreCase("file")) {
            return uri.getPath();
        }

        return null;
    }

    private static String getMimeType(String uri) {
        String ext = MimeTypeMap.getFileExtensionFromUrl(uri);
        if (ext == null || ext.isEmpty()) {
            String[] split = uri.split("\\.");
            ext = split[split.length - 1];
        }

        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
    }

    @Nullable
    public static SharedFile accessUriFile(Context context, Uri uri) {
        if (uri == null) return null;

        String resolved = resolveUri(context, uri);
        if (resolved != null) return new SharedFile(new File(resolved), getMimeType(resolved));

        if (Objects.equals(uri.getScheme(), "file")) {
            File file = new File(uri.getPath());
            return new SharedFile(file, getMimeType(file.getAbsolutePath()));
        }

        String name = Base64.encodeToString(uri.toString().getBytes(), Base64.NO_WRAP);
        if (name.length() > 64) name = name.substring(name.length() - 64);
        String mime = context.getContentResolver().getType(uri);
        File file;
        if (mime == null) {
            file = new File(context.getCacheDir(), name);
        } else {
            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
            file = new File(context.getCacheDir(), name + (ext == null ? "" : ("." + ext)));
        }

        try (InputStream in = context.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(file, false)) {
            CommonUtils.copy(in, out);
            return new SharedFile(file, mime);
        } catch (IOException ex) {
            Logging.logMe(context, ex);
            return null;
        }
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
        public static final Toaster.Message FAILED_REFRESHING = new Toaster.Message(R.string.failedRefreshing, true);
        public static final Toaster.Message GLOBAL_OPTIONS_CHANGED = new Toaster.Message(R.string.globalOptionsChanged, false);
        public static final Toaster.Message ONLY_ONE_TORRENT = new Toaster.Message(R.string.onlyOneTorrentUri, false);
        public static final Toaster.Message NO_FILE_MANAGER = new Toaster.Message(R.string.noFilemanager, true);
        public static final Toaster.Message FILE_DESELECTED = new Toaster.Message(R.string.fileDeselected, false);
        public static final Toaster.Message FILE_SELECTED = new Toaster.Message(R.string.fileSelected, false);
        public static final Toaster.Message DIR_DESELECTED = new Toaster.Message(R.string.dirFilesDeselected, false);
        public static final Toaster.Message DIR_SELECTED = new Toaster.Message(R.string.dirFilesSelected, false);
        public static final Toaster.Message DOWNLOAD_STARTED = new Toaster.Message(R.string.downloadStarted, false);
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
    }

    private static class CustomYAxisValueFormatter implements IAxisValueFormatter {
        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            return CommonUtils.speedFormatter(value, false);
        }
    }
}