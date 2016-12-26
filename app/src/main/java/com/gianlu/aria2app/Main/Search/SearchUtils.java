package com.gianlu.aria2app.Main.Search;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.gianlu.aria2app.NetIO.StatusCodeException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchUtils {
    public static void retrieveWebsiteLogo(final ILogo handler) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL("http://1337x.to").openConnection();
                    conn.connect();

                    if (conn.getResponseCode() != 200) {
                        handler.onException(new StatusCodeException(conn.getResponseCode(), conn.getResponseMessage()));
                        return;
                    }

                    String html = "";
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        html += line;
                    }

                    reader.close();
                    Matcher matcher = Pattern.compile("<div\\sclass=\"logo\"><a\\s.*?><img\\s.*?src=\"(.*)\"></a></div>").matcher(html);

                    if (matcher.find()) {
                        conn.disconnect();

                        HttpURLConnection logoConn = (HttpURLConnection) new URL("http://1337x.to/" + matcher.group(1)).openConnection();
                        logoConn.connect();

                        if (logoConn.getResponseCode() != 200) {
                            handler.onException(new StatusCodeException(logoConn.getResponseCode(), logoConn.getResponseMessage()));
                            return;
                        }

                        // FIXME: Not rendering well
                        SVG logo = SVG.getFromInputStream(logoConn.getInputStream());
                        logoConn.disconnect();

                        handler.onLogo(new PictureDrawable(logo.renderToPicture()));
                    } else {
                        handler.onException(new NullPointerException("<div> tag not found!"));
                    }
                } catch (IOException | SVGParseException ex) {
                    handler.onException(ex);
                }
            }
        }).start();
    }

    public interface ILogo {
        void onLogo(Drawable logo);

        void onException(Exception ex);
    }
}
