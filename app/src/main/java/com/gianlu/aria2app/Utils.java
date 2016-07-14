package com.gianlu.aria2app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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

/**
 * Utils class :D
 */
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
                Toast.makeText(context, "#" + message.toCode() + ": " + message.toString() + (message.isError() ? " See logs for more..." : ""), Toast.LENGTH_SHORT).show();
            }
        });
        LogMe(context, message.toString(), message.isError());
    }

    public static void UIToast(final Activity context, final TOAST_MESSAGES message, final String message_extras) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, "#" + message.toCode() + ": " + message.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        LogMe(context, message + " Details: " + message_extras, message.isError());
    }

    public static void UIToast(final Activity context, final TOAST_MESSAGES message, final String message_extras, Runnable extra) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, "#" + message.toCode() + ": " + message.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        context.runOnUiThread(extra);
        LogMe(context, message + " Details: " + message_extras, message.isError());
    }

    public static void UIToast(final Activity context, final TOAST_MESSAGES message, Runnable extra) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message.toString() + " #" + message.toCode(), Toast.LENGTH_SHORT).show();
            }
        });
        context.runOnUiThread(extra);
        LogMe(context, message.toString(), message.isError());
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
        WS_OPENED("WebSocket connected!", 101, false),
        WS_CLOSED("WebSocket has been closed!", 102, true),
        WS_SERVICE_STOPPED("Notification service has been stopped!", 103, true),
        WS_EXCEPTION("WebSocket exception!", 104, true),
        /* Gathering information */
        FAILED_GATHERING_INFORMATION("Failed on gathering information!", 201, true),
        FILE_DOWNLOAD_COMPLETED("Download complete!", 202, false),
        FILE_DOWNLOAD_FAILED("Download failed!", 203, true),
        FILE_DOWNLOAD_USER_STOPPED("User stopped the download!", 204, false),
        /* Actions on downloads */
        FAILED_FORCEPAUSE("Failed to force pause download!", 301, true),
        FAILED_FORCEREMOVE("Failed to force remove download!", 302, true),
        FAILED_PAUSE("Failed to pause download!", 303, true),
        FAILED_REMOVE("Failed to remove download!", 304, true),
        FAILED_UNPAUSE("Failed to resume download!", 305, true),
        FAILED_REMOVERESULT("Failed to remove download's result!", 306, true),
        FAILED_ADDDOWNLOAD("Failed to add new download!", 307, true),
        FAILED_CHANGEOPTIONS("Failed to change options for download!", 308, true),
        FILE_INCLUDED("File included!", 309, false),
        FILE_EXCLUDED("File excluded!", 310, false),
        FAILED_INCEXCFILE("Failed including/excluding file!", 311, true),
        DOWNLOAD_OPTIONSCHANGED("Download options successfully changed!", 312, false),
        DOWNLOAD_ACTIONSUBMITTED("Action on download submitted!", 313, false),
        DOWNLOAD_URIADDED("New URI download added!", 314, false),
        DOWNLOAD_TORRENTADDED("New BitTorrent download added!", 315, false),
        DOWNLOAD_METALINKADDED("New Metalink download added!", 316, false),
        FILES_INCLUDED("Files included!", 317, false),
        FILES_EXCLUDED("Files excluded!", 317, false),
        FAILED_INCEXCFILES("Failed including/excluding files!", 318, true),
        /* Application */
        INVALID_PROFILE_NAME("Invalid profile name!", 401, false),
        INVALID_SERVER_IP("Invalid server address!", 402, false),
        INVALID_SERVER_PORT("Invalid server port, must be > 0 and < 65536!", 403, false),
        INVALID_SERVER_ENDPOINT("Invalid server RPC endpoint!", 404, false),
        INVALID_SERVER_TOKEN("Invalid server token!", 405, false),
        FILE_NOTFOUND("File not found!", 406, true),
        FATAL_EXCEPTION("Fatal exception!", 407, true),
        FAILED_LICENSE_VERIFICATION("Failed license verification due to app error!", 408, true),
        APPLICATION_NOT_LICENSED("The application is not licensed! Please download it from Google Play!", 409, true),
        FAILED_LOADING_AUTOCOMPLETION("Unable to load method's suggestions!", 410, true),
        NO_EMAIL_CLIENT("There are no email clients installed.", 411, true),
        INVALID_SSID("Invalid SSID!", 412, false),
        MUST_PICK_DEFAULT("You must select one profile as default!", 413, false),
        INVALID_DIRECTDOWNLOAD_ADDR("Invalid DirectDownload's server address!", 414, false),
        INVALID_DIRECTDOWNLOAD_USERORPASSWD("Invalid DirectDownload's username or password!", 415, false),
        CANNOT_START_DOWNLOAD("Cannot start download!", 416, true),
        NO_WRITE_PERMISSION("You denied write permission!", 417, true),
        CANT_VERIFY_LICENSE("Can't verify Google license!", 418, true),
        ANOTHER_DOWNLOAD_RUNNING("Another file is current downloading! Please wait...", 419, false);

        private final String text;
        private final int code;
        private final boolean isError;

        TOAST_MESSAGES(final String text, final int code, final boolean isError) {
            this.text = text;
            this.code = code;
            this.isError = isError;
        }

        @Override
        public String toString() {
            return text;
        }

        public int toCode() {
            return code;
        }

        public boolean isError() {
            return isError;
        }
    }
}