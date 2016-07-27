package com.gianlu.jtitan.JTRequester;

import android.util.Base64;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class JTRequester {
    private String globalUrl;
    private String username;
    private String password;

    public JTRequester(String url, String username, String password) {
        globalUrl = url;
        this.username = username;
        this.password = password;
    }

    public JTResponse send(String req) throws IOException {
        //Create connection
        HttpURLConnection conn = (HttpURLConnection) new URL(globalUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("User-Agent", "Aria2App");
        conn.setConnectTimeout(5000);

        // TODO: Check that
        if (username != null && password != null)
            conn.setRequestProperty("Authorization",
                    "Basic " + Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP));


        //Send data
        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        os.write(req.getBytes());
        os.flush();
        os.close();
        int respCode = conn.getResponseCode();


        if (respCode != 200) {
            System.out.println("FAILED REQUEST: " + req);
        }

        //Read response
        BufferedReader responseReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder responseBuilder = new StringBuilder();
        String inputLine;

        while ((inputLine = responseReader.readLine()) != null) {
            responseBuilder.append(inputLine);
        }
        responseReader.close();

        conn.disconnect();
        return new JTResponse(respCode, responseBuilder.toString());
    }
}
