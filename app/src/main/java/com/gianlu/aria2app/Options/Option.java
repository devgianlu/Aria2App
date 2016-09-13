package com.gianlu.aria2app.Options;

import android.app.Activity;
import android.support.annotation.Nullable;

import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Option implements Serializable {
    public String short_option;
    public String long_option;
    public Object def;
    public List<String> values;
    public TYPE type;

    @SuppressWarnings("unchecked")
    @Nullable
    public static Map<String, Option> loadOptionsMap(final Activity context) {
        try {
            return (Map<String, Option>) new ObjectInputStream(context.openFileInput("options.ser")).readObject();
        } catch (Exception ex) {
            return null;
        }
    }

    public enum TYPE {
        BOOLEAN,
        PATH_DIR,
        PATH_FILE,
        MULTICHOICHE,
        STRING
    }
}
