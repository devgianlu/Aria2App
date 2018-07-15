package com.gianlu.aria2app.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.Activities.AddDownload.AddDownloadBundle;
import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.AriaRequests;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.gianlu.commonutils.Toaster;

import org.json.JSONException;

public abstract class AddDownloadActivity extends ActivityWithDialog {
    private boolean startedForResult = false;

    @Override
    @CallSuper
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startedForResult = getIntent().getBooleanExtra("startedForResult", false);
    }

    @Nullable
    public abstract AddDownloadBundle createBundle();

    protected final void done() {
        AddDownloadBundle bundle = createBundle();

        // TODO: Analytics
        // AnalyticsApplication.sendAnalytics(AddTorrentActivity.this, Utils.ACTION_NEW_TORRENT);
        // AnalyticsApplication.sendAnalytics(AddUriActivity.this, Utils.ACTION_NEW_URI);
        // AnalyticsApplication.sendAnalytics(AddMetalinkActivity.this, Utils.ACTION_NEW_METALINK);

        if (startedForResult) {
            if (bundle == null) setResult(RESULT_CANCELED, null);
            else setResult(RESULT_OK, new Intent().putExtra("bundle", bundle));
            onBackPressed();
        } else {
            if (bundle == null) return;

            try {
                showDialog(DialogUtils.progressDialog(this, R.string.gathering_information));
                Aria2Helper.instantiate(this).request(AriaRequests.addDownload(bundle), new AbstractClient.OnResult<String>() {
                    @Override
                    public void onResult(@NonNull String result) {
                        dismissDialog();
                        Toaster.with(AddDownloadActivity.this).message(R.string.downloadAdded).extra(result).show();
                        if (!isDestroyed()) onBackPressed();
                    }

                    @Override
                    public void onException(Exception ex, boolean shouldForce) {
                        dismissDialog();
                        Toaster.with(AddDownloadActivity.this).message(R.string.failedAddingDownload).ex(ex).show();
                    }
                });
            } catch (Aria2Helper.InitializingException | JSONException ex) {
                Toaster.with(this).message(R.string.failedAddingDownload).ex(ex).show();
            }
        }
    }
}
