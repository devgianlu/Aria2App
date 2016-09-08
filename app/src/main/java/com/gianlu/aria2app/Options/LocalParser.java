package com.gianlu.aria2app.Options;

import android.content.Context;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class LocalParser {
    private static JSONObject options;

    public static void loadOptions(final Context context, final boolean force, final IParsing handler) {
        if (force || options == null) {
            try {
                options = new JSONObject(
                        new BufferedReader(
                                new InputStreamReader(
                                        context.openFileInput("source.aria2c")))
                                .readLine());
                handler.onEnd();
            } catch (JSONException | IOException ex) {
                if (ex instanceof FileNotFoundException) {
                    Parser.refreshSource(context, new Parser.ISourceProcessor() {
                        @Override
                        public void onStarted() {

                        }

                        @Override
                        public void onDownloadEnded(String source) {

                        }

                        @Override
                        public void onConnectionError(int code, String message) {
                            handler.onError(null);
                        }

                        @Override
                        public void onError(Exception ex) {
                            handler.onError(ex);
                        }

                        @Override
                        public void onFailed() {
                            handler.onError(null);
                        }

                        @Override
                        public void onEnd() {
                            loadOptions(context, force, handler);
                        }
                    });
                } else {
                    handler.onError(ex);
                }
            }
        }
    }

    public static String getDefinition(String option) throws JSONException {
        return options.getJSONObject(option).getString("definition");
    }

    public static String getCommandLine(String option) throws JSONException {
        return options.getJSONObject(option).getString("nameCMD");
    }

    public static String getDefaultValue(String option) throws JSONException {
        return options.getJSONObject(option).optString("defaultVal");
    }

    public interface IParsing {
        void onEnd();

        void onError(@Nullable Exception exception);
    }
}
