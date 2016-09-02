package com.gianlu.aria2app.Main;

import android.annotation.SuppressLint;
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
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.gianlu.aria2app.Google.Analytics;
import com.gianlu.aria2app.MainActivity;
import com.gianlu.aria2app.NetIO.JTA2.IGID;
import com.gianlu.aria2app.NetIO.JTA2.IOption;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.Options.LocalParser;
import com.gianlu.aria2app.Options.OptionAdapter;
import com.gianlu.aria2app.Options.OptionChild;
import com.gianlu.aria2app.Options.OptionHeader;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.google.android.gms.analytics.HitBuilders;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddTorrentActivity extends AppCompatActivity {
    private List<String> urisList = new ArrayList<>();
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
                AlertDialog.Builder dialog = new AlertDialog.Builder(AddTorrentActivity.this);
                final EditText uri = new EditText(AddTorrentActivity.this);
                uri.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
                dialog.setView(uri)
                        .setTitle(R.string.uri)
                        .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (uri.getText().toString().trim().isEmpty()) return;
                                urisList.add(uri.getText().toString().trim());
                                uris.setAdapter(new URIAdapter(AddTorrentActivity.this, urisList));
                            }
                        }).create().show();
            }
        });


        Button options = (Button) findViewById(R.id.addTorrent_options);
        assert options != null;
        options.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOptionsDialog();
            }
        });
    }

    private void buildDialog(List<OptionHeader> headers, final Map<OptionHeader, OptionChild> children) {
        int colorRes;
        if (getIntent().getBooleanExtra("torrentMode", true))
            colorRes = R.color.colorTorrent_pressed;
        else
            colorRes = R.color.colorMetalink_pressed;

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        @SuppressLint("InflateParams") final View view = getLayoutInflater().inflate(R.layout.options_dialog, null);
        ((ViewGroup) view).removeView(view.findViewById(R.id.optionsDialog_info));
        ExpandableListView listView = (ExpandableListView) view.findViewById(R.id.moreAboutDownload_dialog_expandableListView);
        listView.setAdapter(new OptionAdapter(this, colorRes, headers, children));

        builder.setView(view)
                .setTitle(R.string.menu_globalOptions)
                .setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        for (Map.Entry<OptionHeader, OptionChild> item : children.entrySet()) {
                            if (!item.getValue().isChanged()) continue;
                            options.put(item.getKey().getOptionName(), item.getValue().getValue());
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                });

        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        ViewTreeObserver vto = view.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                dialog.getWindow().setLayout(dialog.getWindow().getDecorView().getWidth(), dialog.getWindow().getDecorView().getHeight());
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    private void showOptionsDialog() {
        final List<OptionHeader> headers = new ArrayList<>();
        final Map<OptionHeader, OptionChild> children = new HashMap<>();

        final ProgressDialog pd = Utils.fastProgressDialog(this, R.string.gathering_information, true, false);
        pd.show();

        try {
            if (options.isEmpty()) {
                JTA2 jta2 = JTA2.newInstance(this);

                jta2.getGlobalOption(new IOption() {
                    @Override
                    public void onOptions(Map<String, String> options) {
                        LocalParser localOptions;
                        try {
                            localOptions = new LocalParser(AddTorrentActivity.this, false);
                        } catch (IOException | JSONException ex) {
                            pd.dismiss();
                            Utils.UIToast(AddTorrentActivity.this, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex);
                            return;
                        }

                        for (String resOption : getResources().getStringArray(R.array.downloadOptions)) {
                            try {
                                OptionHeader header = new OptionHeader(resOption, localOptions.getCommandLine(resOption), options.get(resOption), false);
                                headers.add(header);

                                children.put(header, new OptionChild(
                                        localOptions.getDefinition(resOption),
                                        String.valueOf(localOptions.getDefaultValue(resOption)),
                                        String.valueOf(options.get(resOption))));
                            } catch (JSONException ex) {
                                pd.dismiss();
                                Utils.UIToast(AddTorrentActivity.this, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex);
                            }
                        }

                        AddTorrentActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                buildDialog(headers, children);
                            }
                        });

                        pd.dismiss();
                    }

                    @Override
                    public void onException(Exception exception) {
                        pd.dismiss();
                        Utils.UIToast(AddTorrentActivity.this, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception);
                    }
                });
            } else {
                LocalParser localOptions;
                try {
                    localOptions = new LocalParser(AddTorrentActivity.this, false);
                } catch (IOException | JSONException ex) {
                    pd.dismiss();
                    Utils.UIToast(AddTorrentActivity.this, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex);
                    return;
                }

                for (String resOption : getResources().getStringArray(R.array.downloadOptions)) {
                    try {
                        OptionHeader header = new OptionHeader(resOption, localOptions.getCommandLine(resOption), options.get(resOption), false);
                        headers.add(header);

                        children.put(header, new OptionChild(
                                localOptions.getDefinition(resOption),
                                String.valueOf(localOptions.getDefaultValue(resOption)),
                                String.valueOf(options.get(resOption))));
                    } catch (JSONException ex) {
                        pd.dismiss();
                        Utils.UIToast(AddTorrentActivity.this, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex);
                    }
                }

                AddTorrentActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        buildDialog(headers, children);
                    }
                });

                pd.dismiss();
            }
        } catch (IOException | NoSuchAlgorithmException ex) {
            Utils.UIToast(this, Utils.TOAST_MESSAGES.WS_EXCEPTION, ex);
            pd.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_download, menu);
        return true;
    }

    public Integer getPosition() {
        try {
            return Integer.parseInt(position.getText().toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public Map<String, String> getOptions() {
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
                    Utils.UIToast(this, Utils.TOAST_MESSAGES.WS_EXCEPTION, ex);
                    return true;
                }

                if (data == null) break;

                final ProgressDialog pd = Utils.fastProgressDialog(this, R.string.gathering_information, true, false);
                pd.show();

                InputStream in;
                try {
                    in = getContentResolver().openInputStream(data);
                } catch (FileNotFoundException ex) {
                    pd.dismiss();
                    Utils.UIToast(AddTorrentActivity.this, Utils.TOAST_MESSAGES.FAILED_ADD_DOWNLOAD, ex);
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
                    Utils.UIToast(AddTorrentActivity.this, Utils.TOAST_MESSAGES.FAILED_ADD_DOWNLOAD, ex);
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
                            Utils.UIToast(AddTorrentActivity.this, Utils.TOAST_MESSAGES.FAILED_ADD_DOWNLOAD, ex);
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
                            Utils.UIToast(AddTorrentActivity.this, Utils.TOAST_MESSAGES.FAILED_ADD_DOWNLOAD, ex);
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
