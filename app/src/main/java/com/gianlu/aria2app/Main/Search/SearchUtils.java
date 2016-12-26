package com.gianlu.aria2app.Main.Search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchUtils {
    public static void search(final String query, final int page, final ISearch handler) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL("http://1337x.to/search/" + query + "/" + page + "/").openConnection();
                    conn.connect();

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

    public interface ISearch {
        void onResults(List<SearchResult> results);
        void onException(Exception ex);
    }
}
