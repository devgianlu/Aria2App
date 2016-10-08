package com.gianlu.aria2app.Main;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.ArrayMap;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.gianlu.aria2app.Google.Analytics;
import com.gianlu.aria2app.MainActivity;
import com.gianlu.aria2app.NetIO.JTA2.IGID;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.Options.OptionsDialog;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.google.android.gms.analytics.HitBuilders;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AddTorrentActivity extends AppCompatActivity {
    private final List<String> urisList = new ArrayList<>();
    private EditText position;
    private TextView path;
    private Uri data;
    private Map<String, String> options = new ArrayMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(getIntent().getBooleanExtra("torrentMode", true) ? R.style.AddTorrentTheme : R.style.AddMetalinkTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_torrent);
        setTitle(getIntent().getBooleanExtra("torrentMode", true) ? R.string.torrent : R.string.metalink);

        final ListView uris = (ListView) findViewById(R.id.addTorrent_uris);
        assert uris != null;
        final Button pick = (Button) findViewById(R.id.addTorrent_pick);
        assert pick != null;
        path = (TextView) findViewById(R.id.addTorrent_pickHelp);
        ImageButton addUri = (ImageButton) findViewById(R.id.addTorrent_newUri);
        assert addUri != null;
        position = (EditText) findViewById(R.id.addTorrent_position);

        pick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent pickFile = new Intent(Intent.ACTION_GET_CONTENT)
                        .setType(getIntent().getBooleanExtra("torrentMode", true) ? "application/x-bittorrent" : "application/metalink4+xml")
                        .addCategory(Intent.CATEGORY_OPENABLE);

                AddTorrentActivity.this.startActivityForResult(Intent.createChooser(pickFile, getString(getIntent().getBooleanExtra("torrentMode", true) ? R.string.pickTorrent : R.string.pickMetalink)), 1);
            }
        });

        addUri.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText uri = new EditText(AddTorrentActivity.this);
                uri.setInputType(InputType.TYPE_TEXT_VARIATION_URI);

                CommonUtils.showDialog(AddTorrentActivity.this, new AlertDialog.Builder(AddTorrentActivity.this).setView(uri)
                        .setTitle(R.string.uri)
                        .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (uri.getText().toString().trim().isEmpty()) return;
                                urisList.add(uri.getText().toString().trim());
                                uris.setAdapter(new URIAdapter(AddTorrentActivity.this, urisList));
                            }
                        }));
            }
        });


        Button options = (Button) findViewById(R.id.addTorrent_options);
        assert options != null;
        options.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new OptionsDialog(AddTorrentActivity.this, R.array.downloadOptions, false, new OptionsDialog.IDialog() {
                    @Override
                    public void onApply(JTA2 jta2, Map<String, String> options) {
                        AddTorrentActivity.this.options = options;
                    }
                }).hideHearts().showDialog();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_download, menu);
        return true;
    }

    private Integer getPosition() {
        try {
            return Integer.parseInt(position.getText().toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Map<String, String> getOptions() {
        return options;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finishActivity(0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.addDownloadMenu_done:
                JTA2 jta2;
                try {
                    jta2 = JTA2.newInstance(this);
                } catch (IOException | NoSuchAlgorithmException ex) {
                    CommonUtils.UIToast(this, Utils.ToastMessages.WS_EXCEPTION, ex);
                    return true;
                }

                if (data == null) break;

                final ProgressDialog pd = CommonUtils.fastIndeterminateProgressDialog(this, R.string.gathering_information);
                CommonUtils.showDialog(this, pd);

                InputStream in;
                try {
                    in = getContentResolver().openInputStream(data);
                } catch (FileNotFoundException ex) {
                    pd.dismiss();
                    CommonUtils.UIToast(AddTorrentActivity.this, Utils.ToastMessages.FAILED_ADD_DOWNLOAD, ex);
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
                    pd.dismiss();
                    CommonUtils.UIToast(AddTorrentActivity.this, Utils.ToastMessages.FAILED_ADD_DOWNLOAD, ex);
                    break;
                }

                if (getIntent().getBooleanExtra("torrentMode", true)) {
                    if (Analytics.isTrackingAllowed(this))
                        Analytics.getDefaultTracker(this.getApplication()).send(new HitBuilders.EventBuilder()
                                .setCategory(Analytics.CATEGORY_USER_INPUT)
                                .setAction(Analytics.ACTION_NEW_TORRENT).build());

                    jta2.addTorrent(base64, urisList, getOptions(), getPosition(), new IGID() {
                        @Override
                        public void onGID(String GID) {
                            pd.dismiss();
                            AddTorrentActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    AddTorrentActivity.this.onBackPressed();
                                }
                            });
                        }

                        @Override
                        public void onException(Exception ex) {
                            pd.dismiss();
                            CommonUtils.UIToast(AddTorrentActivity.this, Utils.ToastMessages.FAILED_ADD_DOWNLOAD, ex);
                        }
                    });
                } else {
                    if (Analytics.isTrackingAllowed(this))
                        Analytics.getDefaultTracker(this.getApplication()).send(new HitBuilders.EventBuilder()
                                .setCategory(Analytics.CATEGORY_USER_INPUT)
                                .setAction(Analytics.ACTION_NEW_METALINK).build());

                    jta2.addMetalink(base64, urisList, getOptions(), getPosition(), new IGID() {
                        @Override
                        public void onGID(String GID) {
                            pd.dismiss();
                            AddTorrentActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    AddTorrentActivity.this.onBackPressed();
                                }
                            });
                        }

                        @Override
                        public void onException(Exception ex) {
                            pd.dismiss();
                            CommonUtils.UIToast(AddTorrentActivity.this, Utils.ToastMessages.FAILED_ADD_DOWNLOAD, ex);
                        }
                    });
                }

                pd.dismiss();
                break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 1:
                if (resultCode == Activity.RESULT_OK) {
                    this.data = data.getData();

                    String fileName = null;
                    String scheme = data.getData().getScheme();
                    if (scheme.equals("file")) {
                        fileName = data.getData().getLastPathSegment();
                    } else if (scheme.equals("content")) {
                        String[] proj = {MediaStore.Images.Media.DISPLAY_NAME};
                        Cursor cursor = this.getContentResolver().query(data.getData(), proj, null, null, null);
                        if (cursor != null && cursor.getCount() != 0) {
                            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                            cursor.moveToFirst();
                            fileName = cursor.getString(columnIndex);
                        }
                        if (cursor != null) {
                            cursor.close();
                        }
                    }

                    path.setText(fileName);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
