package com.gianlu.aria2app.Options;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class LocalParser {
    private JSONObject options;

    public LocalParser(Context context, boolean force) throws IOException, JSONException {
        if (force || options == null) {
            options = new JSONObject(
                    new BufferedReader(
                            new InputStreamReader(
                                    context.openFileInput("source.aria2c")))
                            .readLine());
        }
    }

    public String getDefinition(String option) throws JSONException {
        return options.getJSONObject(option).getString("definition");
    }

    public String getCommandLine(String option) throws JSONException {
        return options.getJSONObject(option).getString("nameCMD");
    }

    public String getDefaultValue(String option) throws JSONException {
        return options.getJSONObject(option).optString("defaultVal");
    }
}
