package com.gianlu.aria2app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Base64;

import com.gianlu.aria2app.NetIO.JTA2.AFile;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.ProfilesManager.UserProfile;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import java.util.Random;

public class Utils {
    public static final int CHART_DOWNLOAD_SET = 1;
    public static final int CHART_UPLOAD_SET = 0;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    static void renameOldProfiles(Context context) {
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("oldProfiles", true))
            return;

        for (File file : context.getFilesDir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.toLowerCase().endsWith(".profile");
            }
        })) {
            if (!file.renameTo(new File(file.getParent(), new String(Base64.encode(file.getName().trim().replace(".profile", "").getBytes(), Base64.NO_WRAP)) + ".profile"))) {
                file.delete();
            }
        }

        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("oldProfiles", false).apply();
    }

    public static int indexOf(String[] items, String item) {
        for (int i = 0; i < items.length; i++)
            if (Objects.equals(items[i], item))
                return i;

        return -1;
    }

    public static void setupChart(LineChart chart, boolean isCardView) {
        chart.clear();

        chart.setDescription(null);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.alpha(0));
        chart.setTouchEnabled(false);
        chart.getLegend().setEnabled(true);

        LineData data = new LineData();
        data.setValueTextColor(ContextCompat.getColor(chart.getContext(), R.color.colorPrimaryDark));
        chart.setData(data);

        YAxis ya = chart.getAxisLeft();
        ya.setAxisLineColor(ContextCompat.getColor(chart.getContext(), R.color.colorPrimaryDark));
        ya.setTextColor(ContextCompat.getColor(chart.getContext(), R.color.colorPrimaryDark));
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

    public static JSONArray readyParams(Context context) {
        JSONArray array = new JSONArray();
        UserProfile profile = ProfilesManager.get(context).getCurrentAssert();
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

    public static void requestReadPermission(final Activity activity, final int code) {
        if (activity == null) return;
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                CommonUtils.showDialog(activity, new AlertDialog.Builder(activity)
                        .setTitle(R.string.readExternalStorageRequest_title)
                        .setMessage(R.string.readExternalStorageRequest_message)
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
        return new JSONObject().put("jsonrpc", "2.0").put("id", String.valueOf(new Random().nextInt(9999)));
    }

    @Nullable
    public static URL createDownloadRemoteURL(Context context, String downloadDir, AFile file) {
        final UserProfile.DirectDownload dd = ProfilesManager.get(context).getCurrentAssert().directDownload;

        try {
            URL base = dd.getURLAddress();
            URI uri = new URI(base.getProtocol(), null, base.getHost(), base.getPort(), file.getRelativePath(downloadDir), null, null);
            return uri.toURL();
        } catch (MalformedURLException | URISyntaxException ex) {
            Logging.logMe(context, ex);
            return null;
        }
    }

    public static File createDownloadLocalPath(File parent, String filename) {
        File ioFile = new File(parent, filename);

        int c = 1;
        while (ioFile.exists()) {
            String[] split = filename.split("\\.");

            String newName;
            if (split.length == 1) {
                newName = filename + "." + c;
            } else {
                String ext = split[split.length - 1];
                newName = filename.substring(0, filename.length() - ext.length() - 1) + "." + c + "." + ext;
            }

            ioFile = new File(parent, newName);
            c++;
        }

        return ioFile;
    }

    public static <T> int indexOf(T[] items, T item) {
        for (int i = 0; i < items.length; i++)
            if (items[i] == item)
                return i;

        return -1;
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
            } catch (Exception ignored) {
            }
        } else if (uri.getScheme().equalsIgnoreCase("file")) {
            return uri.getPath();
        }

        return null;
    }

    @SuppressWarnings("WeakerAccess")
    public static class ToastMessages {
        public static final CommonUtils.ToastMessage WS_EXCEPTION = new CommonUtils.ToastMessage("WebSocket exception!", true);
        public static final CommonUtils.ToastMessage FAILED_GATHERING_INFORMATION = new CommonUtils.ToastMessage("Failed on gathering information!", true);
        public static final CommonUtils.ToastMessage WRITE_STORAGE_DENIED = new CommonUtils.ToastMessage("Cannot download. You denied the write permission!", false);
        public static final CommonUtils.ToastMessage FAILED_DOWNLOAD_FILE = new CommonUtils.ToastMessage("Failed downloading file!", true);
        public static final CommonUtils.ToastMessage DOWNLOAD_ADDED = new CommonUtils.ToastMessage("Download added.", false);
        public static final CommonUtils.ToastMessage CHANGED_SELECTION = new CommonUtils.ToastMessage("File selected/deselected.", false);
        public static final CommonUtils.ToastMessage SESSION_SAVED = new CommonUtils.ToastMessage("Session saved correctly.", false);
        public static final CommonUtils.ToastMessage FAILED_SAVE_SESSION = new CommonUtils.ToastMessage("Failed saving current session!", true);
        public static final CommonUtils.ToastMessage NO_URIS = new CommonUtils.ToastMessage("Add at least one URI!", false);
        public static final CommonUtils.ToastMessage FAILED_ADD_DOWNLOAD = new CommonUtils.ToastMessage("Failed to add new download!", true);
        public static final CommonUtils.ToastMessage FAILED_CHANGE_OPTIONS = new CommonUtils.ToastMessage("Failed to change options for download!", true);
        public static final CommonUtils.ToastMessage DOWNLOAD_OPTIONS_CHANGED = new CommonUtils.ToastMessage("Download options successfully changed!", false);
        public static final CommonUtils.ToastMessage FAILED_CHANGE_FILE_SELECTION = new CommonUtils.ToastMessage("Failed selecting/deselecting file(s)!", true);
        public static final CommonUtils.ToastMessage FILE_NOT_FOUND = new CommonUtils.ToastMessage("File not found!", true);
        public static final CommonUtils.ToastMessage NO_QUICK_OPTIONS = new CommonUtils.ToastMessage("You have no quick options!", false);
        public static final CommonUtils.ToastMessage INVALID_DOWNLOAD_PATH = new CommonUtils.ToastMessage("Invalid download path.", false);
        public static final CommonUtils.ToastMessage INVALID_FILE = new CommonUtils.ToastMessage("Invalid file!", false);
        public static final CommonUtils.ToastMessage SEARCH_FAILED = new CommonUtils.ToastMessage("Search failed!", true);
        public static final CommonUtils.ToastMessage NOT_ONLINE = new CommonUtils.ToastMessage("This server isn't online. If it should try swiping down to refresh.", false);
        public static final CommonUtils.ToastMessage FAILED_CONNECTING = new CommonUtils.ToastMessage("Failed connecting!", true);
        public static final CommonUtils.ToastMessage FAILED_LOADING = new CommonUtils.ToastMessage("Failed loading!", true);
        public static final CommonUtils.ToastMessage CANNOT_SAVE_PROFILE = new CommonUtils.ToastMessage("Cannot save profile!", true);
        public static final CommonUtils.ToastMessage FAILED_PERFORMING_ACTION = new CommonUtils.ToastMessage("Failed performing the action!", true);
        public static final CommonUtils.ToastMessage PAUSED = new CommonUtils.ToastMessage("Download paused!", false);
        public static final CommonUtils.ToastMessage RESTARTED = new CommonUtils.ToastMessage("Download restarted!", false);
        public static final CommonUtils.ToastMessage RESUMED = new CommonUtils.ToastMessage("Download resumed!", false);
        public static final CommonUtils.ToastMessage MOVED = new CommonUtils.ToastMessage("Download moved in queue!", false);
        public static final CommonUtils.ToastMessage REMOVED = new CommonUtils.ToastMessage("Download removed!", false);
        public static final CommonUtils.ToastMessage RESULT_REMOVED = new CommonUtils.ToastMessage("Download result removed!", false);
        public static final CommonUtils.ToastMessage FAILED_REFRESHING = new CommonUtils.ToastMessage("Failed refreshing!", true);
        public static final CommonUtils.ToastMessage GLOBAL_OPTIONS_CHANGED = new CommonUtils.ToastMessage("Global options successfully changed!", false);
        public static final CommonUtils.ToastMessage ONLY_ONE_TORRENT = new CommonUtils.ToastMessage("When adding torrent magnets only one link should be added at once.", false);
        public static final CommonUtils.ToastMessage NO_BASE64_FILE = new CommonUtils.ToastMessage("No file selected!", false);
        public static final CommonUtils.ToastMessage NO_FILE_MANAGER = new CommonUtils.ToastMessage("A file manager has not been found!", true);
        public static final CommonUtils.ToastMessage FILE_DESELECTED = new CommonUtils.ToastMessage("File deselected for download.", false);
        public static final CommonUtils.ToastMessage FILE_SELECTED = new CommonUtils.ToastMessage("File selected for download.", false);
        public static final CommonUtils.ToastMessage DIR_DESELECTED = new CommonUtils.ToastMessage("All files in directory are deselected for download.", false);
        public static final CommonUtils.ToastMessage DIR_SELECTED = new CommonUtils.ToastMessage("All files in directory are selected for download.", false);
        public static final CommonUtils.ToastMessage DOWNLOAD_FILE_STARTED = new CommonUtils.ToastMessage("Download started!", false);
    }

    private static class CustomYAxisValueFormatter implements IAxisValueFormatter {
        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            return CommonUtils.speedFormatter(value, false);
        }
    }
}