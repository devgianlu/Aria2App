package com.gianlu.aria2app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.aria2app.activities.EditProfileActivity;
import com.gianlu.aria2app.api.AbstractClient;
import com.gianlu.aria2app.api.HttpClient;
import com.gianlu.aria2app.api.NetInstanceHolder;
import com.gianlu.aria2app.api.OnConnect;
import com.gianlu.aria2app.api.WebSocketClient;
import com.gianlu.aria2app.inappdownloader.InAppAria2ConfActivity;
import com.gianlu.aria2app.main.MainActivity;
import com.gianlu.aria2app.profiles.CustomProfilesAdapter;
import com.gianlu.aria2app.profiles.MultiProfile;
import com.gianlu.aria2app.profiles.ProfilesManager;
import com.gianlu.aria2app.services.NotificationService;
import com.gianlu.aria2app.webview.WebViewActivity;
import com.gianlu.aria2lib.Aria2Ui;
import com.gianlu.aria2lib.BadEnvironmentException;
import com.gianlu.aria2lib.internal.Message;
import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.drawer.DrawerManager;
import com.gianlu.commonutils.logs.LogsHelper;
import com.gianlu.commonutils.permissions.AskPermission;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.ui.Toaster;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONException;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

public class LoadingActivity extends ActivityWithDialog implements OnConnect, DrawerManager.ProfilesDrawerListener<MultiProfile>, Aria2Ui.Listener {
    public static final String SHORTCUT_ADD_URI = "com.gianlu.aria2app.ADD_URI";
    public static final String SHORTCUT_ADD_METALINK = "com.gianlu.aria2app.ADD_METALINK";
    public static final String SHORTCUT_ADD_TORRENT = "com.gianlu.aria2app.ADD_TORRENT";
    public static final String SHORTCUT_SEARCH = "com.gianlu.aria2app.SEARCH";
    public static final String SHORTCUT_WEB_VIEW = "com.gianlu.aria2app.WEB_VIEW";
    public static final String SHORTCUT_BATCH_ADD = "com.gianlu.aria2app.BATCH_ADD";
    private static final String TAG = LoadingActivity.class.getSimpleName();
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
    private volatile Closeable ongoingTest;
    private volatile MultiProfile startAria2ServiceOn = null;
    private View pickerSpacing;

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
        pickerSpacing = findViewById(R.id.loading_pickerSpacing);
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
            showDialog(new MaterialAlertDialogBuilder(this)
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
                Log.e(TAG, "Failed getting profile.", ex);
            }
        }

        if (getIntent().getBooleanExtra("openFromNotification", false)) {
            for (MultiProfile profile : manager.getProfiles()) {
                if (profile.isInAppDownloader()) {
                    connectToInAppDownloader(profile);
                    return;
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

        if (getIntent().getBooleanExtra("showPicker", false))
            displayPicker(false);
        else
            tryConnecting(manager.getLastProfile());
    }

    private void connectToInAppDownloader(@NonNull MultiProfile profile) {
        AskPermission.Listener listener = new AskPermission.Listener() {
            @Override
            public void permissionGranted(@NonNull String permission) {
                connecting.setVisibility(View.VISIBLE);
                picker.setVisibility(View.GONE);
                if (pickerSpacing != null) pickerSpacing.setVisibility(View.VISIBLE);
                seeError.setVisibility(View.GONE);
                cancel.setVisibility(View.GONE);

                handler.postDelayed(() -> {
                    cancel.setVisibility(View.VISIBLE);
                    cancel.setOnClickListener(view -> cancelConnection());
                }, 2000);

                ThisApplication app = ((ThisApplication) getApplication());

                try {
                    app.loadAria2ServiceEnv();
                } catch (BadEnvironmentException ex) {
                    Log.e(TAG, "Failed loading aria2 environment.", ex);
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
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listener.permissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return;
        }

        AskPermission.ask(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, listener);
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
                || Objects.equals(action, SHORTCUT_SEARCH)
                || Objects.equals(action, SHORTCUT_BATCH_ADD)
                || Objects.equals(action, SHORTCUT_WEB_VIEW);
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
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                    grantUriPermission("com.gianlu.aria2app", data, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                return data;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                grantUriPermission("com.gianlu.aria2app", stream, Intent.FLAG_GRANT_READ_URI_PERMISSION);

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
        if (ongoingTest != null) return;

        if (profile != null && profile.isInAppDownloader() && profile != startAria2ServiceOn) {
            connectToInAppDownloader(profile);
            return;
        }

        connecting.setVisibility(View.VISIBLE);
        picker.setVisibility(View.GONE);
        if (pickerSpacing != null) pickerSpacing.setVisibility(View.VISIBLE);
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

            ongoingTest = null;
        }

        aria2AndroidProfile = null;

        displayPicker(hasShareData());
        seeError.setVisibility(View.GONE);
    }

    private void showErrorDialog(@NonNull final Throwable ex) {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.failedConnecting)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.contactMe, (dialog, which) -> LogsHelper.openGithubIssue(LoadingActivity.this, "Aria2App", ex))
                .setMessage(ex.toString());

        showDialog(builder);
    }

    private void displayPicker(boolean share) {
        connecting.setVisibility(View.GONE);
        picker.setVisibility(View.VISIBLE);
        if (pickerSpacing != null) pickerSpacing.setVisibility(View.GONE);

        if (share) pickerHint.setText(R.string.pickProfile_someAction);
        else pickerHint.setText(R.string.pickProfile);

        if (manager == null) manager = ProfilesManager.get(this);
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
    protected void onDestroy() {
        super.onDestroy();

        ((ThisApplication) getApplication()).removeAria2UiListener(this);
    }

    @Override
    public boolean onDrawerProfileLongClick(@NonNull MultiProfile profile) {
        if (profile.isInAppDownloader()) {
            try {
                ThisApplication app = (ThisApplication) getApplicationContext();
                app.loadAria2ServiceEnv();
            } catch (BadEnvironmentException ex) {
                return false;
            }

            startActivity(new Intent(this, InAppAria2ConfActivity.class));
        } else {
            EditProfileActivity.start(this, profile.id);
        }

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

            if (!Prefs.getBoolean(PK.A2_SKIP_WEBVIEW_DIALOG) && Utils.hasWebView(this)) {
                AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
                builder.setTitle(R.string.useWebView)
                        .setMessage(R.string.useWebView_message)
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> launchWebView())
                        .setNeutralButton(android.R.string.no, (dialog, which) -> launchMain());
                showDialog(builder);
                return false;
            }
        }

        launchMain();
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
                Toaster.with(this).message(R.string.failedStartingAria2Android).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void failedConnecting(@NonNull Throwable ex) {
        Toaster.with(this).message(R.string.failedConnecting).show();
        displayPicker(hasShareData());
        seeError.setVisibility(View.VISIBLE);
        seeError.setOnClickListener(v -> showErrorDialog(ex));
        Log.e(TAG, "Failed connecting.", ex);
    }

    @Override
    public void onFailedConnecting(@NonNull MultiProfile.UserProfile profile, @NonNull Throwable ex) {
        ongoingTest = null;
        failedConnecting(ex);
    }

    @Override
    public void onUpdateLogs(@NonNull List<Aria2Ui.LogMessage> msg) {
    }

    @Override
    public void onMessage(@NonNull Aria2Ui.LogMessage msg) {
        if (isDestroyed()) return;

        if (msg.type == Message.Type.PROCESS_STARTED && startAria2ServiceOn != null)
            tryConnecting(startAria2ServiceOn);
    }

    @Override
    public void updateUi(boolean on) {
    }
}
