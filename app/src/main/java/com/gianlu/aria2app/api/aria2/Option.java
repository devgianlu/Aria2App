package com.gianlu.aria2app.api.aria2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.aria2app.PK;
import com.gianlu.commonutils.preferences.Prefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Option implements Comparable<Option> {
    public final String name;
    public final OptionsMap.OptionValue value;
    public OptionsMap.OptionValue newValue;
    private boolean dummyChanged;

    private Option(String name, OptionsMap.OptionValue value, boolean dummyChanged) {
        this.name = name;
        this.value = value;
        this.dummyChanged = dummyChanged;
        if (dummyChanged) this.newValue = value;
        else this.newValue = null;
    }

    @NonNull
    public static List<Option> fromMapSimpleChanged(@NonNull OptionsMap map) {
        List<Option> list = new ArrayList<>(map.size());
        for (String key : map.keySet()) list.add(new Option(key, map.get(key), true));
        return list;
    }

    @NonNull
    public static List<Option> fromOptionsMap(@NonNull OptionsMap map, @NonNull List<String> all, @Nullable Set<String> filter) {
        List<Option> options = new ArrayList<>();

        for (String key : all) {
            if (map.get(key) == null) map.put(key, "");
        }

        for (String key : map.keySet()) {
            if (all.contains(key)) {
                if (filter == null || filter.contains(key))
                    options.add(new Option(key, map.get(key), false));
            }
        }

        Collections.sort(options);

        return options;
    }

    public void setNewValue(String... str) {
        OptionsMap.OptionValue value = new OptionsMap.OptionValue(str);
        if (!Objects.equals(newValue, value)) dummyChanged = false;
        newValue = value;
    }

    public boolean isValueChanged() {
        return dummyChanged || (newValue != null && !Objects.equals(value, newValue));
    }

    public boolean isQuick() {
        return Prefs.setContains(PK.A2_QUICK_OPTIONS_MIXED, name);
    }

    public void setQuick(boolean quick) {
        if (quick) Prefs.addToSet(PK.A2_QUICK_OPTIONS_MIXED, name);
        else Prefs.removeFromSet(PK.A2_QUICK_OPTIONS_MIXED, name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Option option = (Option) o;
        return name.equals(option.name);
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(@NonNull Option o) {
        return name.compareToIgnoreCase(o.name);
    }
}
