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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.gianlu.aria2app.MainActivity;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.Options.Option;
import com.gianlu.aria2app.Options.OptionsAdapter;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.ThisApplication;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.google.android.gms.analytics.HitBuilders;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddTorrentActivity extends AppCompatActivity {
    private static final int READ_STORAGE_REQUEST_CODE = 1;
    private URIsAdapter urisAdapter;
    private int position;
    private Uri fileUri;
    private TextView filePath;
    private OptionsAdapter optionsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(getIntent().getBooleanExtra("torrentMode", true) ? R.style.AddTorrentTheme : R.style.AddMetalinkTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_torrent);
        setTitle(getIntent().getBooleanExtra("torrentMode", true) ? R.string.torrent : R.string.metalink);


        urisAdapter = new URIsAdapter(this, new ArrayList<String>(), null);
        ListView uris = (ListView) findViewById(R.id.addTorrent_urisList);
        uris.setAdapter(urisAdapter);

        filePath = (TextView) findViewById(R.id.addTorrent_pickHelp);
        Button pick = (Button) findViewById(R.id.addTorrent_pick);
        pick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent pickFile = new Intent(Intent.ACTION_GET_CONTENT)
                        .setType(getIntent().getBooleanExtra("torrentMode", true) ? "application/x-bittorrent" : "application/metalink4+xml")
                        .addCategory(Intent.CATEGORY_OPENABLE);

                startActivityForResult(Intent.createChooser(pickFile, getString(getIntent().getBooleanExtra("torrentMode", true) ? R.string.pickTorrent : R.string.pickMetalink)), 1);
            }
        });

        ImageButton addUri = (ImageButton) findViewById(R.id.addTorrent_newUri);
        addUri.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText newUri = new EditText(AddTorrentActivity.this);
                newUri.setInputType(InputType.TYPE_TEXT_VARIATION_URI);

                CommonUtils.showDialog(AddTorrentActivity.this, new AlertDialog.Builder(AddTorrentActivity.this)
                        .setTitle(R.string.addUri)
                        .setView(newUri)
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                            }
                        })
                        .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                urisAdapter.add(newUri.getText().toString().trim());
                            }
                        }));
            }
        });
        EditText queuePosition = (EditText) findViewById(R.id.addTorrent_position);
        queuePosition.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    position = Integer.parseInt(editable.toString());
                } catch (Exception ex) {
                    position = 0;
                }
            }
        });

        final JTA2 jta2;
        try {
            jta2 = JTA2.newInstance(this);
        } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyManagementException | KeyStoreException ex) {
            CommonUtils.UIToast(this, Utils.ToastMessages.WS_EXCEPTION, ex);
            return;
        }

        final ProgressDialog pd = CommonUtils.fastIndeterminateProgressDialog(this, R.string.gathering_information);
        CommonUtils.showDialog(this, pd);

        jta2.getGlobalOption(new JTA2.IOption() {
            @Override
            public void onOptions(Map<String, String> options) {
                final List<Option> optionsList = new ArrayList<>();

                for (String resLongOption : getResources().getStringArray(R.array.globalOptions)) {
                    String optionVal = options.get(resLongOption);
                    if (optionVal != null) {
                        optionsList.add(new Option(resLongOption, optionVal, false));
                    }
                }

                pd.dismiss();

                final RecyclerView list = (RecyclerView) findViewById(R.id.optionsDialog_list);
                list.setLayoutManager(new LinearLayoutManager(AddTorrentActivity.this, LinearLayoutManager.VERTICAL, false));
                final EditText query = (EditText) findViewById(R.id.optionsDialog_query);
                final ImageButton search = (ImageButton) findViewById(R.id.optionsDialog_search);

                AddTorrentActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        optionsAdapter = new OptionsAdapter(AddTorrentActivity.this, optionsList, false, true, false);
                        list.setAdapter(optionsAdapter);

                        search.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                list.scrollToPosition(0);
                                optionsAdapter.getFilter().filter(query.getText().toString().trim());
                            }
                        });
                    }
                });

                query.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        search.callOnClick();
                    }
                });
            }

            @Override
            public void onException(Exception exception) {
                pd.dismiss();
                CommonUtils.UIToast(AddTorrentActivity.this, Utils.ToastMessages.FAILED_GATHERING_INFORMATION, exception);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_download, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }

    private void done() {
        if (fileUri == null) {
            CommonUtils.UIToast(this, Utils.ToastMessages.INVALID_FILE);
            return;
        }

        Utils.requestReadPermission(this, READ_STORAGE_REQUEST_CODE);

        final ProgressDialog pd = CommonUtils.fastIndeterminateProgressDialog(this, R.string.gathering_information);
        CommonUtils.showDialog(this, pd);

        final JTA2 jta2;
        try {
            jta2 = JTA2.newInstance(this);
        } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyManagementException | KeyStoreException ex) {
            pd.dismiss();
            CommonUtils.UIToast(this, Utils.ToastMessages.WS_EXCEPTION, ex);
            return;
        }

        InputStream in;
        try {
            in = getContentResolver().openInputStream(fileUri);
        } catch (FileNotFoundException ex) {
            pd.dismiss();
            CommonUtils.UIToast(AddTorrentActivity.this, Utils.ToastMessages.INVALID_FILE, ex);
            return;
        }

        if (in == null) {
            pd.dismiss();
            CommonUtils.UIToast(this, Utils.ToastMessages.INVALID_FILE);
            return;
        }

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
            return;
        }

        Map<String, String> options = new HashMap<>();

        if (optionsAdapter != null) {
            for (Option item : optionsAdapter.getOptions()) {
                if (item.useMe)
                    options.put(item.longName, item.newValue);
            }
        }

        JTA2.IGID handler = new JTA2.IGID() {
            @Override
            public void onGID(String gid) {
                pd.dismiss();
                CommonUtils.UIToast(AddTorrentActivity.this, Utils.ToastMessages.DOWNLOAD_ADDED, gid);
                onBackPressed();
            }

            @Override
            public void onException(Exception ex) {
                pd.dismiss();
                CommonUtils.UIToast(AddTorrentActivity.this, Utils.ToastMessages.FAILED_ADD_DOWNLOAD, ex);
            }
        };

        if (getIntent().getBooleanExtra("torrentMode", true)) {
            ThisApplication.sendAnalytics(this, new HitBuilders.EventBuilder()
                    .setCategory(ThisApplication.CATEGORY_USER_INPUT)
                    .setAction(ThisApplication.ACTION_NEW_TORRENT).build());

            jta2.addTorrent(base64, urisAdapter.getURIs(), options, position, handler);
        } else {
            ThisApplication.sendAnalytics(this, new HitBuilders.EventBuilder()
                    .setCategory(ThisApplication.CATEGORY_USER_INPUT)
                    .setAction(ThisApplication.ACTION_NEW_METALINK).build());

            jta2.addMetalink(base64, urisAdapter.getURIs(), options, position, handler);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.addDownloadMenu_done:
                done();
                break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 1:
                if (resultCode == Activity.RESULT_OK) {
                    fileUri = data.getData();

                    String fileName = null;
                    String scheme = data.getData().getScheme();
                    if (scheme.equals("file")) {
                        fileName = data.getData().getLastPathSegment();
                    } else if (scheme.equals("content")) {
                        Cursor cursor = this.getContentResolver().query(data.getData(), new String[]{MediaStore.Images.Media.DISPLAY_NAME}, null, null, null);
                        if (cursor != null && cursor.getCount() != 0) {
                            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                            cursor.moveToFirst();
                            fileName = cursor.getString(columnIndex);
                        }
                        if (cursor != null) {
                            cursor.close();
                        }
                    }

                    filePath.setText(fileName);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
