package com.gianlu.aria2app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.gianlu.aria2app.Main.MainActivity;
import com.gianlu.aria2app.WebView.InterceptedRequest;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.commonutils.Logging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WebViewActivity extends ActivityWithDialog {
    private final List<InterceptedRequest> interceptedRequests = new ArrayList<>();
    private OkHttpClient client;

    @Nullable
    private static Request buildRequest(@NonNull WebResourceRequest req) {
        String method = req.getMethod();
        if (method.equals("POST") || method.equals("PUT") || method.equals("PATCH") || method.equals("PROPPATCH") || method.equals("REPORT"))
            return null;

        String url = req.getUrl().toString();

        Request.Builder builder = new Request.Builder().url(url);
        builder.method(req.getMethod(), null);

        for (Map.Entry<String, String> entry : req.getRequestHeaders().entrySet())
            builder.addHeader(entry.getKey(), entry.getValue());

        String cookies = CookieManager.getInstance().getCookie(url);
        if (cookies != null && !cookies.isEmpty()) builder.addHeader("Cookie", cookies);

        return builder.build();
    }

    @NonNull
    private static WebResourceResponse buildResponse(@NonNull Response resp) {
        Map<String, String> headers = new HashMap<>();
        for (int i = 0; i < resp.headers().size(); i++)
            headers.put(resp.headers().name(i), resp.headers().value(i));

        String cookies;
        if ((cookies = resp.header("Set-Cookie")) != null)
            CookieManager.getInstance().setCookie(resp.request().url().toString(), cookies);

        String contentType = resp.header("Content-Type");
        String mimeType = null;
        if (contentType != null)
            mimeType = contentType.split(";")[0];

        int code = resp.code();
        String message = resp.message();
        if (message == null || message.isEmpty()) {
            if (code == 200) message = "OK";
            else message = "UNKNOWN";
        }

        return new WebResourceResponse(mimeType, resp.header("Content-Encoding"),
                code, message, headers, resp.body() == null ? null : resp.body().byteStream());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView web = new WebView(this);
        setContentView(web);
        setTitle(getString(R.string.webView) + " - " + getString(R.string.app_name));

        Uri uri = getIntent().getParcelableExtra("shareData");
        String loadUrl = uri.toString();

        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);

        client = new OkHttpClient();
        web.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            synchronized (interceptedRequests) {
                for (InterceptedRequest req : interceptedRequests) {
                    if (req.matches(url)) {
                        interceptedDownload(req);
                        return;
                    }
                }
            }
        });

        web.setWebViewClient(new WebViewClient() {
            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Request req = buildRequest(request);
                if (req == null) return null;

                try {
                    Response resp = client.newCall(req).execute();
                    synchronized (interceptedRequests) {
                        interceptedRequests.add(InterceptedRequest.from(resp));
                    }

                    return buildResponse(resp);
                } catch (IOException ex) {
                    Logging.log(ex);
                    return null;
                }
            }
        });

        web.loadUrl(loadUrl);
    }

    private void interceptedDownload(@NonNull InterceptedRequest req) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.isThisToDownload)
                .setMessage(getString(R.string.isThisToDownload_message, req.url()))
                .setPositiveButton(android.R.string.yes, (dialog, which) -> launchMain(req))
                .setNegativeButton(android.R.string.no, null);

        showDialog(builder);
    }

    private void launchMain(@NonNull InterceptedRequest req) {
        Intent intent = new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("shareReq", req);
        startActivity(intent);
    }
}
