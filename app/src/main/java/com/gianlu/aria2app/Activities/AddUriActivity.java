package com.gianlu.aria2app.Activities;

import android.app.ProgressDialog;
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

import com.gianlu.aria2app.Activities.AddDownload.OptionsFragment;
import com.gianlu.aria2app.Activities.AddDownload.UrisFragment;
import com.gianlu.aria2app.Adapters.PagerAdapter;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.JTA2.JTA2InitializingException;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.ThisApplication;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Toaster;
import com.google.android.gms.analytics.HitBuilders;

import java.net.URI;
import java.util.List;
import java.util.Map;

public class AddUriActivity extends AppCompatActivity {
    private UrisFragment urisFragment;
    private OptionsFragment optionsFragment;
    private ViewPager pager;

    public static void startAndAdd(Context context, URI uri) {
        context.startActivity(new Intent(context, AddUriActivity.class).putExtra("uri", uri));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_download);
        setTitle(R.string.addUri);

        Toolbar toolbar = findViewById(R.id.addDownload_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        pager = findViewById(R.id.addDownload_pager);
        final TabLayout tabLayout = findViewById(R.id.addDownload_tabs);

        urisFragment = UrisFragment.getInstance(this, true, (URI) getIntent().getSerializableExtra("uri"));
        optionsFragment = OptionsFragment.getInstance(this, true);

        pager.setAdapter(new PagerAdapter<>(getSupportFragmentManager(), urisFragment, optionsFragment));
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

    private void done() {
        List<String> uris = urisFragment.getUris();
        if (uris.isEmpty()) {
            Toaster.show(this, Utils.Messages.NO_URIS);
            pager.setCurrentItem(0, true);
            return;
        }

        Map<String, String> options = optionsFragment.getOptions();
        Integer position = optionsFragment.getPosition();
        String filename = optionsFragment.getFilename();

        if (filename != null) options.put("out", filename);

        JTA2 jta2;
        try {
            jta2 = JTA2.instantiate(this);
        } catch (JTA2InitializingException ex) {
            Toaster.show(this, Utils.Messages.FAILED_ADD_DOWNLOAD, ex);
            return;
        }

        final ProgressDialog pd = CommonUtils.fastIndeterminateProgressDialog(this, R.string.gathering_information);
        CommonUtils.showDialog(this, pd);
        jta2.addUri(uris, position, options, new JTA2.IGID() {
            @Override
            public void onGID(String gid) {
                pd.dismiss();
                Toaster.show(AddUriActivity.this, Utils.Messages.DOWNLOAD_ADDED, gid, new Runnable() {
                    @Override
                    public void run() {
                        onBackPressed();
                    }
                });
            }

            @Override
            public void onException(Exception ex) {
                pd.dismiss();
                Toaster.show(AddUriActivity.this, Utils.Messages.FAILED_ADD_DOWNLOAD, ex);
            }
        });

        ThisApplication.sendAnalytics(AddUriActivity.this, new HitBuilders.EventBuilder()
                .setCategory(ThisApplication.CATEGORY_USER_INPUT)
                .setAction(ThisApplication.ACTION_NEW_URI)
                .build());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.addDownload_done:
                done();
                break;
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        try {
            super.onBackPressed();
        } catch (IllegalStateException ignored) {
        }
    }
}
