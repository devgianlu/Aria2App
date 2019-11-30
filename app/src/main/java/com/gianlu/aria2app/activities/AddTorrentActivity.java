package com.gianlu.aria2app.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.aria2app.activities.adddownload.AddBase64Bundle;
import com.gianlu.aria2app.activities.adddownload.AddDownloadBundle;
import com.gianlu.aria2app.activities.adddownload.AddTorrentBundle;
import com.gianlu.aria2app.activities.adddownload.Base64Fragment;
import com.gianlu.aria2app.activities.adddownload.OptionsFragment;
import com.gianlu.aria2app.activities.adddownload.UrisFragment;
import com.gianlu.aria2app.adapters.StatePagerAdapter;
import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.google.android.material.tabs.TabLayout;

public class AddTorrentActivity extends AddDownloadActivity {
    private ViewPager pager;
    private UrisFragment urisFragment;
    private OptionsFragment optionsFragment;
    private Base64Fragment base64Fragment;

    public static void startAndAdd(Context context, @NonNull Uri uri) {
        context.startActivity(new Intent(context, AddTorrentActivity.class).putExtra("uri", uri));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState, @Nullable AddDownloadBundle bundle) {
        setContentView(R.layout.activity_add_download);
        setTitle(R.string.addTorrent);

        Toolbar toolbar = findViewById(R.id.addDownload_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        pager = findViewById(R.id.addDownload_pager);
        final TabLayout tabLayout = findViewById(R.id.addDownload_tabs);

        if (bundle instanceof AddTorrentBundle) {
            base64Fragment = Base64Fragment.getInstance(this, (AddBase64Bundle) bundle);
            urisFragment = UrisFragment.getInstance(this, bundle);
            optionsFragment = OptionsFragment.getInstance(this, bundle);
        } else {
            base64Fragment = Base64Fragment.getInstance(this, true, getIntent().getParcelableExtra("uri"));
            urisFragment = UrisFragment.getInstance(this, false, null);
            optionsFragment = OptionsFragment.getInstance(this, false);
        }

        pager.setAdapter(new StatePagerAdapter<>(getSupportFragmentManager(), base64Fragment, urisFragment, optionsFragment));
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
        AnalyticsApplication.sendAnalytics(Utils.ACTION_NEW_TORRENT);

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

        return new AddTorrentBundle(base64, filename, fileUri, urisFragment.getUris(), optionsFragment.getPosition(), optionsFragment.getOptions());
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
