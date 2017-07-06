package com.gianlu.aria2app.Options;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.Prefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Option implements Comparable<Option> {
    public final String name;
    public final String value;
    public String newValue;

    private Option(Map.Entry<String, String> entry) {
        name = entry.getKey();
        value = entry.getValue();
    }

    public static List<Option> fromOptionsMap(Map<String, String> map, List<String> allOptions) {
        return fromOptionsMap(map, allOptions, null);
    }

    public static List<Option> fromOptionsMap(Map<String, String> map, List<String> allOptions, @Nullable Set<String> filter) {
        List<Option> options = new ArrayList<>();

        Map<String, String> allowedOptions = filterMap(map, allOptions);
        for (Map.Entry<String, String> entry : allowedOptions.entrySet()) {
            if (filter == null) {
                options.add(new Option(entry));
            } else {
                if (filter.contains(entry.getKey()))
                    options.add(new Option(entry));
            }
        }

        Collections.sort(options);

        return options;
    }

    private static Map<String, String> filterMap(Map<String, String> map, List<String> allowedOptions) {
        Map<String, String> newMap = new HashMap<>();

        for (Map.Entry<String, String> entry : map.entrySet())
            if (allowedOptions.contains(entry.getKey()))
                newMap.put(entry.getKey(), entry.getValue());

        for (String allowedOption : allowedOptions)
            if (!map.containsKey(allowedOption))
                newMap.put(allowedOption, null);

        return newMap;
    }

    public void setNewValue(String value) {
        newValue = value;
    }

    public boolean isValueChanged() {
        return newValue != null && !Objects.equals(value, newValue);
    }

    @SuppressWarnings("ConstantConditions")
    public boolean isQuick(Context context, boolean global) {
        return Prefs.getSet(context, global ? Prefs.Keys.A2_GLOBAL_QUICK_OPTIONS : Prefs.Keys.A2_QUICK_OPTIONS, new HashSet<String>()).contains(name);
    }

    public void setQuick(Context context, boolean global, boolean quick) {
        if (quick)
            Prefs.addToSet(context, global ? Prefs.Keys.A2_GLOBAL_QUICK_OPTIONS : Prefs.Keys.A2_QUICK_OPTIONS, name);
        else
            Prefs.removeFromSet(context, global ? Prefs.Keys.A2_GLOBAL_QUICK_OPTIONS : Prefs.Keys.A2_QUICK_OPTIONS, name);
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
