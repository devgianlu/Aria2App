package com.gianlu.aria2app.Main.Search;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.Html;

import com.gianlu.aria2app.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchResult {
    private static final Pattern pattern = Pattern.compile("<td\\sclass=\".*\\sname\">(?:<div\\s.*?</div>|<a\\s.*?</a>)<a\\shref=\"(.*?)\">(.*?)</a>(?:<span.*>.*?</span>|)(?:\\s|)</td><td\\sclass=\".*\\sseeds\">(.*?)</td><td\\sclass=\".*\\sleeches\">(.*?)</td><td\\sclass=\"coll-date\">.*?</td><td\\sclass=\".*\\smob-.*?\">(.*?)<span.*>.*</span></td><td\\sclass=\"coll-5\\s(.*?)\"><a\\s.*?>(.*?)</a></td>");
    public String name;
    public String href;
    public String size;
    public UploaderType uploaderType;
    public String uploader;
    int seeders;
    int leeches;

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
            result.size = matcher.group(5);
            result.uploaderType = UploaderType.parse(matcher.group(6));
            result.uploader = matcher.group(7);

            return result;
        }

        return null;
    }

    public enum UploaderType {
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

        public String toFormal(Context context) {
            switch (this) {
                case ADMINISTRATOR:
                    return context.getString(R.string.administrator);
                case MODERATOR:
                    return context.getString(R.string.moderator);
                case VIP:
                    return context.getString(R.string.vip);
                case UPLOADER:
                    return context.getString(R.string.normal_uploader);
                case TRIAL_UPLOADER:
                    return context.getString(R.string.trial_uploader);
                default:
                case USER:
                    return context.getString(R.string.user);
            }
        }
    }
}
