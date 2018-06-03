package com.gianlu.aria2app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

import com.gianlu.aria2app.NetIO.Aria2.AriaFile;
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithUpdate;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.util.Collection;
import java.util.HashSet;

import okhttp3.HttpUrl;

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
    public static final String ACTION_PLAY_VIDEO = "play_video";
    private static final Collection<String> streamableMimeTypes = new HashSet<>();

    static {
        streamableMimeTypes.add("video/*");
        streamableMimeTypes.add("audio/*");
        streamableMimeTypes.add("*/rmvb");
        streamableMimeTypes.add("*/avi ");
        streamableMimeTypes.add("*/mkv");
        streamableMimeTypes.add("application/3gpp*");
        streamableMimeTypes.add("application/mp4");
        streamableMimeTypes.add("application/mpeg*");
        streamableMimeTypes.add("application/ogg");
        streamableMimeTypes.add("application/sdp");
        streamableMimeTypes.add("application/vnd.3gp*");
        streamableMimeTypes.add("application/vnd.apple.mpegurl");
        streamableMimeTypes.add("application/vnd.dvd*");
        streamableMimeTypes.add("application/vnd.dolby*");
        streamableMimeTypes.add("application/vnd.rn-realmedia*");
        streamableMimeTypes.add("application/x-iso9660-image");
        streamableMimeTypes.add("application/x-extension-mp4");
        streamableMimeTypes.add("application/x-flac");
        streamableMimeTypes.add("application/x-matroska");
        streamableMimeTypes.add("application/x-mpegURL");
        streamableMimeTypes.add("application/x-ogg");
        streamableMimeTypes.add("application/x-quicktimeplayer");
        streamableMimeTypes.add("application/x-shockwave-flash");
        streamableMimeTypes.add("application/xspf+xml");
        streamableMimeTypes.add("misc/ultravox");
    }

    public static boolean isStreamable(String mime) {
        for (String supported : streamableMimeTypes) {
            if (supported.charAt(0) == '*') {
                if (mime.endsWith(supported.substring(1)))
                    return true;
            } else if (supported.charAt(supported.length() - 1) == '*') {
                if (mime.startsWith(supported.substring(0, supported.length() - 1)))
                    return true;
            } else {
                if (mime.equalsIgnoreCase(supported))
                    return true;
            }
        }

        return false;
    }

    @Nullable
    public static Intent getStreamIntent(DownloadWithUpdate download, MultiProfile.UserProfile profile, AriaFile file) {
        MultiProfile.DirectDownload dd = profile.directDownload;
        if (dd == null) throw new IllegalStateException("WTF?!");

        HttpUrl base = dd.getUrl();
        if (base == null) return null;

        HttpUrl url = file.getDownloadUrl(download.update().dir, base);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(url.toString()), file.getMimeType());
        return intent;
    }

    public static boolean canHandleIntent(Context context, @NonNull Intent intent) {
        return intent.resolveActivity(context.getPackageManager()) != null;
    }

    public static void setupChart(@NonNull LineChart chart, boolean small, @ColorRes int textColor) {
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

    @NonNull
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

    @NonNull
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

    public static boolean requestWritePermission(final Activity activity, final int code) {
        if (activity == null) return false;
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                DialogUtils.showDialog(activity, new AlertDialog.Builder(activity)
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
                DialogUtils.showDialog(activity, new AlertDialog.Builder(activity)
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

    @NonNull
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

    private static class CustomYAxisValueFormatter implements IAxisValueFormatter {
        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            return CommonUtils.speedFormatter(value, false);
        }
    }
}