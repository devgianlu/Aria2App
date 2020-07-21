package com.gianlu.aria2app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.aria2app.R;
import com.gianlu.aria2app.activities.adddownload.AddDownloadBundle;
import com.gianlu.aria2app.api.AbstractClient;
import com.gianlu.aria2app.api.AriaRequests;
import com.gianlu.aria2app.api.aria2.Aria2Helper;
import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.ui.Toaster;

import org.json.JSONException;

public abstract class AddDownloadActivity extends ActivityWithDialog {
    private boolean startedForResult = false;
    private int startedForEdit = -1;

    @Override
    @CallSuper
    protected final void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startedForResult = getIntent().getBooleanExtra("startedForResult", false);
        startedForEdit = getIntent().getIntExtra("startedForEdit", -1);
        if (startedForEdit >= 0) startedForResult = true;
        onCreate(savedInstanceState, (AddDownloadBundle) getIntent().getSerializableExtra("edit"));
    }

    protected abstract void onCreate(@Nullable Bundle savedInstanceState, @Nullable AddDownloadBundle bundle);

    @Nullable
    public abstract AddDownloadBundle createBundle();

    private static final String TAG = AddDownloadActivity.class.getSimpleName();

    protected final void done() {
        AddDownloadBundle bundle = createBundle();

        if (startedForResult) {
            if (bundle == null) setResult(RESULT_CANCELED, null);
            else setResult(RESULT_OK, new Intent()
                    .putExtra("pos", startedForEdit)
                    .putExtra("bundle", bundle));
            onBackPressed();
        } else {
            if (bundle == null) return;

            try {
                showProgress(R.string.gathering_information);
                Aria2Helper.instantiate(this).request(AriaRequests.addDownload(bundle), new AbstractClient.OnResult<String>() {
                    @Override
                    public void onResult(@NonNull String result) {
                        dismissDialog();
                        Toaster.with(AddDownloadActivity.this).message(R.string.downloadAdded).extra(result).show();
                        if (!isDestroyed()) onBackPressed();
                    }

                    @Override
                    public void onException(@NonNull Exception ex) {
                        dismissDialog();
                        Log.e(TAG, "Failed adding download.", ex);
                        Toaster.with(AddDownloadActivity.this).message(R.string.failedAddingDownload).show();
                    }
                });
            } catch (Aria2Helper.InitializingException | JSONException ex) {
                Log.e(TAG, "Failed initializing/parsing.", ex);
                Toaster.with(this).message(R.string.failedAddingDownload).show();
            }
        }
    }
}
