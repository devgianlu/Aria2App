package com.gianlu.aria2app.Main.Search;

import android.support.annotation.Nullable;

import com.gianlu.aria2app.NetIO.StatusCodeException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchUtils {
    public static final String TRENDING_WEEK = "trending-week";

    public static void search(final String query, final ISearch handler) {
        if (Objects.equals(query, TRENDING_WEEK)) {
            fetchTrendingThisWeek(handler);
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL("http://1337x.to/search/" + query + "/1/").openConnection();
                    conn.connect();

                    if (conn.getResponseCode() != 200) {
                        handler.onException(new StatusCodeException(conn.getResponseCode(), conn.getResponseMessage()));
                        return;
                    }

                    String html = "";
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    String line;
                    while ((line = reader.readLine()) != null)
                        html += line;

                    reader.close();
                    conn.disconnect();

                    Matcher matcher = Pattern.compile("<table\\s.*?><thead>.*?</thead><tbody>(.*?)</tbody></table>").matcher(html);
                    List<SearchResult> results = new ArrayList<>();
                    if (matcher.find()) {
                        Matcher resultMatcher = Pattern.compile("<tr>(.*?)</tr>").matcher(matcher.group(1));

                        while (resultMatcher.find()) {
                            SearchResult result = SearchResult.fromHTML(resultMatcher.group(1));
                            if (result != null)
                                results.add(result);
                        }
                    }

                    handler.onResults(results);
                } catch (IOException ex) {
                    handler.onException(ex);
                }
            }
        }).start();
    }

    private static void fetchTrendingThisWeek(final ISearch handler) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL("http://1337x.to/trending-week").openConnection();
                    conn.connect();

                    if (conn.getResponseCode() != 200) {
                        handler.onException(new StatusCodeException(conn.getResponseCode(), conn.getResponseMessage()));
                        return;
                    }

                    String html = "";
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    String line;
                    while ((line = reader.readLine()) != null)
                        html += line;

                    reader.close();
                    conn.disconnect();

                    Matcher matcher = Pattern.compile("<table\\s.*?><thead>.*?</thead><tbody>(.*?)</tbody></table>").matcher(html);
                    List<SearchResult> results = new ArrayList<>();
                    if (matcher.find()) {
                        Matcher resultMatcher = Pattern.compile("<tr>(.*?)</tr>").matcher(matcher.group(1));

                        while (resultMatcher.find()) {
                            SearchResult result = SearchResult.fromHTML(resultMatcher.group(1));
                            if (result != null)
                                results.add(result);
                        }
                    }

                    handler.onResults(results);
                } catch (IOException ex) {
                    handler.onException(ex);
                }
            }
        }).start();
    }

    public static void findMagnetLink(final SearchResult item, final IMagnetLink handler) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(item.href).openConnection();
                    conn.connect();

                    if (conn.getResponseCode() != 200) {
                        handler.onException(new StatusCodeException(conn.getResponseCode(), conn.getResponseMessage()));
                        return;
                    }

                    String html = "";
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    String line;
                    while ((line = reader.readLine()) != null)
                        html += line;

                    reader.close();
                    conn.disconnect();

                    Matcher matcher = Pattern.compile("<a\\sclass=\".*?btn-magnet\"\\shref=\"(.*?)\".*?</a>").matcher(html);
                    if (matcher.find()) {
                        handler.onMagnetLink(matcher.group(1));
                    } else {
                        handler.onMagnetLink(null);
                    }
                } catch (IOException ex) {
                    handler.onException(ex);
                }
            }
        }).start();
    }

    public interface IMagnetLink {
        void onMagnetLink(@Nullable String magnet);

        void onException(Exception ex);
    }

    public interface ISearch {
        void onResults(List<SearchResult> results);
        void onException(Exception ex);
    }
}
