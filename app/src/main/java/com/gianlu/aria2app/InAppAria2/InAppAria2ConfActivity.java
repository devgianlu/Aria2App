package com.gianlu.aria2app.InAppAria2;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.gianlu.aria2app.PK;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.ThisApplication;
import com.gianlu.aria2lib.Aria2Ui;
import com.gianlu.aria2lib.Interface.Aria2ConfigurationScreen;
import com.gianlu.aria2lib.Internal.Message;
import com.gianlu.commonutils.FileUtil;
import com.gianlu.commonutils.Logging;

import java.io.Serializable;

public class InAppAria2ConfActivity extends AppCompatActivity implements Aria2Ui.Listener {
    private static final int STORAGE_ACCESS_CODE = 3;
    private Aria2ConfigurationScreen screen;
    private ToggleButton toggle;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == STORAGE_ACCESS_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = data.getData();
                if (uri != null) {
                    screen.setOutputPathValue(FileUtil.getFullPathFromTreeUri(uri, this));
                    getContentResolver().takePersistableUriPermission(uri,
                            data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_app_aria2_conf);
        setTitle(getString(R.string.inAppDownloader_configurtion) + " - " + getString(R.string.app_name));

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
        screen.setup(new Aria2ConfigurationScreen.OutputPathSelector(this, STORAGE_ACCESS_CODE), PK.IN_APP_DOWNLOADER_AT_BOOT, false);

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

    private void addLog(@NonNull Logging.LogLine line) {
        if (screen != null) screen.appendLogLine(line);
    }

    @Override
    public void onMessage(@NonNull Message.Type type, int i, @Nullable Serializable o) {
        switch (type) {
            case PROCESS_TERMINATED:
                addLog(new Logging.LogLine(Logging.LogLine.Type.INFO, getString(R.string.logTerminated, i)));
                break;
            case PROCESS_STARTED:
                addLog(new Logging.LogLine(Logging.LogLine.Type.INFO, getString(R.string.logStarted, o)));
                break;
            case MONITOR_FAILED:
                Logging.log("Monitor failed!", (Throwable) o);
                break;
            case MONITOR_UPDATE:
                break;
            case PROCESS_WARN:
                if (o != null)
                    addLog(new Logging.LogLine(Logging.LogLine.Type.WARNING, (String) o));
                break;
            case PROCESS_ERROR:
                if (o != null)
                    addLog(new Logging.LogLine(Logging.LogLine.Type.ERROR, (String) o));
                break;
            case PROCESS_INFO:
                if (o != null)
                    addLog(new Logging.LogLine(Logging.LogLine.Type.INFO, (String) o));
                break;
        }
    }

    @Override
    public void updateUi(boolean on) {
        toggle.setChecked(on);
        screen.lockPreferences(on);
    }
}
