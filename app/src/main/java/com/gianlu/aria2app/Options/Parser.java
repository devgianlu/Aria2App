package com.gianlu.aria2app.Options;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.Spanned;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    public static Spanned formatDefinition(String colorAccent, String definition) {
        // TODO: Do it better
        return Html.fromHtml(definition
                .replaceAll("``(\\S*)``", "<b>$1</b>")
                .replaceAll("\\s*\\.\\.\\scode-block::\\s(.*)", " <b>Code</b> ($1):\n")
                .replaceAll("\\s*\\.\\.\\snote::\\s", " <b>Note:</b>\n")
                .replaceAll(":option:`(\\S*)`", "<font color='#" + colorAccent + "'>$1</font>"));
    }

    @Nullable
    private static String getOptionName(String option, String raw) {
        if (raw == null) return null;

        Matcher matcher = Pattern.compile("\\s\\s\\*\\s:option:`(.*) <" + option + ">`", Pattern.MULTILINE).matcher(raw);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    @Nullable
    private static List<String> getAllOptionsList(String raw) {
        if (raw == null) return null;

        Matcher matcher = Pattern.compile("^\\s\\s\\*\\s:option:`(.*)`.*$", Pattern.MULTILINE).matcher(raw);

        List<String> options = new ArrayList<>();
        while (matcher.find()) {
            String _option = matcher.group();

            Matcher mmatcher = Pattern.compile("<(.*)>").matcher(_option);
            if (mmatcher.find()) {
                options.add(mmatcher.group(1));
            }
        }

        return options;
    }

    private static String getOptionDefinitionsRaw(String source) {
        Matcher matcher = Pattern.compile("(?<=Basic\\sOptions\\n~~~~~~~~~~~~~)(.*)(?=Notes\\sfor\\sOptions\\n~~~~~~~~~~~~~~~~~)", Pattern.DOTALL).matcher(source);

        String definitionsRaw = "";
        while (matcher.find()) definitionsRaw += matcher.group();
        return definitionsRaw;
    }

    @Nullable
    private static String getOptionDefinition(String option, String source) {
        String raw = getOptionDefinitionsRaw(source);

        Matcher matcher = Pattern.compile("^.*^\\.\\.\\soption::.*" + option + ".*$", Pattern.MULTILINE).matcher(raw);

        if (matcher.find()) {
            Matcher mmatcher = Pattern.compile("(?<=" + matcher.group().replace(" ", "\\s").replace(".", "\\.").replace("[", "\\[").replace("]", "\\]").replace("|", "\\|") + "\\n\\n)(.*?)(?=\\n\\.\\.\\soption::)", Pattern.MULTILINE | Pattern.DOTALL).matcher(source);
            if (mmatcher.find()) {
                return mmatcher.group().replace("  ", "").replace("\\n  ", " ");
            }
        }

        return null;
    }

    @Nullable
    private static String getOptionDefault(String optionDefinition) {
        Matcher matcher = Pattern.compile("Default:.*``(.*)``", Pattern.DOTALL).matcher(optionDefinition);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    static SourceOption getOption(String option, String source) {
        String definition = getOptionDefinition(option, source);
        return new SourceOption(getOptionName(option, source), option, definition, getOptionDefault(definition));
    }

    @Nullable
    public static List<SourceOption> getAllOptions(String source) {
        List<String> list = getAllOptionsList(source);
        if (list == null) return null;

        List<SourceOption> options = new ArrayList<>();

        for (String name : list) {
            options.add(getOption(name, source));
        }

        return options;
    }

    public void refreshSource(Context context, ISourceProcessor handler) {
        new Thread(new DownloadSource(context, handler)).start();
    }

    public interface ISourceProcessor {
        void onStarted();
        void onDownloadEnded(String source);
        void onConnectionError(int code, String message);
        void onError(Exception ex);
        void onFailed();
        void onEnd();
    }

    private class DownloadSource implements Runnable {
        private ISourceProcessor handler;
        private Context context;

        public DownloadSource(Context context, ISourceProcessor handler) {
            this.handler = handler;
            this.context = context;
        }

        @Override
        public void run() {
            String source;
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL("https://aria2.github.io/manual/en/html/_sources/aria2c.txt").openConnection();
                try {
                    connection.setRequestProperty("User-Agent", "Aria2App " + context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName);
                } catch (PackageManager.NameNotFoundException ex) {
                    connection.setRequestProperty("User-Agent", "Aria2App");
                }
                connection.setRequestProperty("Connection", "keep-alive");
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    handler.onConnectionError(connection.getResponseCode(), connection.getResponseMessage());
                    return;
                }

                InputStream in = connection.getInputStream();
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                handler.onStarted();

                byte data[] = new byte[4096];
                int count;
                while ((count = in.read(data)) != -1) out.write(data, 0, count);

                in.close();
                out.close();
                source = out.toString("UTF-8");
                handler.onDownloadEnded(source);
            } catch (IOException ex) {
                handler.onError(ex);
                return;
            }

            List<SourceOption> options = getAllOptions(source);
            if (options == null) {
                handler.onFailed();
                return;
            }

            try {
                JSONObject jOptions = new JSONObject();

                for (SourceOption option : options) {
                    jOptions.put(option.getName(), option.toJSON());
                }

                OutputStream out = context.openFileOutput("source.aria2c", Context.MODE_PRIVATE);
                out.write(jOptions.toString().getBytes());
                out.flush();
                out.close();

                handler.onEnd();
            } catch (JSONException | IOException ex) {
                handler.onError(ex);
            }
        }
    }
}