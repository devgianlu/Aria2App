package com.gianlu.aria2app.Activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithHelper;
import com.gianlu.aria2app.NetIO.Updater.BaseUpdater;
import com.gianlu.aria2app.NetIO.Updater.UpdaterActivity;
import com.gianlu.aria2app.Options.OptionsUtils;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Toaster;

public class MoreAboutDownloadActivity extends UpdaterActivity<DownloadWithHelper> implements InfoFragment.OnStatusChanged {
    private PagerAdapter<? extends OnBackPressed> adapter;
    private ViewPager pager;
    private Download.Status currentStatus = null;

    public static void start(Context context, @NonNull Download download) {
        context.startActivity(new Intent(context, MoreAboutDownloadActivity.class)
                .putExtra("theme", download.isTorrent() ? R.style.AppTheme_NoActionBar_Torrent : R.style.AppTheme_NoActionBar)
                .putExtra("gid", download.gid));
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
    protected void onPreCreate(@Nullable Bundle savedInstanceState) {
        int theme = getIntent().getIntExtra("theme", 0);
        if (!getIntent().hasExtra("gid") || theme == 0) {
            super.onCreate(savedInstanceState);
            Toaster.show(this, Utils.Messages.FAILED_LOADING, new IllegalArgumentException("missing gid or theme = " + theme));
            onBackPressed();
            return;
        }

        setTheme(theme);
    }

    @Override
    protected void onPostCreate() {
        setContentView(R.layout.activity_more_about_download);

        Toolbar toolbar = findViewById(R.id.moreAboutDownload_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        pager = findViewById(R.id.moreAboutDownload_pager);
        final TabLayout tabLayout = findViewById(R.id.moreAboutDownload_tabs);

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
                if (adapter != null && state == ViewPager.SCROLL_STATE_DRAGGING)
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
    protected void onLoad(@NonNull DownloadWithHelper payload) { // FIXME
        Download.SmallUpdate last = payload.get().lastBig();
        if (currentStatus == null) currentStatus = last.status;

        setTitle(last.getName());

        adapter = new PagerAdapter<>(getSupportFragmentManager(),
                InfoFragment.getInstance(this, payload.get()),
                payload.get().isTorrent() ? PeersFragment.getInstance(this, payload.get()) : ServersFragment.getInstance(this, payload.get()),
                FilesFragment.getInstance(this, payload.get()));
        pager.setAdapter(adapter);
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

    @NonNull
    @Override
    public BaseUpdater<DownloadWithHelper> createUpdater(@NonNull Bundle args) throws Exception {
        return new Updater(this, args.getString("gid"), this);
    }

    @Override
    public void onUpdateUi(@NonNull DownloadWithHelper payload) {
        // TODO
        // TODO: Should broadcast to fragments, remove fragment updaters
    }

    @Override
    public void onCouldntLoad(@NonNull Exception ex) {
        // TODO
    }
}