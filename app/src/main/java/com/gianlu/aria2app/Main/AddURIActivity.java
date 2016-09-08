package com.gianlu.aria2app.Main;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.ArrayMap;
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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddURIActivity extends AppCompatActivity {
    private List<String> urisList = new ArrayList<>();
    private EditText position;
    private Map<String, String> options = new ArrayMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_uri);
        setTitle(R.string.uri);

        final ListView uris = (ListView) findViewById(R.id.addURI_uris);
        assert uris != null;
        ImageButton addUri = (ImageButton) findViewById(R.id.addURI_newUri);
        assert addUri != null;
        position = (EditText) findViewById(R.id.addURI_position);

        addUri.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText uri = new EditText(AddURIActivity.this);
                uri.setInputType(InputType.TYPE_TEXT_VARIATION_URI);

                Utils.showDialog(AddURIActivity.this, new AlertDialog.Builder(AddURIActivity.this)
                        .setView(uri)
                        .setTitle(R.string.uri)
                        .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (uri.getText().toString().trim().isEmpty()) return;
                                urisList.add(uri.getText().toString().trim());
                                uris.setAdapter(new URIAdapter(AddURIActivity.this, urisList));
                            }
                        }));
            }
        });

        Button options = (Button) findViewById(R.id.addURI_options);
        assert options != null;
        options.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOptionsDialog();
            }
        });

        addUri.performClick();
    }

    private void buildDialog(List<OptionHeader> headers, final Map<OptionHeader, OptionChild> children) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        @SuppressLint("InflateParams") final View view = getLayoutInflater().inflate(R.layout.options_dialog, null);
        ((ViewGroup) view).removeView(view.findViewById(R.id.optionsDialog_info));
        ExpandableListView listView = (ExpandableListView) view.findViewById(R.id.moreAboutDownload_dialog_expandableListView);
        listView.setAdapter(new OptionAdapter(this, headers, children));

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
        Utils.showDialog(this, dialog);
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
        Utils.showDialog(this, pd);

        try {
            if (options.isEmpty()) {
                JTA2 jta2 = JTA2.newInstance(this);

                jta2.getGlobalOption(new IOption() {
                    @Override
                    public void onOptions(Map<String, String> options) {
                        LocalParser localOptions;
                        try {
                            localOptions = new LocalParser(AddURIActivity.this, false);
                        } catch (IOException | JSONException ex) {
                            pd.dismiss();
                            Utils.UIToast(AddURIActivity.this, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex);
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
                                Utils.UIToast(AddURIActivity.this, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex);
                            }
                        }

                        AddURIActivity.this.runOnUiThread(new Runnable() {
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
                        Utils.UIToast(AddURIActivity.this, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception);
                    }
                });
            } else {
                LocalParser localOptions;
                try {
                    localOptions = new LocalParser(AddURIActivity.this, false);
                } catch (IOException | JSONException ex) {
                    pd.dismiss();
                    Utils.UIToast(AddURIActivity.this, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex);
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
                        Utils.UIToast(AddURIActivity.this, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex);
                    }
                }

                AddURIActivity.this.runOnUiThread(new Runnable() {
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
        } catch (Exception ex) {
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

                final ProgressDialog pd = Utils.fastProgressDialog(this, R.string.gathering_information, true, false);

                if (urisList.size() == 0) break;
                Utils.showDialog(this, pd);

                if (Analytics.isTrackingAllowed(this))
                    Analytics.getDefaultTracker(this.getApplication()).send(new HitBuilders.EventBuilder()
                            .setCategory(Analytics.CATEGORY_USER_INPUT)
                            .setAction(Analytics.ACTION_NEW_URI).build());

                jta2.addUri(urisList, getPosition(), getOptions(), new IGID() {
                    @Override
                    public void onGID(String GID) {
                        pd.dismiss();
                        AddURIActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                AddURIActivity.this.onBackPressed();
                            }
                        });
                    }

                    @Override
                    public void onException(Exception ex) {
                        pd.dismiss();
                        Utils.UIToast(AddURIActivity.this, Utils.TOAST_MESSAGES.FAILED_ADD_DOWNLOAD, ex);
                    }
                });
        }

        return true;
    }
}
