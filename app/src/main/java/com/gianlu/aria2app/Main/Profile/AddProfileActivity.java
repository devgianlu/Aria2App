package com.gianlu.aria2app.Main.Profile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.gianlu.aria2app.Google.Analytics;
import com.gianlu.aria2app.MainActivity;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.google.android.gms.analytics.HitBuilders;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AddProfileActivity extends AppCompatActivity {
    private static final int WRITE_STORAGE_REQUEST_CODE = 1;
    private boolean isEditMode = true;
    private String oldFileName;

    private EditText profileName;
    private CheckBox enableNotifications;
    private RadioGroup modeGroup;

    // Single mode
    private SingleModeViewHolder sViewHolder;
    // Multi mode
    private Map<ConnectivityCondition, SingleModeProfileItem> mProfiles = new ArrayMap<>();
    private ListView mListView;
    private Spinner mSpinner;

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, MainActivity.class).putExtra("backFromAddProfile", true));
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_profile);

        oldFileName = getIntent().getStringExtra("base64name");

        profileName = (EditText) findViewById(R.id.addProfile_name);
        enableNotifications = (CheckBox) findViewById(R.id.addProfile_enableNotifications);
        modeGroup = (RadioGroup) findViewById(R.id.addProfile_modeGroup);
        modeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (i == R.id.addProfile_singleMode) {
                    findViewById(R.id.addProfile_singleModeContainer).setVisibility(View.VISIBLE);
                    findViewById(R.id.addProfile_multiModeContainer).setVisibility(View.GONE);
                } else {
                    findViewById(R.id.addProfile_singleModeContainer).setVisibility(View.GONE);
                    findViewById(R.id.addProfile_multiModeContainer).setVisibility(View.VISIBLE);
                }
            }
        });

        // Single mode
        sViewHolder = new SingleModeViewHolder(findViewById(R.id.addProfile_singleModeContainer));

        // Multi mode
        mListView = (ListView) findViewById(R.id.addProfile_multiModeListView);
        assert mListView != null;
        mSpinner = (Spinner) findViewById(R.id.addProfile_multiModeSpinner);
        assert mSpinner != null;
        Button mAdd = (Button) findViewById(R.id.addProfile_multiModeAdd);
        assert mAdd != null;

        reloadSpinner();
        mAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createNewCondition(ConnectivityCondition.getTypeFromString(mSpinner.getSelectedItem().toString()), null);
            }
        });

        Bundle edit = getIntent().getExtras();
        if (edit.getBoolean("edit", false)) {
            setTitle(R.string.title_activity_addProfile_edit);
            isEditMode = true;
            try {
                if (edit.getBoolean("isSingleMode", true)) {
                    modeGroup.check(R.id.addProfile_singleMode);
                    loadSingle(edit.getString("base64name"));
                } else {
                    modeGroup.check(R.id.addProfile_multiMode);
                    loadMulti(edit.getString("base64name"));
                }

                modeGroup.getChildAt(0).setEnabled(false);
                modeGroup.getChildAt(1).setEnabled(false);
            } catch (FileNotFoundException ex) {
                CommonUtils.UIToast(this, Utils.ToastMessages.FILE_NOT_FOUND);
                onBackPressed();
            } catch (IOException | JSONException ex) {
                CommonUtils.UIToast(this, Utils.ToastMessages.FATAL_EXCEPTION, ex);
                ex.printStackTrace();
                onBackPressed();
            }
        } else {
            setTitle(R.string.title_activity_addProfile_new);
            isEditMode = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_profile, menu);
        if (!isEditMode) menu.getItem(0).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (getIntent().getBooleanExtra("canGoBack", true)) {
                    onBackPressed();
                } else {
                    CommonUtils.UIToast(this, Utils.ToastMessages.MUST_CREATE_FIRST_PROFILE);
                }
                break;
            case R.id.addProfileMenu_done:
                if (modeGroup.getCheckedRadioButtonId() == R.id.addProfile_singleMode)
                    createSingle();
                else
                    createMulti();
                break;
            case R.id.addProfileMenu_delete:
                if (Analytics.isTrackingAllowed(this))
                    Analytics.getDefaultTracker(getApplication()).send(new HitBuilders.EventBuilder()
                            .setCategory(Analytics.CATEGORY_USER_INPUT)
                            .setAction(Analytics.ACTION_DELETE_PROFILE)
                            .build());

                deleteFile(oldFileName);
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadSingle(String name) throws IOException, JSONException {
        SingleModeProfileItem item = SingleModeProfileItem.fromString(this, name);

        profileName.setText(item.getProfileName());
        enableNotifications.setChecked(item.areNotificationsEnabled());
        sViewHolder.addr.setText(item.getServerAddr());
        sViewHolder.port.setText(String.valueOf(item.getServerPort()));
        sViewHolder.endpoint.setText(item.getServerEndpoint());

        switch (item.getAuthMethod()) {
            case NONE:
                sViewHolder.authMethodNone.setChecked(true);
                sViewHolder.authMethodToken.setChecked(false);
                sViewHolder.authMethodHTTP.setChecked(false);

                sViewHolder.authMethodHTTPPasswdContainer.setVisibility(View.GONE);
                sViewHolder.authMethodHTTPUserContainer.setVisibility(View.GONE);
                sViewHolder.authMethodTokenToken.setVisibility(View.GONE);
                break;
            case TOKEN:
                sViewHolder.authMethodNone.setChecked(false);
                sViewHolder.authMethodToken.setChecked(true);
                sViewHolder.authMethodHTTP.setChecked(false);

                sViewHolder.authMethodTokenToken.setText(item.getServerToken());

                sViewHolder.authMethodHTTPPasswdContainer.setVisibility(View.GONE);
                sViewHolder.authMethodHTTPUserContainer.setVisibility(View.GONE);
                sViewHolder.authMethodTokenToken.setVisibility(View.VISIBLE);
                break;
            case HTTP:
                sViewHolder.authMethodNone.setChecked(false);
                sViewHolder.authMethodToken.setChecked(false);
                sViewHolder.authMethodHTTP.setChecked(true);

                sViewHolder.authMethodHTTPUsername.setText(item.getServerUsername());
                sViewHolder.authMethodHTTPPassword.setText(item.getServerPassword());

                sViewHolder.authMethodHTTPPasswdContainer.setVisibility(View.VISIBLE);
                sViewHolder.authMethodHTTPUserContainer.setVisibility(View.VISIBLE);
                sViewHolder.authMethodTokenToken.setVisibility(View.GONE);
                break;
        }
        sViewHolder.SSL.setChecked(item.isServerSSL());

        sViewHolder.directDownload.setChecked(item.isDirectDownloadEnabled());
        if (item.isDirectDownloadEnabled()) {
            sViewHolder.directDownloadAddr.setText(item.getDirectDownload().getAddress());
            sViewHolder.directDownloadAuth.setChecked(item.getDirectDownload().isAuth());
            sViewHolder.directDownloadUsername.setText(item.getDirectDownload().getUsername());
            sViewHolder.directDownloadPassword.setText(item.getDirectDownload().getPassword());
        }
    }

    private void loadMulti(String name) throws IOException, JSONException {
        MultiModeProfileItem item = MultiModeProfileItem.fromString(this, name);

        profileName.setText(item.getGlobalProfileName());
        enableNotifications.setChecked(item.areNotificationsEnabled());
        mProfiles = item.getProfiles();
        mListView.setAdapter(new ConditionsCustomAdapter(this, mProfiles, new ConditionsCustomAdapter.OnClickListener() {
            @Override
            public void onClick(SingleModeProfileItem item, ConnectivityCondition condition) {
                createNewCondition(condition.getType(), new Pair<>(condition, item));
            }
        }));
        reloadSpinner();
    }

    private void createNewCondition(final ConnectivityCondition.TYPE type, final Pair<ConnectivityCondition, SingleModeProfileItem> edit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        @SuppressLint("InflateParams") final LinearLayout view = (LinearLayout) getLayoutInflater().inflate(R.layout.new_condition_dialog, null);

        AutoCompleteTextView ssid = null;
        switch (type) {
            case WIFI:
                view.findViewById(R.id.newConditionDialog_containerWiFi).setVisibility(View.VISIBLE);
                ssid = (AutoCompleteTextView) view.findViewById(R.id.newConditionDialog_containerWiFi).findViewById(R.id.newConditionDialog_ssid);

                List<String> configuredSSIDs = new ArrayList<>();
                List<WifiConfiguration> configured_wifis = ((WifiManager) getSystemService(WIFI_SERVICE)).getConfiguredNetworks();
                if (configured_wifis == null) break;
                for (WifiConfiguration net : configured_wifis) {
                    String _ssid = net.SSID;
                    if (_ssid.startsWith("\"") && _ssid.endsWith("\"")) {
                        _ssid = _ssid.substring(1, _ssid.length() - 1);
                    }
                    configuredSSIDs.add(_ssid);
                }

                ssid.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, configuredSSIDs));

                if (edit != null) ssid.setText(edit.first.getSSID());
                break;
            case MOBILE:
            case ETHERNET:
            case BLUETOOTH:
                view.findViewById(R.id.newConditionDialog_containerWiFi).setVisibility(View.GONE);
                break;
        }

        final SingleModeViewHolder holder = new SingleModeViewHolder(view.findViewById(R.id.newConditionDialog_include));

        final MultiOnTextChangedListener listener = new MultiOnTextChangedListener(holder.completeURL, holder.addr, holder.port, holder.endpoint, holder.SSL);
        holder.addr.addTextChangedListener(listener);
        holder.port.addTextChangedListener(listener);
        holder.endpoint.addTextChangedListener(listener);
        holder.SSL.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                listener.afterTextChanged(null);
            }
        });

        builder.setPositiveButton(edit != null ? R.string.save : R.string.add, null)
                .setTitle(type.getFormal())
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                })
                .setView(view);

        if (edit != null) {
            builder.setNeutralButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mProfiles.remove(edit.first);

                    mListView.setAdapter(new ConditionsCustomAdapter(AddProfileActivity.this, mProfiles, new ConditionsCustomAdapter.OnClickListener() {
                        @Override
                        public void onClick(SingleModeProfileItem item, ConnectivityCondition condition) {
                            createNewCondition(condition.getType(), new Pair<>(condition, item));
                        }
                    }));

                    reloadSpinner();

                    dialogInterface.dismiss();
                }
            });

            holder.addr.setText(edit.second.getServerAddr());
            holder.port.setText(String.valueOf(edit.second.getServerPort()));
            holder.endpoint.setText(edit.second.getServerEndpoint());
            holder.completeURL.setText(edit.second.getFullServerAddress());
            switch (edit.second.getAuthMethod()) {
                case NONE:
                    sViewHolder.authMethodNone.setChecked(true);
                    sViewHolder.authMethodToken.setChecked(false);
                    sViewHolder.authMethodHTTP.setChecked(false);

                    sViewHolder.authMethodHTTPPasswdContainer.setVisibility(View.GONE);
                    sViewHolder.authMethodHTTPUserContainer.setVisibility(View.GONE);
                    sViewHolder.authMethodTokenToken.setVisibility(View.GONE);
                    break;
                case TOKEN:
                    sViewHolder.authMethodNone.setChecked(false);
                    sViewHolder.authMethodToken.setChecked(true);
                    sViewHolder.authMethodHTTP.setChecked(false);

                    sViewHolder.authMethodHTTPUsername.setText(edit.second.getServerUsername());
                    sViewHolder.authMethodHTTPPassword.setText(edit.second.getServerPassword());

                    sViewHolder.authMethodHTTPPasswdContainer.setVisibility(View.GONE);
                    sViewHolder.authMethodHTTPUserContainer.setVisibility(View.GONE);
                    sViewHolder.authMethodTokenToken.setVisibility(View.VISIBLE);
                    break;
                case HTTP:
                    sViewHolder.authMethodNone.setChecked(false);
                    sViewHolder.authMethodToken.setChecked(false);
                    sViewHolder.authMethodHTTP.setChecked(true);

                    sViewHolder.authMethodTokenToken.setText(edit.second.getServerToken());

                    sViewHolder.authMethodHTTPPasswdContainer.setVisibility(View.VISIBLE);
                    sViewHolder.authMethodHTTPUserContainer.setVisibility(View.VISIBLE);
                    sViewHolder.authMethodTokenToken.setVisibility(View.GONE);
                    break;
            }
            holder.SSL.setChecked(edit.second.isServerSSL());
            holder.directDownload.setChecked(edit.second.isDirectDownloadEnabled());
            if (edit.second.isDirectDownloadEnabled()) {
                holder.directDownloadAddr.setText(edit.second.getDirectDownload().getAddress());
                holder.directDownloadAuth.setChecked(edit.second.getDirectDownload().isAuth());
                holder.directDownloadUsername.setText(edit.second.getDirectDownload().getUsername());
                holder.directDownloadPassword.setText(edit.second.getDirectDownload().getPassword());
            }
        }

        final AlertDialog dialog = builder.create();
        CommonUtils.showDialog(this, dialog);
        final AutoCompleteTextView finalSsid = ssid;
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConnectivityCondition condition = null;
                switch (type) {
                    case WIFI:
                        if (finalSsid.getText().toString().trim().isEmpty()) {
                            CommonUtils.UIToast(AddProfileActivity.this, Utils.ToastMessages.INVALID_SSID);
                            return;
                        }
                        condition = ConnectivityCondition.newWiFiCondition(finalSsid.getText().toString().trim());
                        break;
                    case MOBILE:
                        condition = ConnectivityCondition.newMobileCondition();
                        break;
                    case ETHERNET:
                        condition = ConnectivityCondition.newEthernetCondition();
                        break;
                    case BLUETOOTH:
                        condition = ConnectivityCondition.newBluetoothCondition();
                        break;
                }

                if (condition == null) {
                    CommonUtils.UIToast(AddProfileActivity.this, Utils.ToastMessages.FATAL_EXCEPTION, "CONDITION is null");
                    return;
                }

                if (holder.addr.getText().toString().trim().isEmpty()) {
                    CommonUtils.UIToast(AddProfileActivity.this, Utils.ToastMessages.INVALID_SERVER_IP);
                    return;
                }

                if (!holder.port.getText().toString().trim().isEmpty()) {
                    if (0 > Integer.parseInt(holder.port.getText().toString()) && Integer.parseInt(holder.port.getText().toString()) < 65536) {
                        CommonUtils.UIToast(AddProfileActivity.this, Utils.ToastMessages.INVALID_SERVER_PORT);
                        return;
                    }
                } else {
                    CommonUtils.UIToast(AddProfileActivity.this, Utils.ToastMessages.INVALID_SERVER_PORT);
                    return;
                }

                if (holder.endpoint.getText().toString().trim().isEmpty() || (!holder.endpoint.getText().toString().startsWith("/"))) {
                    CommonUtils.UIToast(AddProfileActivity.this, Utils.ToastMessages.INVALID_SERVER_ENDPOINT);
                    return;
                }

                if (holder.authMethodToken.isChecked() && holder.authMethodTokenToken.getText().toString().trim().isEmpty()) {
                    CommonUtils.UIToast(AddProfileActivity.this, Utils.ToastMessages.INVALID_SERVER_TOKEN);
                    return;
                }

                if (holder.authMethodHTTP.isChecked() && (holder.authMethodHTTPUsername.getText().toString().trim().isEmpty() || holder.authMethodHTTPPassword.getText().toString().trim().isEmpty())) {
                    CommonUtils.UIToast(AddProfileActivity.this, Utils.ToastMessages.INVALID_SERVER_USER_OR_PASSWD);
                    return;
                }

                if (holder.directDownload.isChecked() && holder.directDownloadAddr.getText().toString().trim().isEmpty()) {
                    CommonUtils.UIToast(AddProfileActivity.this, Utils.ToastMessages.INVALID_DIRECTDOWNLOAD_ADDR);
                    return;
                }

                if (holder.directDownload.isChecked() && (!holder.directDownloadAddr.getText().toString().trim().endsWith("/"))) {
                    holder.directDownloadAddr.setText(String.format("%s/", holder.directDownloadAddr.getText().toString().trim()));
                }

                if (holder.directDownload.isChecked() && holder.directDownloadAuth.isChecked() && (holder.directDownloadUsername.getText().toString().trim().isEmpty() || holder.directDownloadPassword.getText().toString().trim().isEmpty())) {
                    CommonUtils.UIToast(AddProfileActivity.this, Utils.ToastMessages.INVALID_DIRECTDOWNLOAD_USERORPASSWD);
                    return;
                }

                SingleModeProfileItem profile;
                if (holder.authMethodNone.isChecked()) {
                    profile = new SingleModeProfileItem(condition.getFormalName(),
                            holder.addr.getText().toString().trim(),
                            Integer.parseInt(holder.port.getText().toString().trim()),
                            holder.endpoint.getText().toString().trim(),
                            holder.SSL.isChecked(),
                            enableNotifications.isChecked(),
                            holder.directDownload.isChecked(),
                            new DirectDownload(holder.directDownloadAddr.getText().toString().trim(),
                                    holder.directDownloadAuth.isChecked(),
                                    holder.directDownloadUsername.getText().toString().trim(),
                                    holder.directDownloadPassword.getText().toString().trim()));
                } else if (holder.authMethodToken.isChecked()) {
                    profile = new SingleModeProfileItem(condition.getFormalName(),
                            holder.addr.getText().toString().trim(),
                            Integer.parseInt(holder.port.getText().toString().trim()),
                            holder.endpoint.getText().toString().trim(),
                            holder.SSL.isChecked(),
                            enableNotifications.isChecked(),
                            holder.authMethodTokenToken.getText().toString().trim(),
                            holder.directDownload.isChecked(),
                            new DirectDownload(holder.directDownloadAddr.getText().toString().trim(),
                                    holder.directDownloadAuth.isChecked(),
                                    holder.directDownloadUsername.getText().toString().trim(),
                                    holder.directDownloadPassword.getText().toString().trim()));
                } else if (holder.authMethodHTTP.isChecked()) {
                    profile = new SingleModeProfileItem(condition.getFormalName(),
                            holder.addr.getText().toString().trim(),
                            Integer.parseInt(holder.port.getText().toString().trim()),
                            holder.endpoint.getText().toString().trim(),
                            holder.SSL.isChecked(),
                            enableNotifications.isChecked(),
                            holder.authMethodHTTPUsername.getText().toString().trim(),
                            holder.authMethodHTTPPassword.getText().toString().trim(),
                            holder.directDownload.isChecked(),
                            new DirectDownload(holder.directDownloadAddr.getText().toString().trim(),
                                    holder.directDownloadAuth.isChecked(),
                                    holder.directDownloadUsername.getText().toString().trim(),
                                    holder.directDownloadPassword.getText().toString().trim()));
                } else {
                    profile = new SingleModeProfileItem(condition.getFormalName(),
                            holder.addr.getText().toString().trim(),
                            Integer.parseInt(holder.port.getText().toString().trim()),
                            holder.endpoint.getText().toString().trim(),
                            holder.SSL.isChecked(),
                            enableNotifications.isChecked(),
                            holder.directDownload.isChecked(),
                            new DirectDownload(holder.directDownloadAddr.getText().toString().trim(),
                                    holder.directDownloadAuth.isChecked(),
                                    holder.directDownloadUsername.getText().toString().trim(),
                                    holder.directDownloadPassword.getText().toString().trim()));
                }

                if (edit != null && edit.first != null) mProfiles.remove(edit.first);
                mProfiles.put(condition, profile);
                mListView.setAdapter(new ConditionsCustomAdapter(AddProfileActivity.this, mProfiles, new ConditionsCustomAdapter.OnClickListener() {
                    @Override
                    public void onClick(SingleModeProfileItem item, ConnectivityCondition condition) {
                        createNewCondition(condition.getType(), new Pair<>(condition, item));
                    }
                }));

                reloadSpinner();

                dialog.dismiss();
            }
        });
    }

    private void reloadSpinner() {
        List<ConnectivityCondition.TYPE> avConds = new ArrayList<>(
                Arrays.asList(
                        ConnectivityCondition.TYPE.WIFI,
                        ConnectivityCondition.TYPE.MOBILE,
                        ConnectivityCondition.TYPE.ETHERNET,
                        ConnectivityCondition.TYPE.BLUETOOTH));

        for (ConnectivityCondition _cond : mProfiles.keySet()) {
            if (_cond.getType() == ConnectivityCondition.TYPE.MOBILE)
                avConds.remove(ConnectivityCondition.TYPE.MOBILE);
            if (_cond.getType() == ConnectivityCondition.TYPE.BLUETOOTH)
                avConds.remove(ConnectivityCondition.TYPE.BLUETOOTH);
            if (_cond.getType() == ConnectivityCondition.TYPE.ETHERNET)
                avConds.remove(ConnectivityCondition.TYPE.ETHERNET);
        }

        mSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, avConds));
    }

    private void createSingle() {
        if (profileName.getText().toString().trim().isEmpty()) {
            CommonUtils.UIToast(this, Utils.ToastMessages.INVALID_PROFILE_NAME);
            return;
        }

        if (sViewHolder.addr.getText().toString().trim().isEmpty()) {
            CommonUtils.UIToast(this, Utils.ToastMessages.INVALID_SERVER_IP);
            return;
        }

        if (!sViewHolder.port.getText().toString().trim().isEmpty()) {
            if (0 > Integer.parseInt(sViewHolder.port.getText().toString()) && Integer.parseInt(sViewHolder.port.getText().toString()) < 65536) {
                CommonUtils.UIToast(this, Utils.ToastMessages.INVALID_SERVER_PORT);
                return;
            }
        } else {
            CommonUtils.UIToast(this, Utils.ToastMessages.INVALID_SERVER_PORT);
            return;
        }

        if (sViewHolder.endpoint.getText().toString().trim().isEmpty() || (!sViewHolder.endpoint.getText().toString().startsWith("/"))) {
            CommonUtils.UIToast(this, Utils.ToastMessages.INVALID_SERVER_ENDPOINT);
            return;
        }

        if (sViewHolder.authMethodToken.isChecked() && sViewHolder.authMethodTokenToken.getText().toString().trim().isEmpty()) {
            CommonUtils.UIToast(AddProfileActivity.this, Utils.ToastMessages.INVALID_SERVER_TOKEN);
            return;
        }

        if (sViewHolder.authMethodHTTP.isChecked() && (sViewHolder.authMethodHTTPUsername.getText().toString().trim().isEmpty() || sViewHolder.authMethodHTTPPassword.getText().toString().trim().isEmpty())) {
            CommonUtils.UIToast(AddProfileActivity.this, Utils.ToastMessages.INVALID_SERVER_USER_OR_PASSWD);
            return;
        }

        if (sViewHolder.directDownload.isChecked() && sViewHolder.directDownloadAddr.getText().toString().trim().isEmpty()) {
            CommonUtils.UIToast(this, Utils.ToastMessages.INVALID_DIRECTDOWNLOAD_ADDR);
            return;
        }

        if (sViewHolder.directDownload.isChecked() && (!sViewHolder.directDownloadAddr.getText().toString().trim().endsWith("/"))) {
            sViewHolder.directDownloadAddr.setText(String.format("%s/", sViewHolder.directDownloadAddr.getText().toString().trim()));
        }

        if (sViewHolder.directDownload.isChecked() && sViewHolder.directDownloadAuth.isChecked() && (sViewHolder.directDownloadUsername.getText().toString().trim().isEmpty() || sViewHolder.directDownloadPassword.getText().toString().trim().isEmpty())) {
            CommonUtils.UIToast(this, Utils.ToastMessages.INVALID_DIRECTDOWNLOAD_USERORPASSWD);
            return;
        }


        SingleModeProfileItem profile;
        if (sViewHolder.authMethodNone.isChecked()) {
            profile = new SingleModeProfileItem(profileName.getText().toString().trim(),
                    sViewHolder.addr.getText().toString().trim(),
                    Integer.parseInt(sViewHolder.port.getText().toString().trim()),
                    sViewHolder.endpoint.getText().toString().trim(),
                    sViewHolder.SSL.isChecked(),
                    enableNotifications.isChecked(),
                    sViewHolder.directDownload.isChecked(),
                    new DirectDownload(sViewHolder.directDownloadAddr.getText().toString().trim(),
                            sViewHolder.directDownloadAuth.isChecked(),
                            sViewHolder.directDownloadUsername.getText().toString().trim(),
                            sViewHolder.directDownloadPassword.getText().toString().trim()));
        } else if (sViewHolder.authMethodToken.isChecked()) {
            profile = new SingleModeProfileItem(profileName.getText().toString().trim(),
                    sViewHolder.addr.getText().toString().trim(),
                    Integer.parseInt(sViewHolder.port.getText().toString().trim()),
                    sViewHolder.endpoint.getText().toString().trim(),
                    sViewHolder.SSL.isChecked(),
                    enableNotifications.isChecked(),
                    sViewHolder.authMethodTokenToken.getText().toString().trim(),
                    sViewHolder.directDownload.isChecked(),
                    new DirectDownload(sViewHolder.directDownloadAddr.getText().toString().trim(),
                            sViewHolder.directDownloadAuth.isChecked(),
                            sViewHolder.directDownloadUsername.getText().toString().trim(),
                            sViewHolder.directDownloadPassword.getText().toString().trim()));
        } else if (sViewHolder.authMethodHTTP.isChecked()) {
            profile = new SingleModeProfileItem(profileName.getText().toString().trim(),
                    sViewHolder.addr.getText().toString().trim(),
                    Integer.parseInt(sViewHolder.port.getText().toString().trim()),
                    sViewHolder.endpoint.getText().toString().trim(),
                    sViewHolder.SSL.isChecked(),
                    enableNotifications.isChecked(),
                    sViewHolder.authMethodHTTPUsername.getText().toString().trim(),
                    sViewHolder.authMethodHTTPPassword.getText().toString().trim(),
                    sViewHolder.directDownload.isChecked(),
                    new DirectDownload(sViewHolder.directDownloadAddr.getText().toString().trim(),
                            sViewHolder.directDownloadAuth.isChecked(),
                            sViewHolder.directDownloadUsername.getText().toString().trim(),
                            sViewHolder.directDownloadPassword.getText().toString().trim()));
        } else {
            profile = new SingleModeProfileItem(profileName.getText().toString().trim(),
                    sViewHolder.addr.getText().toString().trim(),
                    Integer.parseInt(sViewHolder.port.getText().toString().trim()),
                    sViewHolder.endpoint.getText().toString().trim(),
                    sViewHolder.SSL.isChecked(),
                    enableNotifications.isChecked(),
                    sViewHolder.directDownload.isChecked(),
                    new DirectDownload(sViewHolder.directDownloadAddr.getText().toString().trim(),
                            sViewHolder.directDownloadAuth.isChecked(),
                            sViewHolder.directDownloadUsername.getText().toString().trim(),
                            sViewHolder.directDownloadPassword.getText().toString().trim()));
        }

        if (profile.isDirectDownloadEnabled()) {
            requestWritePermission();
        }

        try {
            if (oldFileName != null)
                deleteFile(oldFileName);

            FileOutputStream fOut = openFileOutput(new String(Base64.encode(profileName.getText().toString().trim().getBytes(), Base64.NO_WRAP)) + ".profile", Context.MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fOut);

            osw.write(profile.toJSON().toString());
            osw.flush();
            osw.close();
        } catch (IOException | JSONException ex) {
            CommonUtils.UIToast(this, Utils.ToastMessages.FATAL_EXCEPTION, ex);
        }

        if (Analytics.isTrackingAllowed(this))
            Analytics.getDefaultTracker(this.getApplication()).send(new HitBuilders.EventBuilder()
                    .setCategory(Analytics.CATEGORY_USER_INPUT)
                    .setAction(Analytics.ACTION_NEW_PROFILE)
                    .setLabel("Single profile")
                    .build());

        onBackPressed();
    }

    private void requestWritePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                CommonUtils.showDialog(this, new AlertDialog.Builder(this)
                        .setTitle(R.string.writeExternalStorageRequest_title)
                        .setMessage(R.string.writeExternalStorageRequest_message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                requestWritePermission();
                            }
                        }));
            } else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_STORAGE_REQUEST_CODE);
            }
        }
    }

    private void createMulti() {
        if (profileName.getText().toString().trim().isEmpty()) {
            CommonUtils.UIToast(this, Utils.ToastMessages.INVALID_PROFILE_NAME);
            return;
        }

        if (mProfiles.size() <= 1) {
            CommonUtils.UIToast(this, Utils.ToastMessages.INVALID_CONDITIONS_NUMBER);
            return;
        }

        JSONObject profile = new JSONObject();
        try {
            JSONArray conditions = new JSONArray();
            for (ConnectivityCondition condition : mProfiles.keySet()) {
                JSONObject _profile = new JSONObject();
                _profile.put("type", condition.getType().toString());

                if (condition.getType() == ConnectivityCondition.TYPE.WIFI)
                    _profile.put("ssid", condition.getSSID());

                _profile.put("profile", mProfiles.get(condition).toJSON());
                conditions.put(_profile);
            }

            profile.put("name", profileName.getText().toString().trim())
                    .put("notificationsEnabled", enableNotifications.isChecked())
                    .put("conditions", conditions);
        } catch (JSONException ex) {
            CommonUtils.UIToast(this, Utils.ToastMessages.FATAL_EXCEPTION, ex);
            return;
        }

        for (SingleModeProfileItem _profile : mProfiles.values()) {
            if (_profile.isDirectDownloadEnabled()) {
                requestWritePermission();
                break;
            }
        }

        try {
            if (oldFileName != null)
                deleteFile(oldFileName);

            FileOutputStream fOut = openFileOutput(new String(Base64.encode(profileName.getText().toString().trim().getBytes(), Base64.NO_WRAP)) + ".profile", Context.MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fOut);

            osw.write(profile.toString());
            osw.flush();
            osw.close();
        } catch (IOException ex) {
            CommonUtils.UIToast(this, Utils.ToastMessages.FATAL_EXCEPTION, ex);
            ex.printStackTrace();
        }

        if (Analytics.isTrackingAllowed(this))
            Analytics.getDefaultTracker(this.getApplication()).send(new HitBuilders.EventBuilder()
                    .setCategory(Analytics.CATEGORY_USER_INPUT)
                    .setAction(Analytics.ACTION_NEW_PROFILE)
                    .setLabel("Multi profile")
                    .setValue(mProfiles.size())
                    .build());

        onBackPressed();
    }

    private class SingleModeViewHolder {
        final EditText addr;
        final EditText port;
        final EditText endpoint;
        final TextView completeURL;
        final RadioButton authMethodNone;
        final RadioButton authMethodToken;
        final EditText authMethodTokenToken;
        final RadioButton authMethodHTTP;
        final LinearLayout authMethodHTTPUserContainer;
        final EditText authMethodHTTPUsername;
        final LinearLayout authMethodHTTPPasswdContainer;
        final EditText authMethodHTTPPassword;
        final CheckBox SSL;
        final CheckBox directDownload;
        final LinearLayout directDownloadContainer;
        final EditText directDownloadAddr;
        final CheckBox directDownloadAuth;
        final LinearLayout directDownloadAuthContainer;
        final EditText directDownloadUsername;
        final EditText directDownloadPassword;

        SingleModeViewHolder(View rootView) {
            addr = (EditText) rootView.findViewById(R.id.addProfile_serverAddr);
            port = (EditText) rootView.findViewById(R.id.addProfile_serverPort);
            endpoint = (EditText) rootView.findViewById(R.id.addProfile_serverEndpoint);
            completeURL = (TextView) rootView.findViewById(R.id.addProfile_completeURL);
            authMethodNone = (RadioButton) rootView.findViewById(R.id.addProfile_authMethodNone);
            authMethodToken = (RadioButton) rootView.findViewById(R.id.addProfile_authMethodToken);
            authMethodTokenToken = (EditText) rootView.findViewById(R.id.addProfile_authMethodTokenToken);
            authMethodHTTP = (RadioButton) rootView.findViewById(R.id.addProfile_authMethodHTTP);

            authMethodHTTPUserContainer = (LinearLayout) rootView.findViewById(R.id.addProfile_authMethodHTTPUserContainer);
            authMethodHTTPUsername = (EditText) rootView.findViewById(R.id.addProfile_authMethodHTTPUser);
            authMethodHTTPPasswdContainer = (LinearLayout) rootView.findViewById(R.id.addProfile_authMethodHTTPPasswdContainer);
            authMethodHTTPPassword = (EditText) rootView.findViewById(R.id.addProfile_authMethodHTTPPasswd);
            SSL = (CheckBox) rootView.findViewById(R.id.addProfile_serverSSL);
            directDownload = (CheckBox) rootView.findViewById(R.id.addProfile_directDownload);
            directDownloadContainer = (LinearLayout) rootView.findViewById(R.id.addProfile_directDownloadContainer);
            directDownloadAddr = (EditText) rootView.findViewById(R.id.addProfile_directDownload_addr);
            directDownloadAuth = (CheckBox) rootView.findViewById(R.id.addProfile_directDownload_auth);
            directDownloadAuthContainer = (LinearLayout) rootView.findViewById(R.id.addProfile_directDownload_authContainer);
            directDownloadUsername = (EditText) rootView.findViewById(R.id.addProfile_directDownload_user);
            directDownloadPassword = (EditText) rootView.findViewById(R.id.addProfile_directDownload_passwd);

            //Setup
            final SingleOnTextChangedListener listener = new SingleOnTextChangedListener();
            addr.addTextChangedListener(listener);
            port.addTextChangedListener(listener);
            endpoint.addTextChangedListener(listener);
            SSL.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    listener.afterTextChanged(null);
                }
            });

            directDownload.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (b)
                        directDownloadContainer.setVisibility(View.VISIBLE);
                    else
                        directDownloadContainer.setVisibility(View.GONE);
                }
            });
            directDownloadAuth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (b)
                        directDownloadAuthContainer.setVisibility(View.VISIBLE);
                    else
                        directDownloadAuthContainer.setVisibility(View.GONE);
                }
            });

            authMethodNone.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (!b) return;

                    authMethodNone.setChecked(true);
                    authMethodToken.setChecked(false);
                    authMethodHTTP.setChecked(false);

                    authMethodHTTPPasswdContainer.setVisibility(View.GONE);
                    authMethodHTTPUserContainer.setVisibility(View.GONE);
                    authMethodTokenToken.setVisibility(View.GONE);
                }
            });
            authMethodToken.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (!b) return;

                    authMethodNone.setChecked(false);
                    authMethodToken.setChecked(true);
                    authMethodHTTP.setChecked(false);

                    authMethodHTTPPasswdContainer.setVisibility(View.GONE);
                    authMethodHTTPUserContainer.setVisibility(View.GONE);
                    authMethodTokenToken.setVisibility(View.VISIBLE);
                }
            });
            authMethodHTTP.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (!b) return;

                    authMethodNone.setChecked(false);
                    authMethodToken.setChecked(false);
                    authMethodHTTP.setChecked(true);

                    authMethodHTTPPasswdContainer.setVisibility(View.VISIBLE);
                    authMethodHTTPUserContainer.setVisibility(View.VISIBLE);
                    authMethodTokenToken.setVisibility(View.GONE);
                }
            });
        }
    }

    private class SingleOnTextChangedListener implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            sViewHolder.completeURL.setText(String.format((sViewHolder.SSL.isChecked() ? "wss" : "ws") + "://%s:%s%s", sViewHolder.addr.getText().toString(), sViewHolder.port.getText().toString(), sViewHolder.endpoint.getText().toString()));
        }
    }

    private class MultiOnTextChangedListener implements TextWatcher {
        private final TextView completeURL;
        private final EditText addr;
        private final EditText port;
        private final EditText endpoint;
        private final CheckBox ssl;

        MultiOnTextChangedListener(TextView completeURL, EditText addr, EditText port, EditText endpoint, CheckBox ssl) {
            this.completeURL = completeURL;
            this.addr = addr;
            this.port = port;
            this.endpoint = endpoint;
            this.ssl = ssl;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            completeURL.setText(String.format((ssl.isChecked() ? "wss" : "ws") + "://%s:%s%s", addr.getText().toString(), port.getText().toString(), endpoint.getText().toString()));
        }
    }
}
