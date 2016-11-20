package com.gianlu.aria2app;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.gianlu.aria2app.Google.Analytics;
import com.gianlu.aria2app.MoreAboutDownload.CommonFragment;
import com.gianlu.aria2app.MoreAboutDownload.FilesFragment.FilesPagerFragment;
import com.gianlu.aria2app.MoreAboutDownload.InfoFragment.InfoPagerFragment;
import com.gianlu.aria2app.MoreAboutDownload.InfoFragment.UpdateUI;
import com.gianlu.aria2app.MoreAboutDownload.PagerAdapter;
import com.gianlu.aria2app.MoreAboutDownload.PeersFragment.PeersPagerFragment;
import com.gianlu.aria2app.MoreAboutDownload.ServersFragment.ServersPagerFragment;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.commonutils.CommonUtils;
import com.google.android.gms.analytics.HitBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MoreAboutDownloadActivity extends AppCompatActivity {
    private PagerAdapter adapter;
    private String gid;
    private final Utils.IOptionsDialog optionsChanged = new Utils.IOptionsDialog() {
        @Override
        public void onApply(JTA2 jta2, Map<String, String> options) {
            if (options.entrySet().size() == 0) return;

            final ProgressDialog pd = CommonUtils.fastIndeterminateProgressDialog(MoreAboutDownloadActivity.this, R.string.gathering_information);
            CommonUtils.showDialog(MoreAboutDownloadActivity.this, pd);

            if (Analytics.isTrackingAllowed(MoreAboutDownloadActivity.this))
                Analytics.getDefaultTracker(MoreAboutDownloadActivity.this.getApplication()).send(new HitBuilders.EventBuilder()
                        .setCategory(Analytics.CATEGORY_USER_INPUT)
                        .setAction(Analytics.ACTION_CHANGED_DOWNLOAD_OPTIONS)
                        .build());

            jta2.changeOption(gid, options, new JTA2.ISuccess() {
                @Override
                public void onSuccess() {
                    pd.dismiss();
                    CommonUtils.UIToast(MoreAboutDownloadActivity.this, Utils.ToastMessages.DOWNLOAD_OPTIONS_CHANGED);
                }

                @Override
                public void onException(Exception exception) {
                    pd.dismiss();
                    CommonUtils.UIToast(MoreAboutDownloadActivity.this, Utils.ToastMessages.FAILED_CHANGE_OPTIONS, exception);
                }
            });
        }
    };
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(getIntent().getBooleanExtra("isTorrent", false) ? R.style.AppTheme_NoActionBar_Torrent : R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more_about_download);
        setTitle(getIntent().getStringExtra("name"));

        gid = getIntent().getStringExtra("gid");

        Toolbar toolbar = (Toolbar) findViewById(R.id.moreAboutDownload_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);

        final ViewPager pager = (ViewPager) findViewById(R.id.moreAboutDownload_pager);
        final TabLayout tabLayout = (TabLayout) findViewById(R.id.moreAboutDownload_tabs);

        final List<CommonFragment> fragments = new ArrayList<>();
        fragments.add(InfoPagerFragment.newInstance(getString(R.string.info), gid).setObserver(new UpdateUI.IDownloadObserver() {
            @Override
            public void onDownloadStatusChanged(Download.STATUS newStatus) {
                if (menu == null) return;

                switch (newStatus) {
                    case ACTIVE:
                    case PAUSED:
                    case WAITING:
                        menu.findItem(R.id.moreAboutDownloadMenu_options).setVisible(true);
                        menu.findItem(R.id.moreAboutDownloadMenu_quickOptions).setVisible(true);
                        break;
                    case REMOVED:
                    case ERROR:
                    case COMPLETE:
                    case UNKNOWN:
                    default:
                        menu.findItem(R.id.moreAboutDownloadMenu_options).setVisible(false);
                        menu.findItem(R.id.moreAboutDownloadMenu_quickOptions).setVisible(false);
                        break;
                }
            }

            @Override
            public void onReportDownloadPieces(int numPieces) {
                if (numPieces >= 2000 && menu.findItem(R.id.moreAboutDownloadMenu_bitfield).isChecked())
                    onOptionsItemSelected(menu.findItem(R.id.moreAboutDownloadMenu_bitfield));
            }
        }));

        if (getIntent().getBooleanExtra("isTorrent", false))
            fragments.add(PeersPagerFragment.newInstance(getString(R.string.peers), gid));
        else
            fragments.add(ServersPagerFragment.newInstance(getString(R.string.servers), gid));

        fragments.add(FilesPagerFragment.newInstance(getString(R.string.files), gid));

        adapter = new PagerAdapter(getSupportFragmentManager(), fragments);
        pager.setAdapter(adapter);

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
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.more_about_download, menu);
        this.menu = menu;
        menu.findItem(R.id.moreAboutDownloadMenu_bitfield).setChecked(PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean("a2_showBitfield", true));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.moreAboutDownloadMenu_options:
                Utils.showOptionsDialog(this, gid, R.array.downloadOptions, false, true, optionsChanged);
                break;
            case R.id.moreAboutDownloadMenu_quickOptions:
                Utils.showOptionsDialog(this, gid, R.array.downloadOptions, true, true, optionsChanged);
                break;
            case R.id.moreAboutDownloadMenu_bitfield:
                item.setChecked(!item.isChecked());
                ((InfoPagerFragment) adapter.getItem(0)).setBitfieldVisibility(item.isChecked());

                PreferenceManager.getDefaultSharedPreferences(this).edit()
                        .putBoolean("a2_showBitfield", item.isChecked())
                        .apply();
                break;
            case android.R.id.home:
                onBackPressed();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        adapter.stopAllUpdater();
    }
}