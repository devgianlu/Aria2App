package com.gianlu.aria2app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.widget.ImageButton;
import android.widget.Toast;

import com.gianlu.aria2app.NetIO.JTA2.Aria2Exception;
import com.gianlu.commonutils.CommonUtils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.YAxisValueFormatter;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.net.ssl.SSLContext;

public class Utils {
    public static final int CHART_DOWNLOAD_SET = 1;
    public static final int CHART_UPLOAD_SET = 0;

    public static void animateCollapsingArrowList(ImageButton view, boolean expanded) {
        if (expanded)
            view.animate()
                    .rotation(0)
                    .setDuration(200)
                    .start();
        else
            view.animate()
                    .rotation(90)
                    .setDuration(200)
                    .start();
    }

    public static void animateCollapsingArrowBellows(ImageButton view, boolean expanded) {
        if (expanded)
            view.animate()
                    .rotation(0)
                    .setDuration(200)
                    .start();
        else
            view.animate()
                    .rotation(180)
                    .setDuration(200)
                    .start();
    }

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

    public static LineChart setupChart(LineChart chart, boolean isCardView) {
        chart.clear();

        chart.setDescription("");
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.alpha(0));
        chart.setTouchEnabled(false);
        Legend l = chart.getLegend();
        l.setCustom(
                new int[]{ContextCompat.getColor(chart.getContext(), R.color.downloadColor), ContextCompat.getColor(chart.getContext(), R.color.uploadColor)},
                new String[]{chart.getContext().getString(R.string.downloadSpeed), chart.getContext().getString(R.string.uploadSpeed)});
        l.setEnabled(true);

        LineData data = new LineData();
        data.setValueTextColor(ContextCompat.getColor(chart.getContext(), R.color.colorPrimaryDark));
        chart.setData(data);

        YAxis ya = chart.getAxisLeft();
        ya.setAxisLineColor(ContextCompat.getColor(chart.getContext(), R.color.colorPrimaryDark));
        ya.setTextColor(ContextCompat.getColor(chart.getContext(), R.color.colorPrimaryDark));
        ya.setTextSize(isCardView ? 8 : 9);
        ya.setDrawAxisLine(false);
        ya.setLabelCount(isCardView ? 4 : 8, true);
        ya.setEnabled(true);
        ya.setAxisMinValue(0f);
        ya.setDrawGridLines(true);
        ya.setValueFormatter(new CustomYAxisValueFormatter());

        chart.getAxisRight().setEnabled(false);
        XAxis xa = chart.getXAxis();
        xa.setEnabled(!isCardView);
        if (!isCardView) {
            xa.setDrawGridLines(false);
            xa.setTextSize(9);
        }

        data.addDataSet(initUploadSet(chart.getContext(), 2f));
        data.addDataSet(initDownloadSet(chart.getContext(), 2f));

        return chart;
    }

    public static LineChart setupPeerChart(LineChart chart) {
        chart.clear();

        chart.setDescription("");
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.alpha(0));
        chart.setTouchEnabled(false);
        chart.getLegend().setEnabled(false);

        LineData data = new LineData();
        data.setValueTextColor(ContextCompat.getColor(chart.getContext(), R.color.white));
        chart.setData(data);

        YAxis ya = chart.getAxisLeft();
        ya.setAxisLineColor(ContextCompat.getColor(chart.getContext(), R.color.white));
        ya.setTextColor(ContextCompat.getColor(chart.getContext(), R.color.white));
        ya.setTextSize(8);
        ya.setDrawAxisLine(false);
        ya.setLabelCount(3, true);
        ya.setEnabled(true);
        ya.setAxisMinValue(0f);
        ya.setDrawGridLines(true);
        ya.setValueFormatter(new CustomYAxisValueFormatter());

        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setEnabled(false);

        data.addDataSet(initUploadSet(chart.getContext(), 1f));
        data.addDataSet(initDownloadSet(chart.getContext(), 1f));

        return chart;
    }

    private static LineDataSet initDownloadSet(Context context, float lineWidth) {
        LineDataSet set = new LineDataSet(null, context.getString(R.string.downloadSpeed));
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(lineWidth);
        set.setColor(ContextCompat.getColor(context, R.color.downloadColor));
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawFilled(false);
        return set;
    }

    private static LineDataSet initUploadSet(Context context, float lineWidth) {
        LineDataSet set = new LineDataSet(null, context.getString(R.string.uploadSpeed));
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(lineWidth);
        set.setColor(ContextCompat.getColor(context, R.color.uploadColor));
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawFilled(false);
        return set;
    }

    public static String formatConnectionError(int code, String message) {
        return "#" + code + ": " + message;
    }

    @NonNull
    public static List<Integer> bitfieldProcessor(int numPieces, String bitfield) {
        List<Integer> pieces = new ArrayList<>();

        for (char hexChar : bitfield.toLowerCase().toCharArray()) {
            switch (hexChar) {
                case '0':
                    pieces.add(0);
                    break;
                case '1':
                case '2':
                case '4':
                case '8':
                    pieces.add(1);
                    break;
                case '3':
                case '5':
                case '6':
                case '9':
                case 'a':
                case 'c':
                    pieces.add(2);
                    break;
                case '7':
                case 'b':
                case 'd':
                case 'e':
                    pieces.add(3);
                    break;
                case 'f':
                    pieces.add(4);
                    break;
            }
        }

        return pieces.subList(0, numPieces / 4);
    }

    public static int mapAlpha(int val) {
        return 255 / 4 * val;
    }

    public static WebSocket readyWebSocket(boolean isSSL, String url, @NonNull String username, @NonNull String password) throws IOException, NoSuchAlgorithmException {
        if (isSSL) {
            WebSocketFactory factory = new WebSocketFactory();
            factory.setSSLContext(SSLContext.getDefault());

            return factory.createSocket(url.replace("http://", "wss://"), 5000)
                    .addHeader("Authorization", "Basic " + Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP));
        } else {
            return new WebSocketFactory().createSocket(url.replace("http://", "ws://"), 5000)
                    .addHeader("Authorization", "Basic " + Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP));
        }
    }

    public static WebSocket readyWebSocket(boolean isSSL, String url) throws NoSuchAlgorithmException, IOException {
        if (isSSL) {
            return new WebSocketFactory()
                    .setSSLContext(SSLContext.getDefault())
                    .setConnectionTimeout(5000)
                    .createSocket(url.replace("http://", "wss://"), 5000);
        } else {
            return new WebSocketFactory()
                    .setConnectionTimeout(5000)
                    .createSocket(url.replace("http://", "ws://"), 5000);
        }
    }

    public static WebSocket readyWebSocket(Context context) throws IOException, NoSuchAlgorithmException {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences.getBoolean("a2_serverSSL", false)) {
            WebSocketFactory factory = new WebSocketFactory()
                    .setSSLContext(SSLContext.getDefault())
                    .setConnectionTimeout(5000);
            WebSocket socket = factory.createSocket(preferences.getString("a2_serverIP", "http://127.0.0.1:6800/jsonrpc").replace("http://", "wss://"), 5000);

            if (preferences.getString("a2_authMethod", "NONE").equals("HTTP"))
                socket.addHeader("Authorization", "Basic " + Base64.encodeToString((preferences.getString("a2_serverUsername", "username") + ":" + preferences.getString("a2_serverPassword", "password")).getBytes(), Base64.NO_WRAP));

            return socket;
        } else {
            WebSocket socket = new WebSocketFactory()
                    .setConnectionTimeout(5000)
                    .createSocket(preferences.getString("a2_serverIP", "http://127.0.0.1:6800/jsonrpc").replace("http://", "ws://"), 5000);

            if (preferences.getString("a2_authMethod", "NONE").equals("HTTP"))
                socket.addHeader("Authorization", "Basic " + Base64.encodeToString((preferences.getString("a2_serverUsername", "username") + ":" + preferences.getString("a2_serverPassword", "password")).getBytes(), Base64.NO_WRAP));

            return socket;
        }
    }

    public static JSONArray readyParams(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        JSONArray array = new JSONArray();
        if (preferences.getString("a2_authMethod", "NONE").equals("TOKEN"))
            array.put("token:" + preferences.getString("a2_serverToken", "token"));

        return array;
    }

    public static JSONObject readyRequest() throws JSONException {
        return new JSONObject().put("jsonrpc", "2.0").put("id", String.valueOf(new Random().nextInt(9999)));
    }

    public static void UIToast(final Activity context, final String text) {
        UIToast(context, text, Toast.LENGTH_SHORT);
    }

    public static void UIToast(final Activity context, final String text, final int duration) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, text, duration).show();
            }
        });
    }

    public static void UIToast(final Activity context, final String text, final int duration, Runnable extra) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, text, duration).show();
            }
        });
        context.runOnUiThread(extra);
    }

    public static void UIToast(final Activity context, final TOAST_MESSAGES message) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message.toString() + (message.isError() ? " See logs for more..." : ""), Toast.LENGTH_SHORT).show();
            }
        });
        CommonUtils.logMe(context, message.toString(), message.isError());
    }

    public static void UIToast(final Activity context, final TOAST_MESSAGES message, final String message_extras) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        CommonUtils.logMe(context, message + " Details: " + message_extras, message.isError());
    }

    public static void UIToast(final Activity context, final TOAST_MESSAGES message, final Throwable exception) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (exception instanceof Aria2Exception)
                    Toast.makeText(context, message.toString(exception.getMessage()), Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(context, message.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        CommonUtils.logMe(context, message + " Details: " + exception.getMessage(), message.isError());
        CommonUtils.secretLog(context, exception);
    }

    public static void UIToast(final Activity context, final TOAST_MESSAGES message, final String message_extras, Runnable extra) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        context.runOnUiThread(extra);
        CommonUtils.logMe(context, message + " Details: " + message_extras, message.isError());
    }

    public static void UIToast(final Activity context, final TOAST_MESSAGES message, final Throwable exception, Runnable extra) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (exception instanceof Aria2Exception)
                    Toast.makeText(context, message.toString(exception.getMessage()), Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(context, message.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        context.runOnUiThread(extra);

        CommonUtils.logMe(context, message + " Details: " + exception.getMessage(), message.isError());
        CommonUtils.secretLog(context, exception);
    }

    public static void UIToast(final Activity context, final TOAST_MESSAGES message, Runnable extra) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        context.runOnUiThread(extra);
        CommonUtils.logMe(context, message.toString(), message.isError());
    }

    public enum TOAST_MESSAGES {
        WS_OPENED("WebSocket connected!", false),
        WS_CLOSED("WebSocket has been closed!", true),
        WS_EXCEPTION("WebSocket exception!", true),
        FAILED_GATHERING_INFORMATION("Failed on gathering information!", true),
        PAUSED("Download paused.", false),
        REMOVED("Download removed.", false),
        REMOVED_RESULT("Download result removed.", false),
        MOVED("Download moved.", false),
        RESUMED("Download resumed.", false),
        RESTARTED("Download restarted.", false),
        CHANGED_SELECTION("File selected/deselected.", false),
        SESSION_SAVED("Session saved correctly.", false),
        FAILED_SAVE_SESSION("Failed saving current session!", true),
        FAILED_PAUSE("Failed to pause download!", true),
        MUST_CREATE_FIRST_PROFILE("You must create your first profile to run the application!", false),
        CANNOT_EDIT_PROFILE("Cannot edit this profile!", true),
        PROFILE_DOES_NOT_EXIST("Profile doesn't exist!", true),
        FAILED_REMOVE("Failed to remove download!", true),
        FAILED_UNPAUSE("Failed to resume download!", true),
        FAILED_REMOVE_RESULT("Failed to remove download's result!", true),
        FAILED_ADD_DOWNLOAD("Failed to add new download!", true),
        FAILED_CHANGE_OPTIONS("Failed to change options for download!", true),
        DOWNLOAD_OPTIONS_CHANGED("Download options successfully changed!", false),
        FAILED_CHANGE_POSITION("Failed changing download's queue position!", true),
        FAILED_CHANGE_FILE_SELECTION("Failed selecting/deselecting file!", true),
        FAILED_CHECKING_VERSION("Failed checking aria2 version!", true),
        LOGS_DELETED("Logs deleted!", false),
        INVALID_PROFILE_NAME("Invalid profile name!", false),
        INVALID_SERVER_IP("Invalid server address!", false),
        INVALID_SERVER_PORT("Invalid server port, must be > 0 and < 65536!", false),
        INVALID_SERVER_ENDPOINT("Invalid server RPC endpoint!", false),
        INVALID_SERVER_TOKEN("Invalid server token!", false),
        INVALID_SERVER_USER_OR_PASSWD("Invalid username or password!", false),
        INVALID_CONDITIONS_NUMBER("Multi profile should contains more than one condition", false),
        FILE_NOT_FOUND("File not found!", true),
        FATAL_EXCEPTION("Fatal exception!", true),
        FAILED_LOADING_AUTOCOMPLETION("Unable to load method's suggestions!", true),
        FAILED_CLEARING_LOGS("Failed clearing old logs!", true),
        NO_EMAIL_CLIENT("There are no email clients installed.", true),
        INVALID_SSID("Invalid SSID!", false),
        MUST_PICK_DEFAULT("You must select one profile as default!", false),
        INVALID_DIRECTDOWNLOAD_ADDR("Invalid DirectDownload's server address!", false),
        INVALID_DIRECTDOWNLOAD_USERORPASSWD("Invalid DirectDownload's username or password!", false),
        CANT_REFRESH_SOURCE("Can't refresh source file for options. Retry later...", true),
        ADD_QUICK_OPTIONS("You have no quick options!", false),
        SOURCE_REFRESHED("Source file for options refreshed!", false);

        private final String text;
        private final boolean isError;

        TOAST_MESSAGES(final String text, final boolean isError) {
            this.text = text;
            this.isError = isError;
        }

        @Override
        public String toString() {
            return text;
        }

        public String toString(String extra) {
            return text + " " + extra;
        }

        public boolean isError() {
            return isError;
        }
    }

    private static class CustomYAxisValueFormatter implements YAxisValueFormatter {
        @Override
        public String getFormattedValue(float v, YAxis yAxis) {
            return CommonUtils.speedFormatter(v);
        }
    }
}