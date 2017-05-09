package com.gianlu.aria2app.Activities.Search;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.Html;

import com.gianlu.aria2app.R;

import org.jsoup.nodes.Element;

public class SearchResult {
    public final String name;
    public final String href;
    public final String size;
    public final UploaderType uploaderType;
    public final String uploader;
    final int seeders;
    final int leeches;

    @SuppressWarnings("deprecation")
    SearchResult(Element html) {
        Element link = html.select("td.name a:not(:has(i))").first();
        name = Html.fromHtml(link.text()).toString();
        href = SearchUtils.BASE_URL + link.attr("href");

        Element size_col = html.select("td.size").first();
        String _uploaderType = UploaderType.extractUploaderString(size_col);
        uploaderType = UploaderType.parse(_uploaderType);
        size = size_col.textNodes().get(0).text();

        seeders = Integer.parseInt(html.select("td.seeds").first().text());
        leeches = Integer.parseInt(html.select("td.leeches").first().text());

        uploader = html.select("td." + _uploaderType + " > a").first().html();
    }

    public enum UploaderType {
        ADMINISTRATOR,
        MODERATOR,
        VIP,
        UPLOADER,
        TRIAL_UPLOADER,
        USER;

        @NonNull
        public static String extractUploaderString(Element col) {
            for (String _class : col.classNames())
                if (_class.startsWith("mob-"))
                    return _class.substring(4);

            return "user";
        }

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
