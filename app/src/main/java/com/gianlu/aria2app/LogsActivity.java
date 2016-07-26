package com.gianlu.aria2app;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LogsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);
        setTitle(R.string.title_activity_logs);

        final ListView log = (ListView) findViewById(R.id.logs_text);
        assert log != null;
        Spinner logs = (Spinner) findViewById(R.id.logs_spinner);
        assert logs != null;

        log.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("stack trace", ((LogLineItem) adapterView.getItemAtPosition(i)).getMessage());
                clipboard.setPrimaryClip(clip);

                Utils.UIToast(LogsActivity.this, getString(R.string.copiedClipboard), Toast.LENGTH_SHORT);
            }
        });

        File files[] = getFilesDir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.toLowerCase().endsWith(".log");
            }
        });

        List<String> spinnerList = new ArrayList<>();
        for (File logFile : files) {
            spinnerList.add(logFile.getName());
        }

        logs.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerList));
        logs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                List<LogLineItem> logLines = new ArrayList<>();
                try {
                    FileInputStream in = openFileInput(adapterView.getItemAtPosition(i).toString());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logLines.add(new LogLineItem(line.startsWith("--ERROR--"), line.replace(line.startsWith("--ERROR--") ? "--ERROR--" : "--INFO--", "")));
                    }
                } catch (IOException ex) {
                    Utils.UIToast(LogsActivity.this, Utils.TOAST_MESSAGES.FATAL_EXCEPTION, ex);
                    onBackPressed();
                }

                log.setAdapter(new LogLineAdapter(logLines));
                log.setSelection(log.getCount() - 1);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                finishActivity(0);
            }
        });

        logs.setSelection(logs.getCount() - 1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.logs, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logsMenu_deleteAll:
                File files[] = getFilesDir().listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File file, String s) {
                        return s.toLowerCase().endsWith(".log");
                    }
                });

                for (File logFile : files) {
                    logFile.delete();
                }

                Utils.UIToast(this, getString(R.string.logsDeleted), Toast.LENGTH_SHORT);
                startActivity(new Intent(this, MainSettingsActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private class LogLineItem {
        private boolean isError;
        private String message;

        public LogLineItem(boolean isError, String message) {
            this.isError = isError;
            this.message = message;
        }

        public boolean isError() {
            return isError;
        }

        public String getMessage() {
            return message;
        }
    }

    private class LogLineAdapter extends BaseAdapter {
        private List<LogLineItem> objs;

        public LogLineAdapter(List<LogLineItem> objs) {
            this.objs = objs;
        }

        @Override
        public int getCount() {
            return objs.size();
        }

        @Override
        public LogLineItem getItem(int i) {
            return objs.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @SuppressLint({"ViewHolder", "InflateParams", "SetTextI18n"})
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = getLayoutInflater().inflate(R.layout.logline_custom_item, null);

            LogLineItem item = getItem(position);

            TextView type = (TextView) convertView.findViewById(R.id.log_line_type);
            ((TextView) convertView.findViewById(R.id.log_line_message)).setText(item.getMessage());

            type.setText(item.isError() ? "ERROR:" : "INFO:");
            type.setTextColor(ContextCompat.getColor(LogsActivity.this, item.isError() ? android.R.color.holo_red_dark : android.R.color.black));

            return convertView;
        }
    }
}
