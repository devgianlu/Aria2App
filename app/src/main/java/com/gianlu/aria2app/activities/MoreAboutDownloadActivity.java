package com.gianlu.aria2app.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.gianlu.aria2app.R;
import com.gianlu.aria2app.activities.moreabout.BigUpdateProvider;
import com.gianlu.aria2app.activities.moreabout.OnBackPressed;
import com.gianlu.aria2app.activities.moreabout.files.FilesFragment;
import com.gianlu.aria2app.activities.moreabout.info.InfoFragment;
import com.gianlu.aria2app.activities.moreabout.peers.PeersFragment;
import com.gianlu.aria2app.activities.moreabout.servers.ServersFragment;
import com.gianlu.aria2app.adapters.PagerAdapter;
import com.gianlu.aria2app.api.aria2.Aria2Helper;
import com.gianlu.aria2app.api.aria2.AriaException;
import com.gianlu.aria2app.api.aria2.Download;
import com.gianlu.aria2app.api.aria2.DownloadWithUpdate;
import com.gianlu.aria2app.api.updater.PayloadProvider;
import com.gianlu.aria2app.api.updater.Receiver;
import com.gianlu.aria2app.api.updater.UpdaterActivity;
import com.gianlu.aria2app.api.updater.UpdaterFragment;
import com.gianlu.aria2app.api.updater.Wants;
import com.gianlu.aria2app.options.OptionsDialog;
import com.gianlu.commonutils.ui.Toaster;
import com.google.android.material.tabs.TabLayout;

import org.jetbrains.annotations.NotNull;

public class MoreAboutDownloadActivity extends UpdaterActivity {
    private static final String TAG = MoreAboutDownloadActivity.class.getSimpleName();
    private PagerAdapter<UpdaterFragment<?>> adapter;
    private ViewPager pager;
    private Download.Status lastStatus = null;
    private String infoHash = null;

    public static void start(@NotNull Context context, @NonNull DownloadWithUpdate download) {
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
    public boolean onPrepareOptionsMenu(@NotNull Menu menu) {
        MenuItem options = menu.findItem(R.id.moreAboutDownload_options);
        MenuItem quick = menu.findItem(R.id.moreAboutDownload_quickOptions);
        if (lastStatus == Download.Status.ERROR || lastStatus == Download.Status.COMPLETE || lastStatus == Download.Status.REMOVED) {
            options.setVisible(false);
            quick.setVisible(false);
        } else {
            options.setVisible(true);
            quick.setVisible(true);
        }

        menu.findItem(R.id.moreAboutDownload_copyMagnet).setVisible(infoHash != null);
        return true;
    }

    @Override
    protected void onPreCreate(@Nullable Bundle savedInstanceState) {
        int theme = getIntent().getIntExtra("theme", 0);
        final String gid = getIntent().getStringExtra("gid");
        String title = getIntent().getStringExtra("title");
        if (gid == null || theme == 0 || title == null) {
            super.onCreate(savedInstanceState);
            Toaster.with(this).message(R.string.failedLoading).show();
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
                if (lastStatus == null) lastStatus = payload.status;
                setTitle(payload.getName());

                adapter = new PagerAdapter<>(getSupportFragmentManager(),
                        InfoFragment.getInstance(MoreAboutDownloadActivity.this, gid),
                        payload.isTorrent() ? PeersFragment.getInstance(MoreAboutDownloadActivity.this, gid) : ServersFragment.getInstance(MoreAboutDownloadActivity.this, gid),
                        FilesFragment.getInstance(MoreAboutDownloadActivity.this, gid));
                pager.setAdapter(adapter);

                infoHash = payload.infoHash;
                invalidateOptionsMenu();
            }

            @Override
            public boolean onCouldntLoad(@NonNull Exception ex) {
                Log.e(TAG, "Failed loading info.", ex);
                Toaster.with(MoreAboutDownloadActivity.this).message(R.string.failedLoading).show();
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
    public boolean onOptionsItemSelected(@NotNull MenuItem item) {
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
            case R.id.moreAboutDownload_copyMagnet:
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null && infoHash != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("magnet", "magnet:?xt=urn:btih:" + infoHash));
                    showToast(Toaster.build().message(R.string.copiedToClipboard));
                }
                return true;
            default:
                return false;
        }
    }

    private boolean canGoBack(int code) {
        if (adapter != null) {
            UpdaterFragment<?> fragment = adapter.getFragments().get(pager.getCurrentItem());
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
        } catch (NullPointerException | IllegalStateException ignored) {
        }
    }
}