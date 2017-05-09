package com.gianlu.aria2app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;

import com.gianlu.aria2app.NetIO.WebSocketing;
import com.gianlu.aria2app.ProfilesManager.CustomProfilesAdapter;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.ProfilesManager.UserProfile;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Drawer.ProfilesAdapter;
import com.gianlu.commonutils.Logging;

public class LoadingActivity extends AppCompatActivity {
    private Intent goTo;
    private LinearLayout connecting;
    private RecyclerView picker;
    private boolean finished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(this));

        setContentView(R.layout.activity_loading);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.hide();

        connecting = (LinearLayout) findViewById(R.id.loading_connecting);
        picker = (RecyclerView) findViewById(R.id.loading_picker);
        picker.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finished = true;
                if (goTo != null) startActivity(goTo);
            }
        }, 1000);

        Logging.clearLogs(this);

        /* TODO: External profiles from Aria2Android
        if (getIntent().getBooleanExtra("external", false)) {
            setTitle(getString(R.string.app_name) + " - Local device");
            saveExternalProfile(SingleModeProfileItem.externalDefault(getIntent().getIntExtra("port", 6800), getIntent().getStringExtra("token")));
        }
        */

        final ProfilesManager manager = ProfilesManager.get(this);
        if (!manager.hasProfiles()) {
            // TODO: Start "add profile" activity
            return;
        }

        tryConnecting(manager, manager.getLastProfile(this));
    }

    private void tryConnecting(final ProfilesManager manager, UserProfile profile) {
        connecting.setVisibility(View.VISIBLE);
        picker.setVisibility(View.GONE);

        if (profile == null) {
            displayPicker(manager);
        } else {
            manager.setCurrent(this, profile);
            WebSocketing.instantiate(this, new WebSocketing.IConnect() {
                @Override
                public void onConnected() {
                    goTo(MainActivity.class);
                }

                @Override
                public void onFailedConnecting(Exception ex) {
                    CommonUtils.UIToast(LoadingActivity.this, Utils.ToastMessages.FAILED_CONNECTING, ex);
                    displayPicker(manager);
                }
            });
        }
    }

    private void displayPicker(final ProfilesManager manager) {
        connecting.setVisibility(View.GONE);
        picker.setVisibility(View.VISIBLE);

        CustomProfilesAdapter adapter = new CustomProfilesAdapter(this, manager.getProfiles(), new ProfilesAdapter.IAdapter<UserProfile>() {
            @Override
            public void onProfileSelected(UserProfile profile) {
                tryConnecting(manager, profile);
            }
        });

        picker.setAdapter(adapter);
        adapter.startProfilesTest(null);
    }

    private void goTo(Class goTo) {
        Intent intent = new Intent(LoadingActivity.this, goTo).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        if (finished) startActivity(intent);
        else this.goTo = intent;
    }
}
