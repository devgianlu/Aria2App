package com.gianlu.aria2app.WebView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;

import com.gianlu.aria2app.Activities.AddDownload.AddUriBundle;
import com.gianlu.aria2app.Activities.AddUriActivity;
import com.gianlu.aria2app.R;
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
    private static final int ADD_URI_REQUEST_CODE = 3;
    private final List<InterceptedRequest> interceptedRequests = new ArrayList<>();
    private OkHttpClient client;
    private WebView web;

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

        web = new WebView(this);
        setContentView(web);
        setTitle(getString(R.string.webView) + " - " + getString(R.string.app_name));

        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);

        client = new OkHttpClient();
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

        Uri uri = getIntent().getParcelableExtra("shareData");
        if (uri != null) web.loadUrl(uri.toString());
        else showGoToDialog(true);
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
        }

        return super.onOptionsItemSelected(item);
    }

    private void showGoToDialog(boolean compulsory) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("https://example.com");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_URI);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.goTo)
                .setView(input)
                .setCancelable(!compulsory)
                .setPositiveButton(R.string.visit, (dialog, which) -> web.loadUrl(input.getText().toString()));

        if (compulsory)
            builder.setNegativeButton(android.R.string.cancel, (d, i) -> onBackPressed());
        else
            builder.setNegativeButton(android.R.string.cancel, null);

        showDialog(builder);
    }

    private void interceptedDownload(@NonNull InterceptedRequest req) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.isThisToDownload)
                .setMessage(getString(R.string.isThisToDownload_message, req.url()))
                .setPositiveButton(android.R.string.yes, (dialog, which) -> launchAddUri(req))
                .setNegativeButton(android.R.string.no, null);

        showDialog(builder);
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
