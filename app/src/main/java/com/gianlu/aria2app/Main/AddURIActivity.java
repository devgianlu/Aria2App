package com.gianlu.aria2app.Main;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ListView;

import com.gianlu.aria2app.Google.Analytics;
import com.gianlu.aria2app.MainActivity;
import com.gianlu.aria2app.NetIO.JTA2.IGID;
import com.gianlu.aria2app.NetIO.JTA2.IOption;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.Options.BooleanOptionChild;
import com.gianlu.aria2app.Options.IntegerOptionChild;
import com.gianlu.aria2app.Options.LocalParser;
import com.gianlu.aria2app.Options.MultipleOptionChild;
import com.gianlu.aria2app.Options.OptionAdapter;
import com.gianlu.aria2app.Options.OptionChild;
import com.gianlu.aria2app.Options.OptionHeader;
import com.gianlu.aria2app.Options.SourceOption;
import com.gianlu.aria2app.Options.StringOptionChild;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.google.android.gms.analytics.HitBuilders;

import org.json.JSONException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddURIActivity extends AppCompatActivity {
    private List<String> urisList = new ArrayList<>();
    private EditText position;
    private ExpandableListView optionsListView;

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
        optionsListView = (ExpandableListView) findViewById(R.id.addURI_options);

        addUri.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(AddURIActivity.this);
                final EditText uri = new EditText(AddURIActivity.this);
                uri.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
                dialog.setView(uri)
                        .setTitle(R.string.uri)
                        .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (uri.getText().toString().trim().isEmpty()) return;
                                urisList.add(uri.getText().toString().trim());
                                uris.setAdapter(new URIAdapter(AddURIActivity.this, urisList));
                            }
                        }).create().show();
            }
        });

        try {
            final List<OptionHeader> headers = new ArrayList<>();
            final Map<OptionHeader, OptionChild> children = new HashMap<>();

            JTA2 jta2 = Utils.readyJTA2(this);

            final ProgressDialog pd = Utils.fastProgressDialog(this, R.string.gathering_information, true, false);
            pd.show();

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

                            if (getResources().getIdentifier("__" + resOption.replace("-", "_"), "array", "com.gianlu.aria2app") == 0) {
                                children.put(header, new StringOptionChild(
                                        localOptions.getDefinition(resOption),
                                        String.valueOf(localOptions.getDefaultValue(resOption)),
                                        String.valueOf(options.get(resOption))));
                                continue;
                            }

                            switch (SourceOption.OPTION_TYPE.valueOf(getResources().getStringArray(getResources().getIdentifier("__" + resOption.replace("-", "_"), "array", "com.gianlu.aria2app"))[0])) {
                                case INTEGER:
                                    children.put(header, new IntegerOptionChild(
                                            localOptions.getDefinition(resOption),
                                            Utils.parseInt(localOptions.getDefaultValue(resOption)),
                                            Utils.parseInt(options.get(resOption))));
                                    break;
                                case BOOLEAN:
                                    children.put(header, new BooleanOptionChild(
                                            localOptions.getDefinition(resOption),
                                            Utils.parseBoolean(localOptions.getDefaultValue(resOption)),
                                            Utils.parseBoolean(options.get(resOption))));
                                    break;
                                case STRING:
                                    children.put(header, new StringOptionChild(
                                            localOptions.getDefinition(resOption),
                                            String.valueOf(localOptions.getDefaultValue(resOption)),
                                            String.valueOf(options.get(resOption))));
                                    break;
                                case MULTIPLE:
                                    children.put(header, new MultipleOptionChild(
                                            localOptions.getDefinition(resOption),
                                            String.valueOf(localOptions.getDefaultValue(resOption)),
                                            String.valueOf(options.get(resOption)),
                                            Arrays.asList(
                                                    getResources().getStringArray(
                                                            getResources().getIdentifier("__" + resOption.replace("-", "_"), "array", "com.gianlu.aria2app"))[1].split(","))));
                                    break;
                            }
                        } catch (JSONException ex) {
                            pd.dismiss();
                            Utils.UIToast(AddURIActivity.this, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex);
                        }
                    }

                    pd.dismiss();
                }

                @Override
                public void onException(Exception exception) {
                    pd.dismiss();
                    Utils.UIToast(AddURIActivity.this, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception);
                }
            });

            optionsListView.setAdapter(new OptionAdapter(this, headers, children));
        } catch (IOException | NoSuchAlgorithmException ex) {
            Utils.UIToast(this, Utils.TOAST_MESSAGES.WS_EXCEPTION, ex);
        }

        addUri.performClick();
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
        Map<String, String> map = new HashMap<>();

        for (Map.Entry<OptionHeader, OptionChild> item : ((OptionAdapter) optionsListView.getExpandableListAdapter()).getChildren().entrySet()) {
            if (!item.getValue().isChanged()) continue;
            map.put(item.getKey().getOptionName(), item.getValue().getStringValue());
        }

        return map;
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
                    jta2 = Utils.readyJTA2(this);
                } catch (IOException | NoSuchAlgorithmException ex) {
                    Utils.UIToast(this, Utils.TOAST_MESSAGES.WS_EXCEPTION, ex);
                    return true;
                }

                final ProgressDialog pd = Utils.fastProgressDialog(this, R.string.gathering_information, true, false);

                if (urisList.size() == 0) break;
                pd.show();

                if (Analytics.isTrackingAllowed(this))
                    Analytics.getDefaultTracker(this.getApplication()).send(new HitBuilders.EventBuilder()
                            .setCategory(Analytics.CATEGORY_USER_INPUT)
                            .setAction(Analytics.ACTION_NEW_URI).build());

                jta2.addUri(urisList, getPosition(), getOptions(), new IGID() {
                    @Override
                    public void onGID(String GID) {
                        pd.dismiss();
                        onBackPressed();
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
