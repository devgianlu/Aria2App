package com.gianlu.aria2app.inappdownloader;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import com.gianlu.aria2app.PK;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.ThisApplication;
import com.gianlu.aria2lib.Aria2Ui;
import com.gianlu.aria2lib.internal.Message;
import com.gianlu.aria2lib.ui.Aria2ConfigurationScreen;
import com.gianlu.aria2lib.ui.Aria2ConfigurationScreen.LogEntry;
import com.gianlu.aria2lib.ui.ImportExportUtils;
import com.gianlu.commonutils.FileUtils;
import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.ui.Toaster;

import org.json.JSONException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class InAppAria2ConfActivity extends ActivityWithDialog implements Aria2Ui.Listener {
    private static final int RC_STORAGE_ACCESS_CODE = 3;
    private static final String TAG = InAppAria2ConfActivity.class.getSimpleName();
    private static final int RC_IMPORT_CONFIG = 4;
    private Aria2ConfigurationScreen screen;
    private ToggleButton toggle;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_STORAGE_ACCESS_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = data.getData();
                if (uri != null) {
                    screen.setOutputPathValue(FileUtils.getFullPathFromTreeUri(uri, this));
                    getContentResolver().takePersistableUriPermission(uri,
                            data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
                }
            }
        } else if (requestCode == RC_IMPORT_CONFIG) {
            if (resultCode == Activity.RESULT_OK && data.getData() != null) {
                try {
                    InputStream in = getContentResolver().openInputStream(data.getData());
                    if (in != null) {
                        try {
                            ImportExportUtils.importConfigFromStream(in);
                            Toaster.with(this).message(R.string.importedConfig).show();
                        } catch (IOException | JSONException | OutOfMemoryError ex) {
                            Toaster.with(this).message(R.string.cannotImport).show();
                        }
                    }
                } catch (FileNotFoundException ex) {
                    Toaster.with(this).message(R.string.fileNotFound).show();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.in_app_downloader, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.inappAria2conf_importExport:
                ImportExportUtils.showDialog(this, RC_IMPORT_CONFIG);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_app_aria2_conf);
        setTitle(getString(R.string.inAppDownloader_configuration) + " - " + getString(R.string.app_name));

        ActionBar bar = getSupportActionBar();
        if (bar != null) bar.setDisplayHomeAsUpEnabled(true);

        ThisApplication app = ((ThisApplication) getApplication());

        TextView version = findViewById(R.id.inAppAria2conf_binVersion);
        version.setText(app.getInAppAria2Version());

        boolean lastState = app.getLastAria2UiState();

        toggle = findViewById(R.id.inAppAria2conf_toggleServer);
        toggle.setChecked(lastState);
        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) app.startAria2Service();
            else app.stopAria2Service();
        });

        screen = findViewById(R.id.inAppAria2conf_screen);
        screen.setup(new Aria2ConfigurationScreen.OutputPathSelector(this, RC_STORAGE_ACCESS_CODE), PK.IN_APP_DOWNLOADER_AT_BOOT, null, false);

        screen.lockPreferences(lastState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        ((ThisApplication) getApplication()).addAria2UiListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        ((ThisApplication) getApplication()).removeAria2UiListener(this);
    }

    private void addLog(@NonNull LogEntry entry) {
        if (screen != null) screen.appendLogEntry(entry);
    }

    @Override
    public void onUpdateLogs(@NonNull List<Aria2Ui.LogMessage> list) {
        for (Aria2Ui.LogMessage msg : list) {
            LogEntry entry = createLogEntry(msg);
            if (entry != null) addLog(entry);
        }
    }

    @Nullable
    private LogEntry createLogEntry(@NonNull Aria2Ui.LogMessage msg) {
        switch (msg.type) {
            case PROCESS_TERMINATED:
                return new LogEntry(LogEntry.Type.INFO, getString(R.string.logTerminated, msg.i));
            case PROCESS_STARTED:
                return new LogEntry(LogEntry.Type.INFO, getString(R.string.logStarted, msg.o));
            case PROCESS_WARN:
                if (msg.o != null)
                    return new LogEntry(LogEntry.Type.WARNING, (String) msg.o);
            case PROCESS_ERROR:
                if (msg.o != null)
                    return new LogEntry(LogEntry.Type.ERROR, (String) msg.o);
            case PROCESS_INFO:
                if (msg.o != null)
                    return new LogEntry(LogEntry.Type.INFO, (String) msg.o);
            case MONITOR_FAILED:
            case MONITOR_UPDATE:
                return null;
        }

        return null;
    }

    @Override
    public void onMessage(@NonNull Aria2Ui.LogMessage msg) {
        if (msg.type == Message.Type.MONITOR_FAILED) {
            Log.e(TAG, "Monitor failed!", (Throwable) msg.o);
            return;
        }

        if (msg.type == Message.Type.MONITOR_UPDATE) return;

        LogEntry entry = createLogEntry(msg);
        if (entry != null) addLog(entry);
    }

    @Override
    public void updateUi(boolean on) {
        toggle.setChecked(on);
        screen.lockPreferences(on);
    }
}
