package com.gianlu.aria2app.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.aria2app.activities.adddownload.AddDownloadBundle;
import com.gianlu.aria2app.activities.adddownload.AddUriBundle;
import com.gianlu.aria2app.activities.adddownload.OptionsFragment;
import com.gianlu.aria2app.activities.adddownload.UrisFragment;
import com.gianlu.aria2app.adapters.StatePagerAdapter;
import com.gianlu.aria2app.api.aria2.OptionsMap;
import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.commonutils.ui.Toaster;
import com.google.android.material.tabs.TabLayout;

import java.net.URI;
import java.util.ArrayList;

public class AddUriActivity extends AddDownloadActivity {
    private UrisFragment urisFragment;
    private OptionsFragment optionsFragment;
    private ViewPager pager;

    public static void startAndAdd(Context context, URI uri) {
        context.startActivity(new Intent(context, AddUriActivity.class).putExtra("uri", uri));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState, @Nullable AddDownloadBundle bundle) {
        setContentView(R.layout.activity_add_download);
        setTitle(R.string.addUri);

        Toolbar toolbar = findViewById(R.id.addDownload_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        pager = findViewById(R.id.addDownload_pager);
        final TabLayout tabLayout = findViewById(R.id.addDownload_tabs);

        if (bundle instanceof AddUriBundle) {
            urisFragment = UrisFragment.getInstance(this, bundle);
            optionsFragment = OptionsFragment.getInstance(this, bundle);
        } else {
            urisFragment = UrisFragment.getInstance(this, true, (URI) getIntent().getSerializableExtra("uri"));
            optionsFragment = OptionsFragment.getInstance(this, true);
        }

        pager.setAdapter(new StatePagerAdapter<>(getSupportFragmentManager(), urisFragment, optionsFragment));
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
        AnalyticsApplication.sendAnalytics(Utils.ACTION_NEW_URI);

        ArrayList<String> uris = urisFragment.getUris();
        if (uris == null || uris.isEmpty()) {
            Toaster.with(this).message(R.string.noUris).show();
            pager.setCurrentItem(0, true);
            return null;
        }

        OptionsMap options = optionsFragment.getOptions();
        String filename = optionsFragment.getFilename();
        if (filename != null) options.put("out", filename);
        return new AddUriBundle(uris, optionsFragment.getPosition(), options);
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
