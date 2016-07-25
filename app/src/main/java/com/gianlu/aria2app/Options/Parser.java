package com.gianlu.aria2app.Options;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;

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
    @Nullable
    private static String getAllOptionsRaw(String source) {
        Matcher matcher = Pattern.compile("(?<=\\.\\.\\s_input-file:)(.*?)(?=Server\\sPerformance\\sProfile)", Pattern.MULTILINE | Pattern.DOTALL).matcher(source);

        if (matcher.find()) return matcher.group();
        return null;
    }

    @Nullable
    private static String getOptionFormal(String option, String source) {
        String raw = getAllOptionsRaw(source);
        if (raw == null) return null;

        Matcher matcher = Pattern.compile("\\s\\s\\*\\s:option:`(.*) <" + option + ">`", Pattern.MULTILINE).matcher(raw);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    @Nullable
    private static List<String> getAllOptionsList(String source) {
        String raw = getAllOptionsRaw(source);
        if (raw == null) return null;

        Matcher matcher = Pattern.compile("^\\s\\s\\*\\s:option:`(.*)`$", Pattern.MULTILINE).matcher(raw);

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
                return mmatcher.group();
            } else {
                System.out.println("Nothing");
            }
        }

        return null;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static SourceOption.DEFAULT_TYPE getOptionDefaultType(String optionDefinition) {
        Matcher matcher = Pattern.compile("Default:.*``(.*)``", Pattern.DOTALL).matcher(optionDefinition);

        if (matcher.find()) {
            String val = matcher.group(1);

            try {
                Integer.parseInt(val);
                return SourceOption.DEFAULT_TYPE.INTEGER;
            } catch (Exception ignored) {
            }

            if (val.equals("true") || val.equals("false"))
                return SourceOption.DEFAULT_TYPE.BOOLEAN;
            else
                return SourceOption.DEFAULT_TYPE.STRING;
        }

        return SourceOption.DEFAULT_TYPE.NONE;
    }

    @Nullable
    private static Integer getOptionDefaultInt(String optionDefinition) {
        Matcher matcher = Pattern.compile("Default:.*``(.*)``", Pattern.DOTALL).matcher(optionDefinition);

        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (Exception ex) {
                return null;
            }
        } else {
            return null;
        }
    }

    @Nullable
    private static Boolean getOptionDefaultBoolean(String optionDefinition) {
        Matcher matcher = Pattern.compile("Default:.*``(.*)``", Pattern.DOTALL).matcher(optionDefinition);

        if (matcher.find()) {
            try {
                return Boolean.parseBoolean(matcher.group(1));
            } catch (Exception ex) {
                return null;
            }
        } else {
            return null;
        }
    }

    @Nullable
    private static String getOptionDefaultString(String optionDefinition) {
        Matcher matcher = Pattern.compile("Default:.*``(.*)``", Pattern.DOTALL).matcher(optionDefinition);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    static SourceOption getOption(String option, String source) {
        String definition = getOptionDefinition(option, source);
        SourceOption.DEFAULT_TYPE type = getOptionDefaultType(definition);

        Object defaultVal = null;
        switch (type) {
            case INTEGER:
                defaultVal = getOptionDefaultInt(definition);
                break;
            case STRING:
                defaultVal = getOptionDefaultString(definition);
                break;
            case BOOLEAN:
                defaultVal = getOptionDefaultBoolean(definition);
                break;
        }

        return new SourceOption(option, getOptionFormal(option, source), type, defaultVal);
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
                JSONArray jOptions = new JSONArray();

                for (SourceOption option : options) {
                    jOptions.put(option.toJSON());
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