package com.gianlu.aria2app.NetIO.Aria2;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.PKeys;
import com.gianlu.commonutils.Preferences.Prefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Option implements Comparable<Option> {
    public final String name;
    public final String value;
    public String newValue;

    private Option(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @NonNull
    public static List<Option> fromOptionsMap(Map<String, String> map, List<String> all, @Nullable Set<String> filter) {
        List<Option> options = new ArrayList<>();

        for (String key : all) {
            if (map.get(key) == null) map.put(key, "");
        }

        for (String key : map.keySet()) {
            if (all.contains(key)) {
                if (filter == null || filter.contains(key))
                    options.add(new Option(key, map.get(key)));
            }
        }

        Collections.sort(options);

        return options;
    }

    public void setNewValue(String value) {
        newValue = value;
    }

    public boolean isValueChanged() {
        return newValue != null && !Objects.equals(value, newValue);
    }

    public boolean isQuick(Context context) {
        return Prefs.getSet(context, PKeys.A2_QUICK_OPTIONS_MIXED, new HashSet<String>()).contains(name);
    }

    public void setQuick(Context context, boolean quick) {
        if (quick) Prefs.addToSet(context, PKeys.A2_QUICK_OPTIONS_MIXED, name);
        else Prefs.removeFromSet(context, PKeys.A2_QUICK_OPTIONS_MIXED, name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Option option = (Option) o;
        return name.equals(option.name);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(@NonNull Option o) {
        return name.compareToIgnoreCase(o.name);
    }
}
