package com.gianlu.aria2app.WebView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import androidx.annotation.NonNull;
import okhttp3.Response;

public class InterceptedRequest implements Serializable {
    private final String url;
    private final int code;
    private final HashMap<String, String> headers;
    private final ArrayList<InterceptedRequest> prior;

    private InterceptedRequest(String url, int code, HashMap<String, String> headers, ArrayList<InterceptedRequest> prior) {
        this.url = url;
        this.code = code;
        this.headers = headers;
        this.prior = prior;
    }

    @NonNull
    public static InterceptedRequest from(@NonNull Response resp) {
        HashMap<String, String> headers = new HashMap<>();
        for (int i = 0; i < resp.headers().size(); i++)
            headers.put(resp.headers().name(i), resp.headers().value(i));

        ArrayList<InterceptedRequest> prior = new ArrayList<>();
        handlePriorResponse(resp, prior);

        return new InterceptedRequest(resp.request().url().toString(), resp.code(), headers, prior);
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
}
