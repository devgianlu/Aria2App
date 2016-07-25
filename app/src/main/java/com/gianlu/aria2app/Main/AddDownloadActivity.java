package com.gianlu.aria2app.Main;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources.Theme;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ThemedSpinnerAdapter;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.gianlu.aria2app.Google.Analytics;
import com.gianlu.aria2app.MainActivity;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.jtitan.Aria2Helper.IGID;
import com.gianlu.jtitan.Aria2Helper.JTA2;
import com.google.android.gms.analytics.HitBuilders;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


public class AddDownloadActivity extends AppCompatActivity {
    private Spinner spinner;
    private URIFragment uriFragment;
    private TorrentFragment torrentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_download);

        setSupportActionBar((Toolbar) findViewById(R.id.addDownload_toolbar));
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        spinner = (Spinner) findViewById(R.id.addDownload_spinner);
        assert spinner != null;

        spinner.setAdapter(new SpinnerAdapter(this, new String[]{getString(R.string.uri), getString(R.string.torrent), getString(R.string.metalink)}));
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                switch (position) {
                    case 0:
                        uriFragment = URIFragment.newInstance();
                        transaction.replace(R.id.addDownload_container, uriFragment, "URI");
                        break;
                    case 1:
                        torrentFragment = TorrentFragment.newInstance(true);
                        transaction.replace(R.id.addDownload_container, torrentFragment, "TORRENT");
                        break;
                    case 2:
                        torrentFragment = TorrentFragment.newInstance(false);
                        transaction.replace(R.id.addDownload_container, torrentFragment, "METALINK");
                        break;
                }

                transaction.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                onBackPressed();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_download, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.addDownloadMenu_done:
                JTA2 jta2 = Utils.readyJTA2(this);
                final ProgressDialog progressDialog = Utils.fastProgressDialog(this, R.string.gathering_information, true, false);
                progressDialog.show();

                switch (spinner.getSelectedItemPosition()) {
                    case 0:
                        if (uriFragment.getUris().size() == 0) return false;

                        if (Analytics.isTrackingAllowed(this))
                            Analytics.getDefaultTracker(this.getApplication()).send(new HitBuilders.EventBuilder()
                                    .setCategory(Analytics.CATEGORY_USER_INPUT)
                                    .setAction(Analytics.ACTION_NEW_URI).build());

                        jta2.addUri(uriFragment.getUris(), uriFragment.getPosition(), uriFragment.getOptions(), new IGID() {
                            @Override
                            public void onGID(String GID) {
                                progressDialog.dismiss();
                                AddDownloadActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        AddDownloadActivity.this.onBackPressed();
                                    }
                                });
                            }

                            @Override
                            public void onException(Exception ex) {
                                progressDialog.dismiss();
                                Utils.UIToast(AddDownloadActivity.this, Utils.TOAST_MESSAGES.FAILED_ADD_DOWNLOAD, ex.getMessage());
                            }
                        });
                        break;
                    case 1:
                    case 2:
                        if (torrentFragment.getData() == null) return false;

                        InputStream in;
                        try {
                            in = getContentResolver().openInputStream(torrentFragment.getData());
                        } catch (FileNotFoundException ex) {
                            progressDialog.dismiss();
                            Utils.UIToast(AddDownloadActivity.this, Utils.TOAST_MESSAGES.FAILED_ADD_DOWNLOAD, ex.getMessage());
                            break;
                        }
                        if (in == null) break;

                        String base64;
                        try {
                            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                            int nRead;
                            byte[] data = new byte[4096];

                            while ((nRead = in.read(data, 0, data.length)) != -1) {
                                buffer.write(data, 0, nRead);
                            }

                            buffer.flush();

                            base64 = Base64.encodeToString(buffer.toByteArray(), Base64.NO_WRAP);
                        } catch (IOException ex) {
                            progressDialog.dismiss();
                            Utils.UIToast(AddDownloadActivity.this, Utils.TOAST_MESSAGES.FAILED_ADD_DOWNLOAD, ex.getMessage());
                            break;
                        }

                        if (spinner.getSelectedItemPosition() == 1) {
                            if (Analytics.isTrackingAllowed(this))
                                Analytics.getDefaultTracker(this.getApplication()).send(new HitBuilders.EventBuilder()
                                        .setCategory(Analytics.CATEGORY_USER_INPUT)
                                        .setAction(Analytics.ACTION_NEW_TORRENT).build());

                            jta2.addTorrent(base64, torrentFragment.getUris(), torrentFragment.getOptions(), torrentFragment.getPosition(), new IGID() {
                                @Override
                                public void onGID(String GID) {
                                    progressDialog.dismiss();
                                    AddDownloadActivity.this.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            AddDownloadActivity.this.onBackPressed();
                                        }
                                    });
                                }

                                @Override
                                public void onException(Exception ex) {
                                    progressDialog.dismiss();
                                    Utils.UIToast(AddDownloadActivity.this, Utils.TOAST_MESSAGES.FAILED_ADD_DOWNLOAD, ex.getMessage());
                                }
                            });
                        } else {
                            if (Analytics.isTrackingAllowed(this))
                                Analytics.getDefaultTracker(this.getApplication()).send(new HitBuilders.EventBuilder()
                                        .setCategory(Analytics.CATEGORY_USER_INPUT)
                                        .setAction(Analytics.ACTION_NEW_METALINK).build());

                            jta2.addMetalink(base64, torrentFragment.getUris(), torrentFragment.getOptions(), torrentFragment.getPosition(), new IGID() {
                                @Override
                                public void onGID(String GID) {
                                    progressDialog.dismiss();
                                    AddDownloadActivity.this.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            AddDownloadActivity.this.onBackPressed();
                                        }
                                    });
                                }

                                @Override
                                public void onException(Exception ex) {
                                    progressDialog.dismiss();
                                    Utils.UIToast(AddDownloadActivity.this, Utils.TOAST_MESSAGES.FAILED_ADD_DOWNLOAD, ex.getMessage());
                                }
                            });
                        }
                        break;
                }

                progressDialog.dismiss();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finishActivity(0);
    }

    private static class SpinnerAdapter extends ArrayAdapter<String> implements ThemedSpinnerAdapter {
        private final ThemedSpinnerAdapter.Helper mDropDownHelper;

        public SpinnerAdapter(Context context, String[] objects) {
            super(context, android.R.layout.simple_list_item_1, objects);
            mDropDownHelper = new ThemedSpinnerAdapter.Helper(context);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            convertView = mDropDownHelper.getDropDownViewInflater().inflate(android.R.layout.simple_list_item_1, parent, false);

            TextView text = (TextView) convertView.findViewById(android.R.id.text1);
            text.setText(getItem(position));
            text.setTextSize(16);

            return convertView;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView text = (TextView) super.getView(position, convertView, parent);

            text.setTextColor(ContextCompat.getColor(getContext(), android.R.color.white));
            text.setTextSize(20);
            text.setTypeface(null, Typeface.BOLD);

            return text;
        }

        @Override
        public Theme getDropDownViewTheme() {
            return mDropDownHelper.getDropDownViewTheme();
        }

        @Override
        public void setDropDownViewTheme(Theme theme) {
            mDropDownHelper.setDropDownViewTheme(theme);
        }
    }
}
