package com.gianlu.aria2app.Activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.gianlu.aria2app.Activities.AddDownload.Base64Fragment;
import com.gianlu.aria2app.Activities.AddDownload.OptionsFragment;
import com.gianlu.aria2app.Activities.EditProfile.InvalidFieldException;
import com.gianlu.aria2app.Adapters.PagerAdapter;
import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.AriaRequests;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.gianlu.commonutils.Toaster;

import org.json.JSONException;

import java.util.Map;

public class AddMetalinkActivity extends ActivityWithDialog {
    private ViewPager pager;
    private OptionsFragment optionsFragment;
    private Base64Fragment base64Fragment;

    public static void startAndAdd(Context context, Uri uri) {
        context.startActivity(new Intent(context, AddMetalinkActivity.class).putExtra("uri", uri));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_download);
        setTitle(R.string.addMetalink);

        Toolbar toolbar = findViewById(R.id.addDownload_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        pager = findViewById(R.id.addDownload_pager);
        final TabLayout tabLayout = findViewById(R.id.addDownload_tabs);

        base64Fragment = Base64Fragment.getInstance(this, false, (Uri) getIntent().getParcelableExtra("uri"));
        optionsFragment = OptionsFragment.getInstance(this, false);

        pager.setAdapter(new PagerAdapter<>(getSupportFragmentManager(), base64Fragment, optionsFragment));
        pager.setOffscreenPageLimit(2);

        tabLayout.setupWithViewPager(pager);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                pager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_download, menu);
        return true;
    }

    private void done() {
        String base64 = null;
        try {
            base64 = base64Fragment.getBase64();
        } catch (InvalidFieldException ex) {
            if (ex.fragmentClass == Base64Fragment.class) {
                pager.setCurrentItem(0, true);
                return;
            }
        }

        if (base64 == null) {
            pager.setCurrentItem(0, true);
            return;
        }

        Map<String, String> options = optionsFragment.getOptions();
        Integer position = optionsFragment.getPosition();

        try {
            showDialog(DialogUtils.progressDialog(this, R.string.gathering_information));
            Aria2Helper.instantiate(this).request(AriaRequests.addMetalink(base64, position, options), new AbstractClient.OnResult<String>() {
                @Override
                public void onResult(@NonNull String result) {
                    dismissDialog();
                    Toaster.with(AddMetalinkActivity.this).message(R.string.downloadAdded).extra(result).show();
                    onBackPressed();
                }

                @Override
                public void onException(Exception ex, boolean shouldForce) {
                    dismissDialog();
                    Toaster.with(AddMetalinkActivity.this).message(R.string.failedAddingDownload).ex(ex).show();
                }
            });
        } catch (Aria2Helper.InitializingException | JSONException ex) {
            Toaster.with(this).message(R.string.failedAddingDownload).ex(ex).show();
        }

        AnalyticsApplication.sendAnalytics(AddMetalinkActivity.this, Utils.ACTION_NEW_METALINK);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.addDownload_done:
                done();
                break;
        }

        return true;
    }
}
