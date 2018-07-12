package com.gianlu.aria2app;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gianlu.aria2app.Activities.EditProfileActivity;
import com.gianlu.aria2app.Main.MainActivity;
import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.HttpClient;
import com.gianlu.aria2app.NetIO.OnConnect;
import com.gianlu.aria2app.NetIO.WebSocketClient;
import com.gianlu.aria2app.ProfilesManager.CustomProfilesAdapter;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.commonutils.Drawer.DrawerManager;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Toaster;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class LoadingActivity extends ActivityWithDialog implements OnConnect, DrawerManager.ProfilesDrawerListener<MultiProfile> {
    public static final String SHORTCUT_ADD_URI = "com.gianlu.aria2app.ADD_URI";
    public static final String SHORTCUT_ADD_METALINK = "com.gianlu.aria2app.ADD_METALINK";
    public static final String SHORTCUT_ADD_TORRENT = "com.gianlu.aria2app.ADD_TORRENT";
    public static final String SHORTCUT_SEARCH = "com.gianlu.aria2app.SEARCH";
    private Intent goTo;
    private LinearLayout connecting;
    private LinearLayout picker;
    private TextView pickerHint;
    private RecyclerView pickerList;
    private boolean finished = false;
    private Uri shareData;
    private String launchGid;
    private Button seeError;
    private Button cancel;
    private ProfilesManager manager;
    private String shortcutAction;
    private Handler handler;

    public static void startActivity(Context context) {
        context.startActivity(new Intent(context, LoadingActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }

    public static void startActivity(Context context, @Nullable Throwable ex) {
        context.startActivity(new Intent(context, LoadingActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra("showPicker", true)
                .putExtra("ex", ex));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.hide();

        connecting = findViewById(R.id.loading_connecting);
        picker = findViewById(R.id.loading_picker);
        pickerHint = findViewById(R.id.loading_pickerHint);
        pickerList = findViewById(R.id.loading_pickerList);
        seeError = findViewById(R.id.loading_seeError);
        cancel = findViewById(R.id.loading_cancel);
        pickerList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        ImageButton pickerAdd = findViewById(R.id.loading_pickerAdd);
        pickerAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditProfileActivity.start(LoadingActivity.this, false);
            }
        });

        Button settings = findViewById(R.id.loading_settings);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoadingActivity.this, PreferencesActivity.class));
            }
        });

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finished = true;
                if (goTo != null) startActivity(goTo);
            }
        }, 1000);

        WebSocketClient.clear();
        HttpClient.clear();

        manager = ProfilesManager.get(this);
        if (!manager.hasProfiles()) {
            EditProfileActivity.start(this, true);
            return;
        }

        launchGid = getIntent().getStringExtra("gid");

        if (hasShortcutAction()) {
            AnalyticsApplication.sendAnalytics(LoadingActivity.this, Utils.ACTION_SHORTCUT);

            shortcutAction = getIntent().getAction();
            displayPicker(true);
            return;
        }

        if (hasShareData()) {
            AnalyticsApplication.sendAnalytics(LoadingActivity.this, Utils.ACTION_SHARE);

            shareData = getShareData();
            displayPicker(true);
            return;
        }

        if (getIntent().getBooleanExtra("external", false)) {
            MultiProfile profile = ProfilesManager.createExternalProfile(getIntent());
            if (profile != null) {
                try {
                    manager.save(profile);
                    tryConnecting(profile);
                    return;
                } catch (IOException | JSONException ex) {
                    Toaster.with(this).message(R.string.cannotSaveProfile).ex(ex).show();
                    return;
                }
            }
        }

        String profileId = getIntent().getStringExtra("profileId");
        if (profileId != null && manager.profileExists(profileId)) {
            try {
                tryConnecting(manager.retrieveProfile(profileId));
                return;
            } catch (IOException | JSONException ex) {
                Logging.log(ex);
            }
        }

        final Throwable ex = (Throwable) getIntent().getSerializableExtra("ex");
        if (ex != null) {
            seeError.setVisibility(View.VISIBLE);
            seeError.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showErrorDialog(ex);
                }
            });
            getIntent().removeExtra("ex");
        } else {
            seeError.setVisibility(View.GONE);
        }

        if (getIntent().getBooleanExtra("showPicker", false))
            displayPicker(false);
        else
            tryConnecting(manager.getLastProfile());
    }

    private boolean hasShortcutAction() {
        String action = getIntent().getAction();
        return Objects.equals(action, SHORTCUT_ADD_METALINK)
                || Objects.equals(action, SHORTCUT_ADD_URI)
                || Objects.equals(action, SHORTCUT_ADD_TORRENT)
                || Objects.equals(action, SHORTCUT_SEARCH);
    }

    private boolean hasShareData() {
        String action = getIntent().getAction();
        return Objects.equals(action, Intent.ACTION_SEND) || Objects.equals(action, Intent.ACTION_VIEW);
    }

    @Nullable
    private Uri getShareData() {
        Uri stream = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
        if (stream == null) {
            Uri data = getIntent().getData();
            if (data == null) {
                String text = getIntent().getStringExtra(Intent.EXTRA_TEXT);
                if (text != null) return Uri.parse(text);
            } else {
                return data;
            }
        } else {
            return stream;
        }

        return null;
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        ProfilesManager manager = ProfilesManager.get(this);
        if (manager.hasProfiles()) {
            displayPicker(hasShareData());
        } else {
            EditProfileActivity.start(this, true);
        }
    }

    private void tryConnecting(MultiProfile profile) {
        connecting.setVisibility(View.VISIBLE);
        picker.setVisibility(View.GONE);
        seeError.setVisibility(View.GONE);

        if (profile == null) {
            displayPicker(hasShareData());
        } else {
            manager.setCurrent(this, profile);
            MultiProfile.UserProfile single = profile.getProfile(this);
            if (single.connectionMethod == MultiProfile.ConnectionMethod.WEBSOCKET) {
                WebSocketClient.instantiate(this, single, this);
            } else {
                HttpClient.instantiate(this, single, this);
            }

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    cancel.setVisibility(View.VISIBLE);
                    cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            cancelConnection();
                        }
                    });
                }
            }, 2000);
        }
    }

    private void cancelConnection() {
        WebSocketClient.clear();
        HttpClient.clear();
        displayPicker(hasShareData());
        seeError.setVisibility(View.GONE);
    }

    private void showErrorDialog(@NonNull final Throwable ex) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.failedConnecting)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.contactMe, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        CommonUtils.sendEmail(LoadingActivity.this, getString(R.string.app_name), ex);
                    }
                })
                .setMessage(ex.toString());

        showDialog(builder);
    }

    private void displayPicker(boolean share) {
        connecting.setVisibility(View.GONE);
        picker.setVisibility(View.VISIBLE);

        if (share) pickerHint.setText(R.string.pickProfile_someAction);
        else pickerHint.setText(R.string.pickProfile);

        List<MultiProfile> profiles = manager.getProfiles();
        if (share && profiles.size() == 1) {
            tryConnecting(profiles.get(0));
            return;
        }

        CustomProfilesAdapter adapter = new CustomProfilesAdapter(this, profiles, R.style.ForceWhite, this);
        pickerList.setAdapter(adapter);
        adapter.startProfilesTest();
    }

    @Override
    public void onDrawerProfileSelected(@NonNull MultiProfile profile) {
        tryConnecting(profile);
    }

    @Override
    public boolean onDrawerProfileLongClick(@NonNull MultiProfile profile) {
        EditProfileActivity.start(LoadingActivity.this, profile.id);
        return true;
    }

    private void goTo(Class goTo) {
        Intent intent = new Intent(LoadingActivity.this, goTo).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        if (shortcutAction != null) intent.putExtra("shortcutAction", shortcutAction);
        else if (shareData != null) intent.putExtra("shareData", shareData);
        else if (launchGid != null && !launchGid.isEmpty()) intent.putExtra("gid", launchGid);
        if (finished) startActivity(intent);
        else this.goTo = intent;
    }

    @Override
    public boolean onConnected(@NonNull AbstractClient client) {
        goTo(MainActivity.class);
        return false;
    }

    @Override
    public void onPingTested(@NonNull AbstractClient client, long latency) {
    }

    @Override
    public void onFailedConnecting(@NonNull final Throwable ex) {
        Toaster.with(this).message(R.string.failedConnecting).ex(ex).show();
        displayPicker(hasShareData());
        seeError.setVisibility(View.VISIBLE);
        seeError.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showErrorDialog(ex);
            }
        });

        Logging.log(ex);
    }
}
