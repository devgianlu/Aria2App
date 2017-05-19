package com.gianlu.aria2app.Options;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Option {
    public final String name;
    public final String value;

    private Option(Map.Entry<String, String> entry) {
        name = entry.getKey();
        value = entry.getValue();
    }

    public static List<Option> fromOptionsMap(Map<String, String> map) {
        List<Option> options = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) options.add(new Option(entry));
        return options;
    }

    public static List<Option> fromOptionsMap(Map<String, String> map, Set<String> filter) {
        List<Option> options = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet())
            if (filter.contains(entry.getKey()))
                options.add(new Option(entry));
        return options;
    }
}
