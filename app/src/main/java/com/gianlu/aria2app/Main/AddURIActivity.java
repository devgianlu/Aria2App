package com.gianlu.aria2app.Main;

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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import com.gianlu.aria2app.Google.Analytics;
import com.gianlu.aria2app.MainActivity;
import com.gianlu.aria2app.NetIO.JTA2.IGID;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.Options.OptionsDialog;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.google.android.gms.analytics.HitBuilders;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AddURIActivity extends AppCompatActivity {
    private final List<String> urisList = new ArrayList<>();
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

                CommonUtils.showDialog(AddURIActivity.this, new AlertDialog.Builder(AddURIActivity.this)
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
                new OptionsDialog(AddURIActivity.this, R.array.downloadOptions, false, new OptionsDialog.IDialog() {
                    @Override
                    public void onApply(JTA2 jta2, Map<String, String> options) {
                        AddURIActivity.this.options = options;
                    }
                }).hideHearts().showDialog();
            }
        });

        addUri.performClick();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_download, menu);
        return true;
    }

    private Integer getPosition() {
        try {
            return Integer.parseInt(position.getText().toString());
        } catch (Exception ex) {
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

                final ProgressDialog pd = CommonUtils.fastIndeterminateProgressDialog(this, R.string.gathering_information);

                if (urisList.size() == 0) break;
                CommonUtils.showDialog(this, pd);

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
                        CommonUtils.UIToast(AddURIActivity.this, Utils.ToastMessages.FAILED_ADD_DOWNLOAD, ex);
                    }
                });
        }

        return true;
    }
}
