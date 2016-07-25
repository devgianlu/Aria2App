package com.gianlu.aria2app.Options;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    private static String getAllOptionsRaw(String source) {
        Matcher matcher = Pattern.compile("(?<=\\.\\.\\s_input-file:)(.*?)(?=Server\\sPerformance\\sProfile)", Pattern.MULTILINE | Pattern.DOTALL).matcher(source);

        String optionsRaw = "";
        while (matcher.find()) optionsRaw += matcher.group();
        return optionsRaw;
    }

    static Map<String, String> getAllOptions(String source) {
        String raw = getAllOptionsRaw(source);

        Matcher matcher = Pattern.compile("^.*(\\s\\s\\*\\s:option:`).*$", Pattern.MULTILINE).matcher(raw);

        Map<String, String> options = new HashMap<>();
        while (matcher.find()) {
            String _option = matcher.group().replace("  * :option:", "").replace("`", "");

            Matcher mmatcher = Pattern.compile("<(.*)>").matcher(_option);
            if (mmatcher.find()) {
                String val = mmatcher.group();
                options.put(val.replace("<", "").replace(">", ""), _option.replace(" " + val, ""));
            }
        }

        return options;
    }
}
