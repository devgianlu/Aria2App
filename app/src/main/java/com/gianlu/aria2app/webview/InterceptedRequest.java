package com.gianlu.aria2app.webview;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.Response;

public final class InterceptedRequest implements Serializable {
    private static final Pattern FILENAME_PATTERN = Pattern.compile("filename=\"(.*)\"");
    private final String url;
    private final HashMap<String, String> headers;
    private final ArrayList<InterceptedRequest> prior;
    private String filename;

    private InterceptedRequest(String url, HashMap<String, String> headers, ArrayList<InterceptedRequest> prior) {
        this.url = url;
        this.headers = headers;
        this.prior = prior;
    }

    @NonNull
    public static InterceptedRequest from(@NonNull Response resp) {
        HashMap<String, String> headers = new HashMap<>();
        Headers reqHeaders = resp.request().headers();
        for (int i = 0; i < reqHeaders.size(); i++)
            headers.put(reqHeaders.name(i), reqHeaders.value(i));

        ArrayList<InterceptedRequest> prior = new ArrayList<>();
        handlePriorResponse(resp, prior);

        return new InterceptedRequest(resp.request().url().toString(), headers, prior);
    }

    private static void handlePriorResponse(Response resp, List<InterceptedRequest> list) {
        Response prior = resp.priorResponse();
        if (prior != null) {
            list.add(InterceptedRequest.from(prior));
            handlePriorResponse(prior, list);
        }
    }

    @NonNull
    public String url() {
        return url;
    }

    public boolean matches(@NonNull String url) {
        if (Objects.equals(this.url, url))
            return true;

        for (InterceptedRequest prior : this.prior)
            if (prior.matches(url))
                return true;

        return false;
    }

    @NonNull
    public Map<String, String> headers() {
        return headers;
    }

    public void contentDisposition(@Nullable String header) {
        if (header == null || header.isEmpty()) return;

        Matcher matcher = FILENAME_PATTERN.matcher(header);
        if (matcher.find()) filename = matcher.group(1);
    }

    @Nullable
    public String filename() {
        return filename;
    }
}
