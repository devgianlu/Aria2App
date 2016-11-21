package com.gianlu.aria2app.Main;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import com.gianlu.aria2app.MainActivity;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.Options.Option;
import com.gianlu.aria2app.Options.OptionsAdapter;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.ThisApplication;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.google.android.gms.analytics.HitBuilders;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddURIActivity extends AppCompatActivity {
    private URIsAdapter urisAdapter;
    private int position;
    private String filePath;
    private OptionsAdapter optionsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_uri);
        setTitle(R.string.uri);

        final AutoCompleteTextView fileName = (AutoCompleteTextView) findViewById(R.id.addURI_fileName);
        fileName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String path = editable.toString();
                if (path.isEmpty())
                    filePath = null;
                else
                    filePath = path;
            }
        });

        urisAdapter = new URIsAdapter(this, new ArrayList<String>(), new URIsAdapter.IAdapter() {
            @Override
            public void onListChanged(List<String> uris) {
                List<String> uriNames = new ArrayList<>();

                for (String uri : uris) {
                    String[] split = uri.split("/");
                    if (split.length <= 1)
                        continue;
                    uriNames.add(split[split.length - 1]);
                }

                fileName.setAdapter(new ArrayAdapter<>(AddURIActivity.this, android.R.layout.simple_list_item_1, uriNames));
            }
        });
        ListView uris = (ListView) findViewById(R.id.addURI_urisList);
        uris.setAdapter(urisAdapter);

        ImageButton addUri = (ImageButton) findViewById(R.id.addURI_newUri);
        addUri.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText newUri = new EditText(AddURIActivity.this);
                newUri.setInputType(InputType.TYPE_TEXT_VARIATION_URI);

                CommonUtils.showDialog(AddURIActivity.this, new AlertDialog.Builder(AddURIActivity.this)
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
        EditText queuePosition = (EditText) findViewById(R.id.addURI_position);
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
                list.setLayoutManager(new LinearLayoutManager(AddURIActivity.this, LinearLayoutManager.VERTICAL, false));
                final EditText query = (EditText) findViewById(R.id.optionsDialog_query);
                final ImageButton search = (ImageButton) findViewById(R.id.optionsDialog_search);

                AddURIActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        optionsAdapter = new OptionsAdapter(AddURIActivity.this, optionsList, false, true, false);
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
                CommonUtils.UIToast(AddURIActivity.this, Utils.ToastMessages.FAILED_GATHERING_INFORMATION, exception);
            }
        });

        addUri.performClick();
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
        if (urisAdapter.getURIs().size() == 0) {
            CommonUtils.UIToast(this, Utils.ToastMessages.NO_URIS);
            return;
        }

        final ProgressDialog pd = CommonUtils.fastIndeterminateProgressDialog(this, R.string.gathering_information);
        CommonUtils.showDialog(this, pd);

        ThisApplication.sendAnalytics(this, new HitBuilders.EventBuilder()
                .setCategory(ThisApplication.CATEGORY_USER_INPUT)
                .setAction(ThisApplication.ACTION_NEW_URI).build());

        final JTA2 jta2;
        try {
            jta2 = JTA2.newInstance(this);
        } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyManagementException | KeyStoreException ex) {
            pd.dismiss();
            CommonUtils.UIToast(this, Utils.ToastMessages.WS_EXCEPTION, ex);
            return;
        }

        Map<String, String> options = new HashMap<>();

        if (optionsAdapter != null) {
            for (Option item : optionsAdapter.getOptions()) {
                if (item.useMe)
                    options.put(item.longName, item.newValue);
            }
        }

        if (filePath != null) {
            if (options.containsKey("out"))
                options.remove("out");

            if (filePath.startsWith("/") || filePath.startsWith("\\"))
                filePath = filePath.substring(1, filePath.length());

            options.put("out", filePath);
        }

        jta2.addUri(urisAdapter.getURIs(), position, options, new JTA2.IGID() {
            @Override
            public void onGID(String gid) {
                pd.dismiss();
                CommonUtils.UIToast(AddURIActivity.this, Utils.ToastMessages.DOWNLOAD_ADDED, gid);
                onBackPressed();
            }

            @Override
            public void onException(Exception ex) {
                pd.dismiss();
                CommonUtils.UIToast(AddURIActivity.this, Utils.ToastMessages.FAILED_ADD_DOWNLOAD, ex);
            }
        });
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
}
