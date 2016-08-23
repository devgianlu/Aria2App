package com.gianlu.aria2app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.widget.Toast;

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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

public class Utils {
    public static final int CHART_DOWNLOAD_SET = 1;
    public static final int CHART_UPLOAD_SET = 0;

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

    public static LineDataSet initDownloadSet(Context context, float lineWidth) {
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

    public static LineDataSet initUploadSet(Context context, float lineWidth) {
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

    public static String speedFormatter(float v) {
        if (v <= 0) {
            return "0 B/s";
        } else {
            final String[] units = new String[]{"B/s", "KB/s", "MB/s", "GB/s", "TB/s"};
            int digitGroups = (int) (Math.log10(v) / Math.log10(1000));
            return new DecimalFormat("#,##0.#").format(v / Math.pow(1000, digitGroups)) + " " + units[digitGroups];
        }
    }

    public static String dimensionFormatter(float v) {
        if (v <= 0) {
            return "0 B";
        } else {
            final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
            int digitGroups = (int) (Math.log10(v) / Math.log10(1000));
            return new DecimalFormat("#,##0.#").format(v / Math.pow(1000, digitGroups)) + " " + units[digitGroups];
        }
    }

    public static String timeFormatter(Long sec) {
        if (sec == null) return "∞";

        int day = (int) TimeUnit.SECONDS.toDays(sec);
        long hours = TimeUnit.SECONDS.toHours(sec) -
                TimeUnit.DAYS.toHours(day);
        long minute = TimeUnit.SECONDS.toMinutes(sec) -
                TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(sec));
        long second = TimeUnit.SECONDS.toSeconds(sec) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(sec));

        if (day > 0) {
            if (day > 1000) {
                return "∞";
            } else {
                return String.format(Locale.getDefault(), "%02d", day) + "d " + String.format(Locale.getDefault(), "%02d", hours) + "h " + String.format(Locale.getDefault(), "%02d", minute) + "m " + String.format(Locale.getDefault(), "%02d", second) + "s";
            }
        } else {
            if (hours > 0) {
                return String.format(Locale.getDefault(), "%02d", hours) + "h " + String.format(Locale.getDefault(), "%02d", minute) + "m " + String.format(Locale.getDefault(), "%02d", second) + "s";
            } else {
                if (minute > 0) {
                    return String.format(Locale.getDefault(), "%02d", minute) + "m " + String.format(Locale.getDefault(), "%02d", second) + "s";
                } else {
                    if (second > 0) {
                        return String.format(Locale.getDefault(), "%02d", second) + "s";
                    } else {
                        return "∞";
                    }
                }
            }
        }
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

    public static String colorToHex(Context context, @ColorRes int colorRes) {
        int color = ContextCompat.getColor(context, colorRes);
        String hex = Integer.toHexString(Color.rgb(Color.red(color), Color.green(color), Color.blue(color)));
        return hex.length() == 8 ? hex.substring(2) : hex;
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

    public static ProgressDialog fastProgressDialog(Context context, String title, String message, boolean indeterminate, boolean cancelable) {
        ProgressDialog pd = new ProgressDialog(context);
        pd.setTitle(title);
        pd.setMessage(message);
        pd.setIndeterminate(indeterminate);
        pd.setCancelable(cancelable);
        return pd;
    }

    public static ProgressDialog fastProgressDialog(Context context, int message, boolean indeterminate, boolean cancelable) {
        return fastProgressDialog(context, "", context.getString(message), indeterminate, cancelable);
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
        LogMe(context, message.toString(), message.isError());
    }

    public static void UIToast(final Activity context, final TOAST_MESSAGES message, final String message_extras) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        LogMe(context, message + " Details: " + message_extras, message.isError());
    }

    public static void UIToast(final Activity context, final TOAST_MESSAGES message, final Throwable exception) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        LogMe(context, message + " Details: " + exception.getMessage(), message.isError());
        SecretLog(context, exception);
    }

    public static void UIToast(final Activity context, final TOAST_MESSAGES message, final String message_extras, Runnable extra) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        context.runOnUiThread(extra);
        LogMe(context, message + " Details: " + message_extras, message.isError());
    }

    public static void UIToast(final Activity context, final TOAST_MESSAGES message, final Throwable exception, Runnable extra) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        context.runOnUiThread(extra);

        LogMe(context, message + " Details: " + exception.getMessage(), message.isError());
        SecretLog(context, exception);
    }

    public static void UIToast(final Activity context, final TOAST_MESSAGES message, Runnable extra) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        context.runOnUiThread(extra);
        LogMe(context, message.toString(), message.isError());
    }

    public static void SecretLog(Activity context, Throwable exx) {
        exx.printStackTrace();

        try {
            FileOutputStream fOut = context.openFileOutput(new SimpleDateFormat("d-LL-yyyy", Locale.getDefault()).format(new java.util.Date()) + ".secret", Context.MODE_APPEND);
            OutputStreamWriter osw = new OutputStreamWriter(fOut);

            osw.write(new SimpleDateFormat("hh:mm:ss", Locale.getDefault()).format(new java.util.Date()) + " >> " + exx.toString() + "\n" + Arrays.toString(exx.getStackTrace()) + "\n\n");
            osw.flush();
            osw.close();
        } catch (IOException ex) {
            UIToast(context, "Logger: " + ex.getMessage(), Toast.LENGTH_LONG);
        }
    }

    public static void LogMe(Activity context, String message, boolean isError) {
        try {
            FileOutputStream fOut = context.openFileOutput(new SimpleDateFormat("d-LL-yyyy", Locale.getDefault()).format(new java.util.Date()) + ".log", Context.MODE_APPEND);
            OutputStreamWriter osw = new OutputStreamWriter(fOut);

            osw.write((isError ? "--ERROR--" : "--INFO--") + new SimpleDateFormat("hh:mm:ss", Locale.getDefault()).format(new java.util.Date()) + " >> " + message.replace("\n", " ") + "\n");
            osw.flush();
            osw.close();
        } catch (IOException ex) {
            UIToast(context, "Logger: " + ex.getMessage(), Toast.LENGTH_LONG);
        }
    }
    public enum TOAST_MESSAGES {
        /* WebSocket */
        WS_OPENED("WebSocket connected!", false),
        WS_CLOSED("WebSocket has been closed!", true),
        WS_EXCEPTION("WebSocket exception!", true),
        /* Gathering information */
        FAILED_GATHERING_INFORMATION("Failed on gathering information!", true),
        /* Actions on downloads */
        PAUSED("Download paused.", false),
        REMOVED("Download removed.", false),
        REMOVED_RESULT("Download result removed.", false),
        MOVED("Download moved.", false),
        RESUMED("Download resumed.", false),
        RESTARTED("Download restarted.", false),
        CHANGED_SELECTION("File selected/deselected.", false),
        FAILED_PAUSE("Failed to pause download!", true),
        FAILED_REMOVE("Failed to remove download!", true),
        FAILED_UNPAUSE("Failed to resume download!", true),
        FAILED_REMOVE_RESULT("Failed to remove download's result!", true),
        FAILED_ADD_DOWNLOAD("Failed to add new download!", true),
        FAILED_CHANGE_OPTIONS("Failed to change options for download!", true),
        DOWNLOAD_OPTIONS_CHANGED("Download options successfully changed!", false),
        FAILED_CHANGE_POSITION("Failed changing download's queue position!", true),
        FAILED_CHANGE_FILE_SELECTION("Failed selecting/deseletcing file!", true),
        /* Application */
        UNKNOWN_EXCEPTION("Unknown exception. Don't worry!", true),
        INVALID_PROFILE_NAME("Invalid profile name!", false),
        INVALID_SERVER_IP("Invalid server address!", false),
        INVALID_SERVER_PORT("Invalid server port, must be > 0 and < 65536!", false),
        INVALID_SERVER_ENDPOINT("Invalid server RPC endpoint!", false),
        INVALID_SERVER_TOKEN("Invalid server token!", false),
        INVALID_SERVER_USER_OR_PASSWD("Invalid username or password!", false),
        INVALID_CONDITIONS_NUMBER("Mutli profile should contains more than one condition", false),
        FILE_NOT_FOUND("File not found!", true),
        FATAL_EXCEPTION("Fatal exception!", true),
        FAILED_LOADING_AUTOCOMPLETION("Unable to load method's suggestions!", true),
        NO_EMAIL_CLIENT("There are no email clients installed.", true),
        INVALID_SSID("Invalid SSID!", false),
        MUST_PICK_DEFAULT("You must select one profile as default!", false),
        INVALID_DIRECTDOWNLOAD_ADDR("Invalid DirectDownload's server address!", false),
        INVALID_DIRECTDOWNLOAD_USERORPASSWD("Invalid DirectDownload's username or password!", false),
        CANT_REFRESH_SOURCE("Can't refresh source file for options. Retry later...", true),
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

        public boolean isError() {
            return isError;
        }
    }

    public static class CustomYAxisValueFormatter implements YAxisValueFormatter {
        @Override
        public String getFormattedValue(float v, YAxis yAxis) {
            return Utils.speedFormatter(v);
        }
    }
}