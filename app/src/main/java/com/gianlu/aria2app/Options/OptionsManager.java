package com.gianlu.aria2app.Options;

import android.content.Context;
import android.content.res.AssetManager;
import android.support.annotation.NonNull;

import com.gianlu.aria2app.NetIO.Aria2.Option;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class OptionsManager {
    private static OptionsManager instance;
    private final AssetManager manager;
    private JSONObject options;

    private OptionsManager(Context context) {
        manager = context.getAssets();
    }

    @NonNull
    public static OptionsManager get(Context context) {
        if (instance == null) instance = new OptionsManager(context);
        return instance;
    }

    @NonNull
    public List<String> loadGlobalOptions() throws IOException, JSONException {
        openOptions();

        JSONArray array = options.getJSONArray("global");
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++)
            list.add(array.getString(i));

        return list;
    }

    @NonNull
    public List<String> loadDownloadOptions() throws IOException, JSONException {
        openOptions();

        JSONArray array = options.getJSONArray("download");
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++)
            list.add(array.getString(i));

        return list;
    }

    private void openOptions() throws IOException, JSONException {
        if (options != null) return;
        BufferedReader reader = new BufferedReader(new InputStreamReader(manager.open("options.json"), Charset.forName("UTF-8")));
        options = new JSONObject(reader.readLine());
    }

    public static final class IsQuickComparator implements Comparator<Option> {
        private final Context context;
        private final boolean global;

        public IsQuickComparator(Context context, boolean global) {
            this.context = context;
            this.global = global;
        }

        @Override
        public int compare(Option o1, Option o2) {  // Assumes that options are already ordered alphabetically
            boolean b1 = o1.isQuick(context, global);
            boolean b2 = o2.isQuick(context, global);
            if (b1 && b2) return 0; // b1 && b2
            else if (b1) return -1; // b1 && !b2
            else if (b2) return 1; // !b1 && b2
            else return 0; // !b1 && !b2
        }
    }
}
