package com.gianlu.jtitan.JTRequester;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class JTRequester {
    private String globalUrl;

    public JTRequester(String url) {
        globalUrl = url;
    }

    public JTResponse send(String req) throws IOException {
        URL reqURL;
        HttpURLConnection conn;
        int respCode;
        BufferedReader responseReader;
        String response;

        reqURL = new URL(globalUrl);

        //Create connection
        conn = (HttpURLConnection) reqURL.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("User-Agent", "Aria2App");
        conn.setConnectTimeout(5000);

        //Send data
        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        os.write(req.getBytes());
        os.flush();
        os.close();
        respCode = conn.getResponseCode();


        if (respCode != 200) {
            System.out.println("FAILED REQUEST: " + req);
        }

        //Read response
        responseReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder responseBuilder = new StringBuilder();
        String inputLine;

        while ((inputLine = responseReader.readLine()) != null) {
            responseBuilder.append(inputLine);
        }
        responseReader.close();

        response = responseBuilder.toString();

        conn.disconnect();
        return new JTResponse(respCode, response);
    }
}
