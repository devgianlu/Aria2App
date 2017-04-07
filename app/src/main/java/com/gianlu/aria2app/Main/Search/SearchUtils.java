package com.gianlu.aria2app.Main.Search;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.NetIO.StatusCodeException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SearchUtils {
    public static final String TRENDING_WEEK = "trending-week";
    private static final String BASE_URL = "http://1337x.to";

    public static void search(final String query, final ISearch handler) {
        if (Objects.equals(query, TRENDING_WEEK)) {
            fetchTrendingThisWeek(handler);
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + "/search/" + query + "/1/").openConnection();
                    conn.connect();

                    if (conn.getResponseCode() != 200) {
                        handler.onException(new StatusCodeException(conn.getResponseCode(), conn.getResponseMessage()));
                        return;
                    }

                    String html = read(conn.getInputStream());
                    conn.disconnect();

                    Document doc = Jsoup.parse(html);
                    Element table = doc.select("div.box-info-detail table.table-list tbody").first();
                    if (table == null) {
                        handler.onResults(new ArrayList<SearchResult>());
                    } else {
                        Elements items = table.children();
                        List<SearchResult> results = new ArrayList<>();
                        for (int i = 0; i < items.size(); i++)
                            results.add(new SearchResult(items.get(i)));

                        handler.onResults(results);
                    }
                } catch (IOException ex) {
                    handler.onException(ex);
                }
            }
        }).start();
    }

    @NonNull
    private static String read(InputStream in) throws IOException {
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        String line;
        while ((line = reader.readLine()) != null)
            builder.append(line);

        reader.close();
        return builder.toString();
    }

    private static void fetchTrendingThisWeek(final ISearch handler) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + "/trending-week").openConnection();
                    conn.connect();

                    if (conn.getResponseCode() != 200) {
                        handler.onException(new StatusCodeException(conn.getResponseCode(), conn.getResponseMessage()));
                        return;
                    }

                    String html = SearchUtils.read(conn.getInputStream());
                    conn.disconnect();

                    Document doc = Jsoup.parse(html);
                    Elements items = doc.select("div.trending-torrent table.table-list tbody").first().children();

                    List<SearchResult> results = new ArrayList<>();
                    for (int i = 0; i < items.size(); i++)
                        results.add(new SearchResult(items.get(i)));

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

                    String html = read(conn.getInputStream());
                    conn.disconnect();

                    Document doc = Jsoup.parse(html);
                    Elements magnet = doc.select("a.btn.btn-magnet[href]");

                    if (magnet.isEmpty()) {
                        handler.onMagnetLink(null);
                    } else {
                        handler.onMagnetLink(magnet.first().attr("href"));
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
