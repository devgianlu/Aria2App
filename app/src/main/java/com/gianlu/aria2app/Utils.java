package com.gianlu.aria2app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.gianlu.jtitan.Aria2Helper.JTA2;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

public class Utils {
    public static String SpeedFormatter(float v) {
        if (v <= 0) {
            return "0 B/s";
        } else {
            final String[] units = new String[]{"B/s", "KB/s", "MB/s", "GB/s", "TB/s"};
            int digitGroups = (int) (Math.log10(v) / Math.log10(1000));
            return new DecimalFormat("#,##0.#").format(v / Math.pow(1000, digitGroups)) + " " + units[digitGroups];
        }
    }
    public static String DimensionFormatter(float v) {
        if (v <= 0) {
            return "0 B";
        } else {
            final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
            int digitGroups = (int) (Math.log10(v) / Math.log10(1000));
            return new DecimalFormat("#,##0.#").format(v / Math.pow(1000, digitGroups)) + " " + units[digitGroups];
        }
    }
    public static String TimeFormatter(long sec) {
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

    @Nullable
    public static Integer parseInt(String val) {
        try {
            return Integer.parseInt(val);
        } catch (Exception ex) {
            return null;
        }
    }
    @Nullable
    public static Boolean parseBoolean(String val) {
        try {
            return Boolean.parseBoolean(val);
        } catch (Exception ex) {
            return false;
        }
    }

    public static WebSocket readyWebSocket(boolean isSSL, String url) throws NoSuchAlgorithmException, IOException {
        if (isSSL) {
            WebSocketFactory factory = new WebSocketFactory();
            factory.setSSLContext(SSLContext.getDefault());
            return factory.createSocket(url.replace("http://", "wss://"), 5000);
        } else {
            return new WebSocketFactory().createSocket(url.replace("http://", "ws://"), 5000);
        }
    }
    public static WebSocket readyWebSocket(Context context) throws IOException, NoSuchAlgorithmException {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences.getBoolean("a2_serverSSL", false)) {
            WebSocketFactory factory = new WebSocketFactory();
            factory.setSSLContext(SSLContext.getDefault());
            return factory.createSocket(preferences.getString("a2_serverIP", "http://127.0.0.1:6800/jsonrpc").replace("http://", "wss://"), 5000);
        } else {
            return new WebSocketFactory().createSocket(preferences.getString("a2_serverIP", "http://127.0.0.1:6800/jsonrpc").replace("http://", "ws://"), 5000);
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

    public static JTA2 readyJTA2(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        JTA2 jta2 = new JTA2(sharedPreferences.getString("a2_serverIP", "http://127.0.0.1:6800/jsonrpc"));
        jta2.setAuthentication(sharedPreferences.getBoolean("a2_serverAuth", false));
        if (sharedPreferences.getBoolean("a2_serverAuth", false))
            jta2.setToken(sharedPreferences.getString("a2_serverToken", ""));

        return jta2;
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

    public static void UIToast(final Activity context, final TOAST_MESSAGES message, final Exception exception) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message.toString(), Toast.LENGTH_SHORT).show();
            }
        });

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

    public static void UIToast(final Activity context, final TOAST_MESSAGES message, final Exception exception, Runnable extra) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        context.runOnUiThread(extra);
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

    public static void SecretLog(Activity context, Exception exx) {
        try {
            FileOutputStream fOut = context.openFileOutput(new SimpleDateFormat("d-LL-yyyy", Locale.getDefault()).format(new java.util.Date()) + ".secret", Context.MODE_APPEND);
            OutputStreamWriter osw = new OutputStreamWriter(fOut);

            osw.write(new SimpleDateFormat("hh:mm:ss", Locale.getDefault()).format(new java.util.Date()) + " >> " + exx + "\n\n");
            osw.flush();
            osw.close();
        } catch (IOException ex) {
            UIToast(context, "Logger: " + ex.getMessage(), Toast.LENGTH_LONG);
            ex.printStackTrace();
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
            ex.printStackTrace();
        }
    }

    public enum TOAST_MESSAGES {
        /* WebSocket */
        WS_OPENED("WebSocket connected!", false),
        WS_CLOSED("WebSocket has been closed!", true),
        WS_SERVICE_STOPPED("Notification service has been stopped!", true),
        WS_EXCEPTION("WebSocket exception!", true),
        /* Gathering information */
        FAILED_GATHERING_INFORMATION("Failed on gathering information!", true),
        FILE_DOWNLOAD_COMPLETED("Download complete!", false),
        FILE_DOWNLOAD_FAILED("Download failed!", true),
        FILE_DOWNLOAD_USER_STOPPED("User stopped the download!", false),
        /* Actions on downloads */
        FAILED_PAUSE("Failed to pause download!", true),
        FAILED_REMOVE("Failed to remove download!", true),
        FAILED_UNPAUSE("Failed to resume download!", true),
        FAILED_REMOVE_RESULT("Failed to remove download's result!", true),
        FAILED_ADD_DOWNLOAD("Failed to add new download!", true),
        FAILED_CHANGE_OPTIONS("Failed to change options for download!", true),
        FILE_INCLUDED("File included!", false),
        FILE_EXCLUDED("File excluded!", false),
        FAILED_INCEXCFILE("Failed including/excluding file!", true),
        DOWNLOAD_OPTIONS_CHANGED("Download options successfully changed!", false),
        DOWNLOAD_URI_ADDED("New URI download added!", false),
        DOWNLOAD_TORRENT_ADDED("New BitTorrent download added!", false),
        DOWNLOAD_METALINK_ADDED("New Metalink download added!", false),
        FILES_INCLUDED("Files included!", false),
        FILES_EXCLUDED("Files excluded!", false),
        FAILED_INCEXCFILES("Failed including/excluding files!", true),
        FAILED_CHANGE_POSITION("Failed changing download's queue position!", true),
        /* Application */
        INVALID_PROFILE_NAME("Invalid profile name!", false),
        INVALID_SERVER_IP("Invalid server address!", false),
        INVALID_SERVER_PORT("Invalid server port, must be > 0 and < 65536!", false),
        INVALID_SERVER_ENDPOINT("Invalid server RPC endpoint!", false),
        INVALID_SERVER_TOKEN("Invalid server token!", false),
        FILE_NOT_FOUND("File not found!", true),
        FATAL_EXCEPTION("Fatal exception!", true),
        FAILED_LICENSE_VERIFICATION("Failed license verification due to app error!", true),
        APPLICATION_NOT_LICENSED("The application is not licensed! Please download it from Google Play!", true),
        FAILED_LOADING_AUTOCOMPLETION("Unable to load method's suggestions!", true),
        NO_EMAIL_CLIENT("There are no email clients installed.", true),
        INVALID_SSID("Invalid SSID!", false),
        MUST_PICK_DEFAULT("You must select one profile as default!", false),
        INVALID_DIRECTDOWNLOAD_ADDR("Invalid DirectDownload's server address!", false),
        INVALID_DIRECTDOWNLOAD_USERORPASSWD("Invalid DirectDownload's username or password!", false),
        CANNOT_START_DOWNLOAD("Cannot start download!", true),
        NO_WRITE_PERMISSION("You denied write permission!", true),
        CANT_VERIFY_LICENSE("Can't verify Google license!", true),
        ANOTHER_DOWNLOAD_RUNNING("Another file is current downloading! Please wait...", false),
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
}