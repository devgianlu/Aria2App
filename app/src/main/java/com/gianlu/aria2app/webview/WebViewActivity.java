package com.gianlu.aria2app.webview;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import com.gianlu.aria2app.activities.adddownload.AddUriBundle;
import com.gianlu.aria2app.activities.AddUriActivity;
import com.gianlu.aria2app.BuildConfig;
import com.gianlu.aria2app.LoadingActivity;
import com.gianlu.aria2app.PK;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.ThisApplication;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.logging.Logging;
import com.gianlu.commonutils.preferences.Prefs;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class WebViewActivity extends ActivityWithDialog {
    private static final int ADD_URI_REQUEST_CODE = 3;
    private final List<InterceptedRequest> interceptedRequests = new ArrayList<>();
    private OkHttpClient client;
    private WebView web;
    private ProgressBar progress;

    @Nullable
    private static Request buildRequest(@NonNull WebResourceRequest req) {
        String method = req.getMethod();
        if (method.equals("POST") || method.equals("PUT") || method.equals("PATCH") || method.equals("PROPPATCH") || method.equals("REPORT"))
            return null;

        String url = req.getUrl().toString();
        if (!url.startsWith("http://") && !url.startsWith("https://"))
            return null;

        Request.Builder builder = new Request.Builder().url(url);
        builder.method(req.getMethod(), null);

        for (Map.Entry<String, String> entry : req.getRequestHeaders().entrySet())
            builder.addHeader(entry.getKey(), entry.getValue());

        String cookies = CookieManager.getInstance().getCookie(url);
        if (cookies != null && !cookies.isEmpty()) builder.addHeader("Cookie", cookies);

        return builder.build();
    }

    @Nullable
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
        if (message.isEmpty()) {
            if (code == 200) message = "OK";
            else message = "UNKNOWN";
        }

        if (code > 299 && code < 400)
            return null;

        ResponseBody body = resp.body();
        return new WebResourceResponse(mimeType, resp.header("Content-Encoding"),
                code, message, headers, body == null ? null : body.byteStream());
    }

    @NonNull
    private static String guessUrl(@NonNull String url) {
        if (!url.contains("://") && !url.startsWith("http"))
            url = "http://" + url;

        return url;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_webview);
        } catch (RuntimeException ex) {
            onBackPressed();
            return;
        }

        setSupportActionBar(findViewById(R.id.webView_toolbar));

        CookieManager.getInstance().removeAllCookies(null);

        web = findViewById(R.id.webView_webView);
        progress = findViewById(R.id.webView_progress);

        setTitle(getString(R.string.webView) + " - " + getString(R.string.app_name));

        ActionBar bar = getSupportActionBar();
        if (bar != null) bar.setDisplayHomeAsUpEnabled(true);

        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setUserAgentString(settings.getUserAgentString() + " " + BuildConfig.VERSION_NAME + "-" + BuildConfig.FLAVOR);

        client = new OkHttpClient.Builder().followRedirects(true).followSslRedirects(true).build();
        web.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            synchronized (interceptedRequests) {
                for (InterceptedRequest req : interceptedRequests) {
                    if (req.matches(url)) {
                        req.contentDisposition(contentDisposition);
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

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);

                progress.setProgress(newProgress);
                if (newProgress == 100) progress.setVisibility(View.GONE);
                else progress.setVisibility(View.VISIBLE);
            }
        });

        Uri uri = getIntent().getParcelableExtra("shareData");
        if (uri != null)
            web.loadUrl(uri.toString());
        else if (Prefs.has(PK.WEBVIEW_HOMEPAGE))
            web.loadUrl(Prefs.getString(PK.WEBVIEW_HOMEPAGE, null));
        else
            showGoToDialog(true);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else customBackPressed();
    }

    private void customBackPressed() {
        if (getIntent().getBooleanExtra("canGoBack", true)) super.onBackPressed();
        else LoadingActivity.startActivity(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.web_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.webView_goTo) {
            showGoToDialog(false);
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            customBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showGoToDialog(boolean compulsory) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(R.string.webViewUrlHint);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_URI);

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.goTo)
                .setView(input)
                .setCancelable(!compulsory)
                .setNeutralButton(R.string.setAsDefault, (dialog, which) -> {
                    String text = input.getText().toString();
                    if (text.isEmpty()) {
                        Prefs.remove(PK.WEBVIEW_HOMEPAGE);
                        return;
                    }

                    Prefs.putString(PK.WEBVIEW_HOMEPAGE, guessUrl(text));
                    web.loadUrl(guessUrl(text));
                    ThisApplication.sendAnalytics(Utils.ACTION_WEBVIEW_SET_HOMEPAGE);
                })
                .setPositiveButton(R.string.visit, (dialog, which) -> web.loadUrl(guessUrl(input.getText().toString())));

        if (compulsory)
            builder.setNegativeButton(android.R.string.cancel, (d, i) -> onBackPressed());
        else
            builder.setNegativeButton(android.R.string.cancel, null);

        showDialog(builder);
    }

    private void interceptedDownload(@NonNull InterceptedRequest req) {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.isThisToDownload)
                .setMessage(getString(R.string.isThisToDownload_message, req.url()))
                .setPositiveButton(android.R.string.yes, (dialog, which) -> launchAddUri(req))
                .setNegativeButton(android.R.string.no, null);

        showDialog(builder);
        AnalyticsApplication.sendAnalytics(Utils.ACTION_INTERCEPTED_WEBVIEW);
    }

    private void launchAddUri(@NonNull InterceptedRequest req) {
        AddUriBundle bundle = AddUriBundle.fromIntercepted(req);
        if (getIntent().getBooleanExtra("startedForResult", false)) {
            startActivityForResult(new Intent(this, AddUriActivity.class)
                    .putExtra("startedForResult", true)
                    .putExtra("edit", bundle), ADD_URI_REQUEST_CODE);
        } else {
            startActivity(new Intent(this, AddUriActivity.class)
                    .putExtra("edit", bundle));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == ADD_URI_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                setResult(RESULT_OK, new Intent()
                        .putExtra("pos", data.getIntExtra("pos", -1))
                        .putExtra("bundle", data.getSerializableExtra("bundle")));
            } else {
                setResult(RESULT_CANCELED);
            }

            finish();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
