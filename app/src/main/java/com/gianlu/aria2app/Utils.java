package com.gianlu.aria2app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.gianlu.commonutils.CommonUtils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;

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
    public static final String ACTION_NEW_BATCH = "new_batch_add";
    public static final String ACTION_INTERCEPTED_WEBVIEW = "intercepted_webview";
    public static final String ACTION_USE_IN_APP_DOWNLOADER = "use_in_app_downloader";
    public static final String ACTION_WEBVIEW_SET_HOMEPAGE = "set_webview_homepage";
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

    public static boolean hasWebView(@NonNull Context context) {
        return context.getPackageManager().hasSystemFeature("android.software.webview");
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

    public static boolean canHandleIntent(@NotNull Context context, @NonNull Intent intent) {
        return intent.resolveActivity(context.getPackageManager()) != null;
    }

    public static void setupChart(@NonNull LineChart chart, boolean small) {
        setupChart(chart, small, null);
    }

    public static void setupChart(@NonNull LineChart chart, boolean small, @ColorRes @Nullable Integer fgColorRes) {
        chart.clear();

        int fgColor;
        Context context = chart.getContext();
        if (fgColorRes == null)
            fgColor = CommonUtils.resolveAttrAsColor(context, R.attr.colorOnSurface);
        else
            fgColor = ContextCompat.getColor(context, fgColorRes);

        chart.setDescription(null);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.alpha(0));
        chart.setTouchEnabled(false);

        Legend legend = chart.getLegend();
        legend.setTextColor(fgColor);
        legend.setEnabled(true);

        LineData data = new LineData();
        data.setValueTextColor(fgColor);
        chart.setData(data);

        YAxis ya = chart.getAxisLeft();
        ya.setAxisLineColor(fgColor);
        ya.setTextColor(fgColor);
        ya.setTextSize(small ? 8 : 9);
        ya.setAxisMinimum(0);
        ya.setDrawAxisLine(false);
        ya.setLabelCount(small ? 4 : 8, true);
        ya.setEnabled(true);
        ya.setDrawGridLines(true);
        ya.setGridColor(fgColor);
        ya.setValueFormatter(new CustomYAxisValueFormatter());

        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setEnabled(false);

        data.addDataSet(initUploadSet(context));
        data.addDataSet(initDownloadSet(context));

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

    private static class CustomYAxisValueFormatter extends ValueFormatter {

        @Override
        public String getFormattedValue(float value) {
            return CommonUtils.speedFormatter(value, false);
        }
    }
}