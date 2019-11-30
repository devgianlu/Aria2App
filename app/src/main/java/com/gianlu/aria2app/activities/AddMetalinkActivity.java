package com.gianlu.aria2app.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.aria2app.activities.adddownload.AddBase64Bundle;
import com.gianlu.aria2app.activities.adddownload.AddDownloadBundle;
import com.gianlu.aria2app.activities.adddownload.AddMetalinkBundle;
import com.gianlu.aria2app.activities.adddownload.Base64Fragment;
import com.gianlu.aria2app.activities.adddownload.OptionsFragment;
import com.gianlu.aria2app.adapters.StatePagerAdapter;
import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.google.android.material.tabs.TabLayout;

public class AddMetalinkActivity extends AddDownloadActivity {
    private ViewPager pager;
    private OptionsFragment optionsFragment;
    private Base64Fragment base64Fragment;

    public static void startAndAdd(Context context, Uri uri) {
        context.startActivity(new Intent(context, AddMetalinkActivity.class)
                .putExtra("uri", uri));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState, @Nullable AddDownloadBundle bundle) {
        setContentView(R.layout.activity_add_download);
        setTitle(R.string.addMetalink);

        Toolbar toolbar = findViewById(R.id.addDownload_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        pager = findViewById(R.id.addDownload_pager);
        final TabLayout tabLayout = findViewById(R.id.addDownload_tabs);

        if (bundle instanceof AddMetalinkBundle) {
            base64Fragment = Base64Fragment.getInstance(this, (AddBase64Bundle) bundle);
            optionsFragment = OptionsFragment.getInstance(this, bundle);
        } else {
            base64Fragment = Base64Fragment.getInstance(this, false, getIntent().getParcelableExtra("uri"));
            optionsFragment = OptionsFragment.getInstance(this, false);
        }

        pager.setAdapter(new StatePagerAdapter<>(getSupportFragmentManager(), base64Fragment, optionsFragment));
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

    @Nullable
    @Override
    public AddDownloadBundle createBundle() {
        AnalyticsApplication.sendAnalytics(Utils.ACTION_NEW_METALINK);

        String base64;
        try {
            base64 = base64Fragment.getBase64();
        } catch (Base64Fragment.NoFileException ex) {
            pager.setCurrentItem(0, true);
            return null;
        }

        String filename = base64Fragment.getFilenameOnDevice();
        Uri fileUri = base64Fragment.getFileUri();
        if (base64 == null || filename == null || fileUri == null) {
            pager.setCurrentItem(0, true);
            return null;
        }

        return new AddMetalinkBundle(base64, filename, fileUri, optionsFragment.getPosition(), optionsFragment.getOptions());
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
            default:
                return false;
        }
    }
}
