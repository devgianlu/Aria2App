package com.gianlu.aria2app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.aria2app.Activities.EditProfileActivity;
import com.gianlu.aria2app.InAppAria2.InAppAria2ConfActivity;
import com.gianlu.aria2app.Main.MainActivity;
import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.HttpClient;
import com.gianlu.aria2app.NetIO.NetInstanceHolder;
import com.gianlu.aria2app.NetIO.OnConnect;
import com.gianlu.aria2app.NetIO.WebSocketClient;
import com.gianlu.aria2app.ProfilesManager.CustomProfilesAdapter;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.Services.NotificationService;
import com.gianlu.aria2app.WebView.WebViewActivity;
import com.gianlu.aria2lib.Aria2Ui;
import com.gianlu.aria2lib.BadEnvironmentException;
import com.gianlu.aria2lib.Interface.DownloadBinActivity;
import com.gianlu.aria2lib.Internal.Message;
import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.AskPermission;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.commonutils.Drawer.DrawerManager;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.commonutils.Toaster;

import org.json.JSONException;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

public class LoadingActivity extends ActivityWithDialog implements OnConnect, DrawerManager.ProfilesDrawerListener<MultiProfile>, Aria2Ui.Listener {
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
    private MultiProfile.UserProfile aria2AndroidProfile = null;
    private Closeable ongoingTest;
    private volatile MultiProfile startAria2ServiceOn = null;

    public static void startActivity(@NonNull Context context) {
        context.startActivity(new Intent(context, LoadingActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }

    public static void startActivity(@NonNull Context context, @Nullable Throwable ex) {
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
        pickerList.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        ImageButton pickerAdd = findViewById(R.id.loading_pickerAdd);
        pickerAdd.setOnClickListener(v -> EditProfileActivity.start(LoadingActivity.this, false));

        Button settings = findViewById(R.id.loading_settings);
        settings.setOnClickListener(v -> startActivity(new Intent(LoadingActivity.this, PreferenceActivity.class)));

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            finished = true;
            if (goTo != null) {
                startActivity(goTo);
                finishAndRemoveTask();
            }
        }, 1000);

        NetInstanceHolder.close();

        if (getIntent().getBooleanExtra("external", false)) {
            showDialog(new AlertDialog.Builder(this)
                    .setTitle(R.string.oldAria2AppNoInApp)
                    .setMessage(R.string.oldAria2AppNoInApp_message)
                    .setNeutralButton(android.R.string.ok, null));
        }

        manager = ProfilesManager.get(this);
        if (!manager.hasProfiles()) {
            EditProfileActivity.start(this, true);
            return;
        }

        launchGid = getIntent().getStringExtra("gid");

        if (hasShortcutAction()) {
            AnalyticsApplication.sendAnalytics(Utils.ACTION_SHORTCUT);

            shortcutAction = getIntent().getAction();
            displayPicker(true);
            return;
        }

        if (hasShareData()) {
            AnalyticsApplication.sendAnalytics(Utils.ACTION_SHARE);

            shareData = getShareData();
            displayPicker(true);
            return;
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

        if (getIntent().getBooleanExtra("openFromNotification", false)) {
            for (MultiProfile profile : manager.getProfiles()) {
                if (profile.isInAppDownloader()) {
                    connectToInAppDownloader(profile);
                    break;
                }
            }
        }

        Throwable givenEx = (Throwable) getIntent().getSerializableExtra("ex");
        if (givenEx != null) {
            seeError.setVisibility(View.VISIBLE);
            seeError.setOnClickListener(v -> showErrorDialog(givenEx));
            getIntent().removeExtra("ex");
        } else {
            seeError.setVisibility(View.GONE);
        }

        if (getIntent().getBooleanExtra("showPicker", false)) {
            displayPicker(false);
        } else {
            MultiProfile last = manager.getLastProfile();
            if (last != null && last.isInAppDownloader()) connectToInAppDownloader(last);
            else tryConnecting(last);
        }
    }

    private void connectToInAppDownloader(@NonNull MultiProfile profile) {
        AskPermission.ask(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, new AskPermission.Listener() {
            @Override
            public void permissionGranted(@NonNull String permission) {
                cancel.setVisibility(View.GONE);
                handler.postDelayed(() -> {
                    cancel.setVisibility(View.VISIBLE);
                    cancel.setOnClickListener(view -> cancelConnection());
                }, 2000);

                ThisApplication app = ((ThisApplication) getApplication());

                try {
                    app.loadAria2ServiceEnv();
                } catch (BadEnvironmentException ex) {
                    DownloadBinActivity.startActivity(LoadingActivity.this, getString(R.string.downloadBin) + " - " + getString(R.string.app_name),
                            LoadingActivity.class, Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK, null);
                    return;
                }

                startAria2ServiceOn = profile;
                app.startAria2Service();

                ThisApplication.sendAnalytics(Utils.ACTION_USE_IN_APP_DOWNLOADER);
            }

            @Override
            public void permissionDenied(@NonNull String permission) {
                Toaster.with(LoadingActivity.this).message(R.string.cannotStartInAppWithoutWritePermission).show();
                displayPicker(hasShareData());
            }

            @Override
            public void askRationale(@NonNull AlertDialog.Builder builder) {
                builder.setTitle(R.string.writeExternalStorageRequest_title)
                        .setMessage(R.string.writeExternalStorageRequest_message);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        ((ThisApplication) getApplication()).addAria2UiListener(this);
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
        if (manager.hasProfiles()) displayPicker(hasShareData());
        else EditProfileActivity.start(this, true);
    }

    private void tryConnecting(@Nullable MultiProfile profile) {
        connecting.setVisibility(View.VISIBLE);
        picker.setVisibility(View.GONE);
        seeError.setVisibility(View.GONE);
        cancel.setVisibility(View.GONE);

        if (profile == null) {
            displayPicker(hasShareData());
        } else {
            startAria2ServiceOn = null;

            manager.setCurrent(profile);
            MultiProfile.UserProfile single = profile.getProfile(this);
            if (single.connectionMethod == MultiProfile.ConnectionMethod.WEBSOCKET)
                ongoingTest = WebSocketClient.checkConnection(single, this, true);
            else
                ongoingTest = HttpClient.checkConnection(single, this, true);

            handler.postDelayed(() -> {
                cancel.setVisibility(View.VISIBLE);
                cancel.setOnClickListener(view -> cancelConnection());
            }, 2000);
        }
    }

    private void cancelConnection() {
        if (ongoingTest != null) {
            try {
                ongoingTest.close();
            } catch (IOException ignored) {
            }
        }

        aria2AndroidProfile = null;

        displayPicker(hasShareData());
        seeError.setVisibility(View.GONE);
    }

    private void showErrorDialog(@NonNull final Throwable ex) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.failedConnecting)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.contactMe, (dialog, which) -> Logging.sendEmail(LoadingActivity.this, ex))
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
        if (profile.isInAppDownloader()) connectToInAppDownloader(profile);
        else tryConnecting(profile);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        ((ThisApplication) getApplication()).removeAria2UiListener(this);
    }

    @Override
    public boolean onDrawerProfileLongClick(@NonNull MultiProfile profile) {
        if (profile.isInAppDownloader())
            startActivity(new Intent(this, InAppAria2ConfActivity.class));
        else
            EditProfileActivity.start(this, profile.id);

        return true;
    }

    private void launchMain() {
        Intent intent = new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        if (shortcutAction != null) intent.putExtra("shortcutAction", shortcutAction);
        else if (shareData != null) intent.putExtra("shareData", shareData);
        else if (launchGid != null && !launchGid.isEmpty()) intent.putExtra("gid", launchGid);
        if (finished) {
            startActivity(intent);
            finishAndRemoveTask();
        } else {
            this.goTo = intent;
        }
    }

    private void launchWebView() {
        Intent intent = new Intent(this, WebViewActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("shareData", shareData)
                .putExtra("canGoBack", false);
        startActivity(intent);
        finishAndRemoveTask();
    }

    @Override
    public boolean onConnected(@NonNull AbstractClient client) {
        ongoingTest = null;

        if (Prefs.getBoolean(PK.A2_ENABLE_NOTIFS, true))
            NotificationService.start(this);

        if (shareData != null) {
            try {
                new URL(shareData.toString());
            } catch (MalformedURLException ex) {
                launchMain();
                return false;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.useWebView)
                    .setMessage(R.string.useWebView_message)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> launchWebView())
                    .setNeutralButton(android.R.string.no, (dialog, which) -> launchMain());
            showDialog(builder);
        } else {
            launchMain();
        }

        return false;
    }

    @Override
    public void onPingTested(@NonNull AbstractClient client, long latency) {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 1 && aria2AndroidProfile != null) {
            if (resultCode == 1) {
                tryConnecting(aria2AndroidProfile.getParent());
            } else {
                aria2AndroidProfile = null;
                Toaster.with(this).message(R.string.failedStartingAria2Android).error(false).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void failedConnecting(@NonNull Throwable ex) {
        Toaster.with(this).message(R.string.failedConnecting).ex(ex).show();
        displayPicker(hasShareData());
        seeError.setVisibility(View.VISIBLE);
        seeError.setOnClickListener(v -> showErrorDialog(ex));
        Logging.log(ex);
    }

    @Override
    public void onFailedConnecting(@NonNull MultiProfile.UserProfile profile, @NonNull Throwable ex) {
        ongoingTest = null;
        failedConnecting(ex);
    }

    @Override
    public void onMessage(@NonNull Message.Type type, int i, @Nullable Serializable o) {
        if (isDestroyed()) return;

        if (type == Message.Type.PROCESS_STARTED && startAria2ServiceOn != null) {
            tryConnecting(startAria2ServiceOn);

            startAria2ServiceOn = null;
        }
    }

    @Override
    public void updateUi(boolean on) {
    }
}
