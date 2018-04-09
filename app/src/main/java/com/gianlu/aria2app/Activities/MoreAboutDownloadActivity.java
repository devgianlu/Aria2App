package com.gianlu.aria2app.Activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.gianlu.aria2app.Activities.MoreAboutDownload.Files.FilesFragment;
import com.gianlu.aria2app.Activities.MoreAboutDownload.Info.InfoFragment;
import com.gianlu.aria2app.Activities.MoreAboutDownload.OnBackPressed;
import com.gianlu.aria2app.Activities.MoreAboutDownload.Peers.PeersFragment;
import com.gianlu.aria2app.Activities.MoreAboutDownload.Servers.ServersFragment;
import com.gianlu.aria2app.Adapters.PagerAdapter;
import com.gianlu.aria2app.NetIO.Aria2.Download;
import com.gianlu.aria2app.Options.OptionsUtils;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Toaster;

public class MoreAboutDownloadActivity extends ActivityWithDialog implements InfoFragment.IStatusChanged {
    private PagerAdapter<? extends OnBackPressed> adapter;
    private ViewPager pager;
    private Download.Status currentStatus = null;

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
        if (currentStatus == Download.Status.UNKNOWN
                || currentStatus == Download.Status.ERROR
                || currentStatus == Download.Status.COMPLETE
                || currentStatus == Download.Status.REMOVED) {
            menu.findItem(R.id.moreAboutDownload_options).setVisible(false);
            menu.findItem(R.id.moreAboutDownload_quickOptions).setVisible(false);
        } else {
            menu.findItem(R.id.moreAboutDownload_options).setVisible(true);
            menu.findItem(R.id.moreAboutDownload_quickOptions).setVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Download download = (Download) getIntent().getSerializableExtra("download");
        if (download == null) return false;

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.moreAboutDownload_options:
                OptionsUtils.showDownloadDialog(this, download.gid, false);
                return true;
            case R.id.moreAboutDownload_quickOptions:
                OptionsUtils.showDownloadDialog(this, download.gid, true);
                return true;
        }

        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Download download = (Download) getIntent().getSerializableExtra("download");
        if (download == null) {
            super.onCreate(savedInstanceState);
            Toaster.show(this, Utils.Messages.FAILED_LOADING, new NullPointerException("download is null!"));
            onBackPressed();
            return;
        }

        Download.Update last = download.last();
        if (currentStatus == null) currentStatus = last.status;

        setTheme(download.isTorrent() ? R.style.AppTheme_NoActionBar_Torrent : R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more_about_download);
        setTitle(last.getName());

        Toolbar toolbar = findViewById(R.id.moreAboutDownload_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        pager = findViewById(R.id.moreAboutDownload_pager);
        final TabLayout tabLayout = findViewById(R.id.moreAboutDownload_tabs);

        adapter = new PagerAdapter<>(getSupportFragmentManager(),
                InfoFragment.getInstance(this, download, this),
                (download.isTorrent() ? PeersFragment.getInstance(this, download) : ServersFragment.getInstance(this, download)),
                FilesFragment.getInstance(this, download));
        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(3);
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_DRAGGING)
                    adapter.getFragments().get(pager.getCurrentItem()).canGoBack(OnBackPressed.CODE_CLOSE_SHEET);
            }
        });

        tabLayout.setupWithViewPager(pager);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                pager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                adapter.getFragments().get(tab.getPosition()).canGoBack(OnBackPressed.CODE_CLOSE_SHEET);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (adapter != null) {
            OnBackPressed visible = adapter.getFragments().get(pager.getCurrentItem());
            if (!visible.canGoBack(-1)) return;
            visible.onBackPressed();
        }

        try {
            super.onBackPressed();
        } catch (NullPointerException ex) {
            Logging.log(ex);
        }
    }

    @Override
    public void onStatusChanged(Download.Status newStatus) {
        currentStatus = newStatus;
        invalidateOptionsMenu();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (currentStatus == Download.Status.UNKNOWN) onBackPressed();
            }
        });
    }
}