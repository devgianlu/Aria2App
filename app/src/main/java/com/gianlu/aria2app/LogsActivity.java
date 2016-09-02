package com.gianlu.aria2app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
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
                ClipData clip = ClipData.newPlainText("stack trace", ((LoglineItem) adapterView.getItemAtPosition(i)).getMessage());
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
                List<LoglineItem> logLines = new ArrayList<>();
                try {
                    FileInputStream in = openFileInput(adapterView.getItemAtPosition(i).toString());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("--ERROR--")) {
                            logLines.add(new LoglineItem(TYPE.ERROR, line.replace("--ERROR--", "")));
                        } else if (line.startsWith("--INFO--")) {
                            logLines.add(new LoglineItem(TYPE.INFO, line.replace("--INFO--", "")));
                        }
                    }
                } catch (IOException ex) {
                    Utils.UIToast(LogsActivity.this, Utils.TOAST_MESSAGES.FATAL_EXCEPTION, ex);
                    onBackPressed();
                }

                log.setAdapter(new LoglineAdapter(logLines));
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
                startActivity(new Intent(this, MainPreferencesActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public enum TYPE {
        INFO,
        WARNING,
        ERROR
    }

    private class LoglineItem {
        private TYPE type;
        private String message;

        public LoglineItem(TYPE type, String message) {
            this.type = type;
            this.message = message;
        }

        public TYPE getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }
    }

    private class LoglineAdapter extends BaseAdapter {
        private List<LoglineItem> objs;

        public LoglineAdapter(List<LoglineItem> objs) {
            this.objs = objs;
        }

        @Override
        public int getCount() {
            return objs.size();
        }

        @Override
        public LoglineItem getItem(int i) {
            return objs.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout linearLayout = new LinearLayout(LogsActivity.this);
            linearLayout.setPadding(12, 12, 12, 12);
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);

            LoglineItem item = getItem(position);

            TextView type = new TextView(LogsActivity.this);
            type.setTypeface(Typeface.DEFAULT_BOLD);
            switch (item.getType()) {
                case INFO:
                    type.setText(R.string.infoTag);
                    type.setTextColor(Color.BLACK);
                    break;
                case WARNING:
                    type.setText(R.string.warningTag);
                    type.setTextColor(Color.YELLOW);
                    break;
                case ERROR:
                    type.setText(R.string.errorTag);
                    type.setTextColor(Color.RED);
                    break;
            }
            linearLayout.addView(type);
            linearLayout.addView(Utils.fastTextView(LogsActivity.this, item.getMessage()));


            return linearLayout;
        }
    }
}
