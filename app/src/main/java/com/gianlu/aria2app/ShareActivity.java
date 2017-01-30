package com.gianlu.aria2app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import com.gianlu.aria2app.Main.AddTorrentActivity;
import com.gianlu.aria2app.Profile.ProfileItem;
import com.gianlu.aria2app.Profile.ProfilesAdapter;
import com.gianlu.aria2app.Profile.SingleModeProfileItem;
import com.gianlu.commonutils.CommonUtils;

import java.io.File;
import java.util.Objects;

public class ShareActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        setTitle(R.string.startDownload);

        ListView list = (ListView) findViewById(R.id.share_list);
        final ProfilesAdapter adapter = new ProfilesAdapter(this, ProfileItem.getProfiles(this), new ProfilesAdapter.IProfile() {
            @Override
            public void onProfileSelected(SingleModeProfileItem profile) {
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
                adapter.startProfilesTest(new ProfilesAdapter.IFinished() {
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
    }

    private void handleStartDownload() {
        Intent intent = getIntent();
        Uri data = intent.getData();

        if ((!Objects.equals(intent.getAction(), Intent.ACTION_VIEW) && !Objects.equals(intent.getAction(), Intent.ACTION_SEND)) || data == null) {
            CommonUtils.UIToast(this, Utils.ToastMessages.FAILED_ADD_DOWNLOAD, "Wrong action or missing data.");
            finish();
            return;
        }

        if (Objects.equals(data.getScheme(), "file")) {
            File file = new File(data.getPath());
            if (file.exists() && file.canRead()) {
                startActivity(new Intent(this, AddTorrentActivity.class)
                        .putExtra("share_file", file)
                        .putExtra("torrentMode", Objects.equals(intent.getType(), "application/x-bittorrent")));
            } else {
                CommonUtils.UIToast(this, Utils.ToastMessages.FILE_NOT_FOUND, file.getPath());
                finish();
            }
        } else {
            // TODO: Handle URI
        }
    }
}
