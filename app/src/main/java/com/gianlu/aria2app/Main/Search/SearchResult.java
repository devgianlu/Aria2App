package com.gianlu.aria2app.Main.Search;

import android.support.annotation.Nullable;
import android.text.Html;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchResult {
    private static final Pattern pattern = Pattern.compile("<td\\sclass=\".*\\sname\"><a\\s.*?</a><a\\shref=\"(.*?)\">(.*?)</a>(?:<span.*>.*?</span>|)</td><td\\sclass=\".*\\sseeds\">(.*?)</td><td\\sclass=\".*\\sleeches\">(.*?)</td><td\\sclass=\"coll-date\">(.*?)</td><td\\sclass=\".*\\smob-.*?\">(.*?)<span.*>.*</span></td><td\\sclass=\"coll-5\\s(.*?)\"><a\\s.*?>(.*?)</a></td>");
    String name;
    int seeders;
    int leeches;
    long time;
    private String href;
    private String size;
    private UploaderType uploaderType;
    private String uploader;

    private SearchResult() {
    }

    @SuppressWarnings("deprecation")
    @Nullable
    static SearchResult fromHTML(String html) {
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            SearchResult result = new SearchResult();
            result.href = "http://1337x.to" + matcher.group(1);
            result.name = Html.fromHtml(matcher.group(2)).toString();
            result.seeders = Integer.parseInt(matcher.group(3));
            result.leeches = Integer.parseInt(matcher.group(4));

            result.size = matcher.group(6);
            result.uploaderType = UploaderType.parse(matcher.group(7));
            result.uploader = matcher.group(8);

            return result;
        }

        return null;
    }

    private enum UploaderType {
        ADMINISTRATOR,
        MODERATOR,
        VIP,
        UPLOADER,
        TRIAL_UPLOADER,
        USER;

        public static UploaderType parse(String val) {
            switch (val) {
                case "administrator":
                    return ADMINISTRATOR;
                case "moderator":
                    return MODERATOR;
                case "vip":
                    return VIP;
                case "uploader":
                    return UPLOADER;
                case "trial-uploader":
                    return TRIAL_UPLOADER;
                default:
                case "user":
                    return USER;
            }
        }
    }
}
