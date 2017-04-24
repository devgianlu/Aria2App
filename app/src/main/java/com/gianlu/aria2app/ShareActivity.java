package com.gianlu.aria2app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.gianlu.aria2app.Main.AddTorrentActivity;
import com.gianlu.aria2app.Main.AddURIActivity;
import com.gianlu.aria2app.Profile.CustomProfilesAdapter;
import com.gianlu.aria2app.Profile.MultiModeProfileItem;
import com.gianlu.aria2app.Profile.ProfileItem;
import com.gianlu.aria2app.Profile.SingleModeProfileItem;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Drawer.BaseDrawerProfile;
import com.gianlu.commonutils.Drawer.ProfilesAdapter;
import com.google.android.gms.analytics.HitBuilders;

import java.io.File;
import java.util.Objects;

public class ShareActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        setTitle(R.string.startDownload);

        RecyclerView list = (RecyclerView) findViewById(R.id.share_list);
        list.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        final CustomProfilesAdapter adapter = new CustomProfilesAdapter(this, ProfileItem.getBaseProfiles(this), R.drawable.ripple_effect_dark, new ProfilesAdapter.IAdapter() {
            @Override
            public void onProfileSelected(BaseDrawerProfile _profile) {
                SingleModeProfileItem profile;
                if (_profile instanceof MultiModeProfileItem)
                    profile = ((MultiModeProfileItem) _profile).getCurrentProfile(ShareActivity.this);
                else
                    profile = (SingleModeProfileItem) _profile;

                if (profile.status == ProfileItem.STATUS.ONLINE) {
                    CurrentProfile.setCurrentProfile(ShareActivity.this, profile);
                    handleStartDownload();
                } else {
                    CommonUtils.UIToast(ShareActivity.this, Utils.ToastMessages.NOT_ONLINE, profile.getFullServerAddress());
                }
            }
        });
        list.setAdapter(adapter);
        adapter.startProfilesTest(null);

        final SwipeRefreshLayout layout = (SwipeRefreshLayout) findViewById(R.id.share_swipeRefresh);
        layout.setColorSchemeResources(R.color.colorAccent);
        layout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                adapter.startProfilesTest(new CustomProfilesAdapter.IFinished() {
                    @Override
                    public void onFinished() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                layout.setRefreshing(false);
                            }
                        });
                    }
                });
            }
        });

        ThisApplication.sendAnalytics(this, new HitBuilders.EventBuilder()
                .setCategory(ThisApplication.CATEGORY_USER_INPUT)
                .setAction(ThisApplication.ACTION_SHARE)
                .build());
    }

    private void handleStartDownload() {
        Intent intent = getIntent();
        String strData = intent.getStringExtra(Intent.EXTRA_TEXT);
        Uri data = intent.getData();

        if (data != null) {
            if (Objects.equals(data.getScheme(), "file")) {
                File file = new File(data.getPath());
                if (file.exists() && file.canRead()) {
                    startActivity(new Intent(this, AddTorrentActivity.class)
                            .putExtra("share_file", file)
                            .putExtra("torrentMode", Objects.equals(intent.getType(), "application/x-bittorrent")));
                } else {
                    CommonUtils.UIToast(this, Utils.ToastMessages.FILE_NOT_FOUND, file.getPath());
                }
            } else {
                startActivity(new Intent(this, AddURIActivity.class)
                        .putExtra("share_uri", data.toString()));
            }
        } else if (strData != null) {
            startActivity(new Intent(this, AddURIActivity.class)
                    .putExtra("share_uri", strData));
        } else {
            CommonUtils.UIToast(this, Utils.ToastMessages.FAILED_ADD_DOWNLOAD, "Invalid or missing data.");
        }

        finish();
    }
}
