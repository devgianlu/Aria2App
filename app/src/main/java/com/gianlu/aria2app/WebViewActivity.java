package com.gianlu.aria2app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.gianlu.aria2app.Main.MainActivity;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

public class WebViewActivity extends ActivityWithDialog {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView web = new WebView(this);
        setContentView(web);
        setTitle(getString(R.string.webView) + " - " + getString(R.string.app_name));

        Uri uri = getIntent().getParcelableExtra("shareData");
        String loadUrl = uri.toString();

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return true;
            }

            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (request.hasGesture() && request.isForMainFrame()) {
                    Uri url = request.getUrl();
                    if (!loadUrl.equals(url.toString()))
                        interceptedPage(url);
                }

                return null;
            }
        });

        web.loadUrl(loadUrl);
    }

    private void interceptedPage(@NonNull Uri url) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.isThisToDownload)
                .setMessage(getString(R.string.isThisToDownload_message, url))
                .setPositiveButton(android.R.string.yes, (dialog, which) -> launchMain(url))
                .setNegativeButton(android.R.string.no, null);

        showDialog(builder);
    }

    private void launchMain(@NonNull Uri url) {
        Intent intent = new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("shareData", url);
        startActivity(intent);
    }
}
