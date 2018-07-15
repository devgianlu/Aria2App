package com.gianlu.aria2app.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.gianlu.aria2app.Activities.AddDownload.AddDownloadBundle;
import com.gianlu.aria2app.Adapters.AddDownloadBundlesAdapter;
import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.commonutils.Toaster;

import java.util.List;

// TODO: More practical ways to add downloads
public class BatchAddActivity extends ActivityWithDialog implements AddDownloadBundlesAdapter.Listener {
    private final static int REQUEST_URI = 0;
    private final static int REQUEST_TORRENT = 1;
    private final static int REQUEST_METALINK = 2;
    private AddDownloadBundlesAdapter adapter;
    private RecyclerViewLayout layout;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_download, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.addDownload_done:
                done();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void done() {
        List<AddDownloadBundle> bundles = adapter.getBundles();
        if (bundles.isEmpty()) return;

        Bundle analytics = new Bundle();
        analytics.putInt("bundles", bundles.size());
        AnalyticsApplication.sendAnalytics(this, Utils.ACTION_NEW_BATCH, analytics);

        try {
            showDialog(DialogUtils.progressDialog(this, R.string.gathering_information));
            Aria2Helper.instantiate(this).addDownloads(bundles, new AbstractClient.OnResult<List<String>>() {
                @Override
                public void onResult(@NonNull List<String> result) {
                    dismissDialog();
                    Toaster.with(BatchAddActivity.this).message(R.string.downloadsAddedBatch).extra(result).show();
                    if (!isDestroyed()) onBackPressed();
                }

                @Override
                public void onException(Exception ex, boolean shouldForce) {
                    dismissDialog();
                    Toaster.with(BatchAddActivity.this).message(R.string.failedAddingDownloads).ex(ex).show();
                }
            });
        } catch (Aria2Helper.InitializingException ex) {
            Toaster.with(this).message(R.string.failedAddingDownload).ex(ex).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_add);
        setTitle(R.string.batchAdd);

        Button singleUri = findViewById(R.id.batchAdd_singleUri);
        singleUri.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(BatchAddActivity.this, AddUriActivity.class).putExtra("startedForResult", true), REQUEST_URI);
            }
        });
        Button singleTorrent = findViewById(R.id.batchAdd_singleTorrent);
        singleTorrent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(BatchAddActivity.this, AddTorrentActivity.class).putExtra("startedForResult", true), REQUEST_TORRENT);
            }
        });
        Button singleMetalink = findViewById(R.id.batchAdd_singleMetalink);
        singleMetalink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(BatchAddActivity.this, AddMetalinkActivity.class).putExtra("startedForResult", true), REQUEST_METALINK);
            }
        });

        layout = findViewById(R.id.batchAdd_list);
        layout.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        adapter = new AddDownloadBundlesAdapter(this, this);
        layout.loadListData(adapter, false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null)
            adapter.addItem((AddDownloadBundle) data.getSerializableExtra("bundle"));

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onItemCountUpdated(int count) {
        if (count == 0) layout.showInfo(R.string.noBatchAdd);
        else layout.showList();
    }
}
