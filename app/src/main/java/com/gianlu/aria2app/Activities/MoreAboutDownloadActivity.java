package com.gianlu.aria2app.Activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.gianlu.aria2app.Activities.MoreAboutDownload.BackPressedFragment;
import com.gianlu.aria2app.Activities.MoreAboutDownload.FilesFragment;
import com.gianlu.aria2app.Activities.MoreAboutDownload.InfoFragment;
import com.gianlu.aria2app.Activities.MoreAboutDownload.PeersFragment;
import com.gianlu.aria2app.Activities.MoreAboutDownload.ServersFragment;
import com.gianlu.aria2app.Adapters.PagerAdapter;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.Options.OptionsUtils;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;

public class MoreAboutDownloadActivity extends AppCompatActivity {
    private PagerAdapter<BackPressedFragment> adapter;

    public static void start(Context context, Download download) {
        context.startActivity(new Intent(context, MoreAboutDownloadActivity.class).putExtra("download", download));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.more_about_download, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return true; // TODO: Hide if status doesn't allow changing options
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Download download = (Download) getIntent().getSerializableExtra("download");
        if (download == null) return false;

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.moreAboutDownload_options:
                OptionsUtils.showDownloadDialog(this, download.gid, false);
                break;
            case R.id.moreAboutDownload_quickOptions:
                OptionsUtils.showDownloadDialog(this, download.gid, true);
                break;
        }

        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Download download = (Download) getIntent().getSerializableExtra("download");
        if (download == null) {
            CommonUtils.UIToast(this, Utils.ToastMessages.FAILED_LOADING, new NullPointerException("download is null!"));
            onBackPressed();
            return;
        }

        setTheme(download.isTorrent() ? R.style.AppTheme_NoActionBar_Torrent : R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more_about_download);
        setTitle(download.getName());

        Toolbar toolbar = (Toolbar) findViewById(R.id.moreAboutDownload_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        final ViewPager pager = (ViewPager) findViewById(R.id.moreAboutDownload_pager);
        final TabLayout tabLayout = (TabLayout) findViewById(R.id.moreAboutDownload_tabs);

        adapter = new PagerAdapter<>(getSupportFragmentManager(),
                InfoFragment.getInstance(this, download),
                (download.isTorrent() ? PeersFragment.getInstance(this, download) : ServersFragment.getInstance(this, download)),
                FilesFragment.getInstance(this, download));
        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(3);

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
    public void onBackPressed() {
        if (adapter != null) {
            for (BackPressedFragment fragment : adapter.getFragments())
                if (!fragment.canGoBack())
                    return;

            for (BackPressedFragment fragment : adapter.getFragments())
                fragment.onBackPressed();
        }

        super.onBackPressed();
    }
}