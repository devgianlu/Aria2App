package com.gianlu.aria2app.Activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import com.gianlu.aria2app.NetIO.Aria2.AriaException;
import com.gianlu.aria2app.NetIO.Aria2.Download;
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithUpdate;
import com.gianlu.aria2app.NetIO.Updater.PayloadProvider;
import com.gianlu.aria2app.NetIO.Updater.Receiver;
import com.gianlu.aria2app.NetIO.Updater.UpdaterActivity;
import com.gianlu.aria2app.NetIO.Updater.UpdaterFragment;
import com.gianlu.aria2app.NetIO.Updater.Wants;
import com.gianlu.aria2app.Options.OptionsDialog;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Toaster;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

public class MoreAboutDownloadActivity extends UpdaterActivity {
    private PagerAdapter<UpdaterFragment<?>> adapter;
    private ViewPager pager;
    private Download.Status currentStatus = null;
    private Download.Status lastStatus = null;

    public static void start(Context context, @NonNull DownloadWithUpdate download) {
        context.startActivity(new Intent(context, MoreAboutDownloadActivity.class)
                .putExtra("theme", download.update().getThemeResource())
                .putExtra("title", download.update().getName())
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
        if (currentStatus == Download.Status.ERROR || currentStatus == Download.Status.COMPLETE || currentStatus == Download.Status.REMOVED) {
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
        String title = getIntent().getStringExtra("title");
        if (gid == null || theme == 0 || title == null) {
            super.onCreate(savedInstanceState);
            Toaster.with(MoreAboutDownloadActivity.this).message(R.string.failedLoading).ex(new IllegalArgumentException("gid = " + gid + ", theme = " + theme + ", title = " + title)).show();
            onBackPressed();
            return;
        }

        setTheme(theme);
        setTitle(title);

        attachReceiver(this, new Receiver<DownloadWithUpdate.BigUpdate>() {
            @Override
            public void onUpdateUi(@NonNull DownloadWithUpdate.BigUpdate payload) {
                if (lastStatus != payload.status) {
                    lastStatus = payload.status;
                    invalidateOptionsMenu();
                }
            }

            @Override
            public void onLoad(@NonNull DownloadWithUpdate.BigUpdate payload) {
                if (currentStatus == null) currentStatus = payload.status;
                setTitle(payload.getName());

                adapter = new PagerAdapter<>(getSupportFragmentManager(),
                        InfoFragment.getInstance(MoreAboutDownloadActivity.this, gid),
                        payload.isTorrent() ? PeersFragment.getInstance(MoreAboutDownloadActivity.this, gid) : ServersFragment.getInstance(MoreAboutDownloadActivity.this, gid),
                        FilesFragment.getInstance(MoreAboutDownloadActivity.this, gid));
                pager.setAdapter(adapter);
            }

            @Override
            public boolean onCouldntLoad(@NonNull Exception ex) {
                Toaster.with(MoreAboutDownloadActivity.this).message(R.string.failedLoading).ex(ex).show();
                onBackPressed();
                return false;
            }

            @Override
            public boolean onUpdateException(@NonNull Exception ex) {
                if (ex instanceof AriaException && ((AriaException) ex).isNotFound()) {
                    onBackPressed();
                    return true;
                }

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
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        String gid = getIntent().getStringExtra("gid");
        if (gid == null) return false;

        switch (item.getItemId()) {
            case R.id.moreAboutDownload_options:
                showDialog(OptionsDialog.getDownload(gid, false));
                return true;
            case R.id.moreAboutDownload_quickOptions:
                showDialog(OptionsDialog.getDownload(gid, true));
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