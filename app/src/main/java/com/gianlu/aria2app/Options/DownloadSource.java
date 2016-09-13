package com.gianlu.aria2app.Options;

import android.app.Activity;
import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloadSource implements Runnable {
    private Activity context;
    private IDownload handler;

    public DownloadSource(Activity context, IDownload handler) {
        this.context = context;
        this.handler = handler;
    }

    @Override
    public void run() {
        handler.onStart();

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("http://192.168.1.25:8000/help_file.txt").openConnection();
            conn.connect();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                handler.onConnectionFailed(conn.getResponseCode(), conn.getResponseMessage());
                return;
            }

            InputStream in = conn.getInputStream();

            Pattern option_pattern = Pattern.compile("^ (?:(-.), )?--([^\\s\\[=]+)");
            Pattern values_pattern = Pattern.compile("\\s+Valori possibili: (.+)");
            Pattern default_pattern = Pattern.compile("\\s+Predefinito: (.+)");

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            Map<String, Option> options = new HashMap<>();

            String curr_option = null;
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher opt_matcher = option_pattern.matcher(line);

                if (opt_matcher.find()) {
                    Option opt = new Option();
                    opt.long_option = opt_matcher.group(2);
                    opt.short_option = opt_matcher.group(1);

                    options.put(opt.long_option, opt);
                    curr_option = opt.long_option;
                } else {
                    Matcher val_matcher = values_pattern.matcher(line);

                    if (val_matcher.find()) {
                        Option opt = options.get(curr_option);

                        if (opt != null) {
                            opt.values = Arrays.asList(val_matcher.group(1).split(", "));
                            options.remove(curr_option);
                            options.put(curr_option, opt);
                        }
                    } else {
                        Matcher def_pattern = default_pattern.matcher(line);

                        if (def_pattern.find()) {
                            Option opt = options.get(curr_option);

                            if (opt != null) {
                                opt.def = def_pattern.group(1);
                                options.remove(curr_option);
                                options.put(curr_option, opt);
                            }

                            curr_option = null;
                        }
                    }
                }
            }

            // Second stage
            for (String key : new ArrayList<>(options.keySet())) {
                Option val = options.remove(key);

                if (key.equals("torrent-file") || key.equals("metalink-file")) {
                    val.type = Option.TYPE.PATH_FILE;
                    val.values = null;
                } else if (val.values == null) {
                    val.type = Option.TYPE.STRING;
                } else if (val.values.contains("true") && val.values.contains("false")) {
                    val.type = Option.TYPE.BOOLEAN;
                    val.values = null;
                } else if (val.values.get(0).equals("/path/to/directory")) {
                    val.type = Option.TYPE.PATH_DIR;
                    val.values = null;
                } else if (val.values.get(0).equals("/path/to/file")) {
                    val.type = Option.TYPE.PATH_FILE;
                    val.values = null;
                } else if (val.values.size() > 1) {
                    val.type = Option.TYPE.MULTICHOICHE;
                } else {
                    val.type = Option.TYPE.STRING;
                    val.values = null;
                }

                options.put(key, val);
            }

            ObjectOutputStream out = new ObjectOutputStream(context.openFileOutput("options.ser", Context.MODE_PRIVATE));
            out.writeObject(options);
            out.close();

            handler.onDone();
        } catch (Exception ex) {
            handler.onException(ex);
        }
    }

    public interface IDownload {
        void onStart();

        void onDone();

        void onConnectionFailed(int code, String message);

        void onException(Exception ex);
    }
}

