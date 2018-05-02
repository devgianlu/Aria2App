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

import com.gianlu.aria2app.Activities.MoreAboutDownload.BigUpdateProvider;
import com.gianlu.aria2app.Activities.MoreAboutDownload.Files.FilesFragment;
import com.gianlu.aria2app.Activities.MoreAboutDownload.Info.InfoFragment;
import com.gianlu.aria2app.Activities.MoreAboutDownload.OnBackPressed;
import com.gianlu.aria2app.Activities.MoreAboutDownload.Peers.PeersFragment;
import com.gianlu.aria2app.Activities.MoreAboutDownload.Servers.ServersFragment;
import com.gianlu.aria2app.Adapters.PagerAdapter;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.Download;
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithUpdate;
import com.gianlu.aria2app.NetIO.Updater.PayloadProvider;
import com.gianlu.aria2app.NetIO.Updater.Receiver;
import com.gianlu.aria2app.NetIO.Updater.UpdaterActivity;
import com.gianlu.aria2app.NetIO.Updater.UpdaterFragment;
import com.gianlu.aria2app.NetIO.Updater.Wants;
import com.gianlu.aria2app.Options.OptionsUtils;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Toaster;

public class MoreAboutDownloadActivity extends UpdaterActivity {
    private PagerAdapter<UpdaterFragment<?>> adapter;
    private ViewPager pager;
    private Download.Status currentStatus = null;
    private Download.Status lastStatus = Download.Status.UNKNOWN;

    public static void start(Context context, @NonNull DownloadWithUpdate download) {
        context.startActivity(new Intent(context, MoreAboutDownloadActivity.class)
                .putExtra("theme", download.update().isTorrent() ? R.style.AppTheme_NoActionBar_Torrent : R.style.AppTheme_NoActionBar)
                .putExtra("gid", download.gid));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.more_about_download, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem options = menu.findItem(R.id.moreAboutDownload_options);
        MenuItem quick = menu.findItem(R.id.moreAboutDownload_quickOptions);
        if (currentStatus == Download.Status.UNKNOWN || currentStatus == Download.Status.ERROR || currentStatus == Download.Status.COMPLETE || currentStatus == Download.Status.REMOVED) {
            options.setVisible(false);
            quick.setVisible(false);
        } else {
            options.setVisible(true);
            quick.setVisible(true);
        }

        return true;
    }

    @Override
    protected void onPreCreate(@Nullable Bundle savedInstanceState) {
        int theme = getIntent().getIntExtra("theme", 0);
        final String gid = getIntent().getStringExtra("gid");
        if (gid == null || theme == 0) {
            super.onCreate(savedInstanceState);
            Toaster.show(this, Utils.Messages.FAILED_LOADING, new IllegalArgumentException("gid = " + gid + ", theme = " + theme));
            onBackPressed();
            return;
        }

        setTheme(theme);

        attachReceiver(this, new Receiver<DownloadWithUpdate.BigUpdate>() {
            @Override
            public void onUpdateUi(@NonNull DownloadWithUpdate.BigUpdate payload) {
                if (lastStatus != payload.status) {
                    lastStatus = payload.status;
                    invalidateOptionsMenu();
                }

                if (payload.status == Download.Status.UNKNOWN) onBackPressed();
            }

            @Override
            public void onLoad(@NonNull DownloadWithUpdate.BigUpdate payload) {
                if (currentStatus == null) currentStatus = payload.status;
                setTitle(payload.getName());

                String gid = payload.download().gid;

                adapter = new PagerAdapter<UpdaterFragment<?>>(getSupportFragmentManager(),
                        InfoFragment.getInstance(MoreAboutDownloadActivity.this, gid),
                        payload.isTorrent() ? PeersFragment.getInstance(MoreAboutDownloadActivity.this, gid) : ServersFragment.getInstance(MoreAboutDownloadActivity.this, gid),
                        FilesFragment.getInstance(MoreAboutDownloadActivity.this, gid));
                pager.setAdapter(adapter);
            }

            @Override
            public void onCouldntLoad(@NonNull Exception ex) {
                Toaster.show(MoreAboutDownloadActivity.this, Utils.Messages.FAILED_LOADING, ex);
                onBackPressed();
            }

            @Override
            public boolean onUpdateException(@NonNull Exception ex) {
                return false;
            }

            @NonNull
            @Override
            public Wants<DownloadWithUpdate.BigUpdate> wants() {
                return Wants.bigUpdate(gid);
            }

            @NonNull
            @Override
            public PayloadProvider<DownloadWithUpdate.BigUpdate> requireProvider() throws Aria2Helper.InitializingException {
                return new BigUpdateProvider(MoreAboutDownloadActivity.this, getIntent().getStringExtra("gid"));
            }
        });
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
                if (state == ViewPager.SCROLL_STATE_DRAGGING)
                    canGoBack(OnBackPressed.CODE_CLOSE_SHEET);
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
                canGoBack(OnBackPressed.CODE_CLOSE_SHEET);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String gid = getIntent().getStringExtra("gid");
        if (gid == null) return false;

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.moreAboutDownload_options:
                OptionsUtils.showDownloadDialog(this, gid, false);
                return true;
            case R.id.moreAboutDownload_quickOptions:
                OptionsUtils.showDownloadDialog(this, gid, true);
                return true;
        }

        return false;
    }

    private boolean canGoBack(int code) {
        if (adapter != null) {
            UpdaterFragment fragment = adapter.getFragments().get(pager.getCurrentItem());
            if (fragment instanceof OnBackPressed)
                return ((OnBackPressed) fragment).canGoBack(code);
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        if (!canGoBack(-1)) return;

        try {
            super.onBackPressed();
        } catch (NullPointerException ex) {
            Logging.log(ex);
        }
    }
}